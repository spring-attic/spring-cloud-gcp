/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.bigquery.integration.outbound;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo.WriteDisposition;
import com.google.cloud.bigquery.JobStatus.State;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.WriteChannelConfiguration;

import org.springframework.cloud.gcp.bigquery.integration.BigQuerySpringMessageHeaders;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * A {@link org.springframework.messaging.MessageHandler} which handles sending and
 * loading files to a BigQuery table.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public class BigQueryFileMessageHandler extends AbstractReplyProducingMessageHandler {

	private static final String HEADER_FORMAT = "headers[%s]";

	private final BigQuery bigQuery;

	private EvaluationContext evaluationContext;

	private Expression datasetNameExpression;

	private Expression tableNameExpression;

	private Expression formatOptionsExpression;

	private boolean autoDetectSchema = true;

	private WriteDisposition writeDisposition = WriteDisposition.WRITE_APPEND;

	private Duration timeout = Duration.ofMinutes(5);

	private Duration jobPollInterval = Duration.ofSeconds(2);

	private boolean sync = false;

	public BigQueryFileMessageHandler(BigQuery bigQuery) {
		Assert.notNull(bigQuery, "BigQuery client must not be null.");
		this.bigQuery = bigQuery;

		ExpressionParser expressionParser = new SpelExpressionParser();
		this.datasetNameExpression = expressionParser.parseExpression(
				String.format(HEADER_FORMAT, BigQuerySpringMessageHeaders.DATASET_NAME));
		this.tableNameExpression = expressionParser.parseExpression(
				String.format(HEADER_FORMAT, BigQuerySpringMessageHeaders.TABLE_NAME));
		this.formatOptionsExpression = expressionParser.parseExpression(
				String.format(HEADER_FORMAT, BigQuerySpringMessageHeaders.FORMAT_OPTIONS));
	}

	@Override
	protected void doInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	/**
	 * Sets the SpEL {@link Expression} to evaluate to determine the dataset name.
	 * @param datasetNameExpression the SpEL expression used to evaluate the dataset name
	 */
	public void setDatasetNameExpression(Expression datasetNameExpression) {
		Assert.notNull(datasetNameExpression, "Dataset name expression can't be null.");
		this.datasetNameExpression = datasetNameExpression;
	}

	/**
	 * Sets the BigQuery dataset name to use.
	 * @param datasetName name of the BigQuery dataset
	 */
	public void setDatasetName(String datasetName) {
		this.datasetNameExpression = new LiteralExpression(datasetName);
	}

	/**
	 * Sets the SpEL {@link Expression} to evaluate to determine the table name.
	 * @param tableNameExpression the SpEL expression used to evaluate the table name
	 */
	public void setTableNameExpression(Expression tableNameExpression) {
		this.tableNameExpression = tableNameExpression;
	}

	/**
	 * Sets the BigQuery table name to use.
	 * @param tableName name of the BigQuery table
	 */
	public void setTableName(String tableName) {
		this.tableNameExpression = new LiteralExpression(tableName);
	}

	/**
	 * Sets the default {@link FormatOptions} to use for the handler.
	 * The {@link FormatOptions} describe the type/format of data files being loaded.
	 * @param formatOptions the format of the data file being loaded
	 */
	public void setFormatOptions(FormatOptions formatOptions) {
		this.formatOptionsExpression = new FunctionExpression<Message>(m -> formatOptions);
	}

	/**
	 * Sets the SpEL {@link Expression} used to determine the {@link FormatOptions} for the handler.
	 * @param formatOptionsExpression the SpEL expression used to evaluate the {@link FormatOptions}
	 */
	public void setFormatOptionsExpression(Expression formatOptionsExpression) {
		this.formatOptionsExpression = formatOptionsExpression;
	}

	/**
	 * Sets the {@link WriteDisposition} which specifies how data should be inserted into
	 * BigQuery tables.
	 * @param writeDisposition whether data should be appended or truncated to the BigQuery table
	 */
	public void setWriteDisposition(WriteDisposition writeDisposition) {
		this.writeDisposition = writeDisposition;
	}

	/**
	 * Sets whether BigQuery should attempt to autodetect the schema of the data when loading
	 * data into an empty table for the first time.
	 * @param autoDetectSchema whether data schema should be autodetected from the structure of
	 * 		the data
	 */
	public void setAutoDetectSchema(boolean autoDetectSchema) {
		this.autoDetectSchema = autoDetectSchema;
	}

	/**
	 * Sets the {@link Duration} to wait for a file to be loaded into BigQuery before timing out
	 * when waiting synchronously.
	 * @param timeout the {@link Duration} timeout to wait for a file to load
	 */
	public void setTimeout(Duration timeout) {
		this.timeout = timeout;
	}

	/**
	 * Sets the {@link Duration} amount of time to wait between successive polls on the status of
	 * a BigQuery job.
	 * @param pollInterval the {@link Duration} poll interval for BigQuery job status polling
	 */
	public void setJobPollInterval(Duration pollInterval) {
		this.jobPollInterval = pollInterval;
	}

	/**
	 * A {@code boolean} indicating if the {@link BigQueryFileMessageHandler} should synchronously
	 * wait for each file to be successfully loaded to BigQuery.
	 * @param sync whether {@link BigQueryFileMessageHandler} should wait synchronously for jobs to
	 * 		complete
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		String datasetName =
				this.datasetNameExpression.getValue(this.evaluationContext, message, String.class);
		String tableName =
				this.tableNameExpression.getValue(this.evaluationContext, message, String.class);
		FormatOptions formatOptions =
				this.formatOptionsExpression.getValue(this.evaluationContext, message, FormatOptions.class);

		Assert.notNull(datasetName, "BigQuery dataset name must not be null.");
		Assert.notNull(tableName, "BigQuery table name must not be null.");
		Assert.notNull(formatOptions, "Data file formatOptions must not be null.");

		TableId tableId = TableId.of(datasetName, tableName);

		WriteChannelConfiguration writeChannelConfiguration = WriteChannelConfiguration
				.newBuilder(tableId)
				.setFormatOptions(formatOptions)
				.setAutodetect(this.autoDetectSchema)
				.setWriteDisposition(this.writeDisposition)
				.build();

		TableDataWriteChannel writer = bigQuery.writer(writeChannelConfiguration);

		try (
				InputStream inputStream = convertToInputStream(message.getPayload());
				OutputStream sink = Channels.newOutputStream(writer)) {
			// Write data from data input file to BigQuery
			StreamUtils.copy(inputStream, sink);
		}
		catch (FileNotFoundException e) {
			throw new MessageHandlingException(
					message, "Failed to find file to write to BigQuery in message handler: " + this, e);
		}
		catch (IOException e) {
			throw new MessageHandlingException(
					message, "Failed to write data to BigQuery tables in message handler: " + this, e);
		}

		if (writer.getJob() == null) {
			throw new MessageHandlingException(
					message, "Failed to initialize the BigQuery write job in message handler: " + this);
		}

		Assert.notNull(getTaskScheduler(),
				"You must set a Task Scheduler for BigQueryFileMessageHandler before using "
						+ "by enabling Spring Integration using @EnableIntegration "
						+ "or calling BigQueryFileMessageHandler.setTaskScheduler(taskScheduler).");

		// Prepare the polling task for the ListenableFuture result returned to end-user
		SettableListenableFuture<Job> result = new SettableListenableFuture<>();
		ScheduledFuture<?> scheduledFuture = getTaskScheduler().scheduleAtFixedRate(() -> {
			Job job = writer.getJob().reload();
			if (State.DONE.equals(job.getStatus().getState())) {
				if (job.getStatus().getError() != null) {
					result.setException(
							new RuntimeException(job.getStatus().getError().getMessage()));
				}
				else {
					result.set(job);
				}
			}
		}, this.jobPollInterval);

		result.addCallback(
				response -> scheduledFuture.cancel(true),
				response -> {
					writer.getJob().cancel();
					scheduledFuture.cancel(true);
				});

		if (this.sync) {
			try {
				return result.get(this.timeout.getSeconds(), TimeUnit.SECONDS);
			}
			catch (InterruptedException | ExecutionException | TimeoutException e) {
				throw new MessageHandlingException(
						message, "Failed to wait for BigQuery job to complete in message handler: " + this, e);
			}
		}
		else {
			return result;
		}
	}

	private static InputStream convertToInputStream(Object payload) throws FileNotFoundException {
		InputStream result;

		if (payload instanceof File) {
			result = new BufferedInputStream(new FileInputStream((File) payload));
		}
		else if (payload instanceof byte[]) {
			result = new ByteArrayInputStream((byte[]) payload);
		}
		else if (payload instanceof InputStream) {
			result = (InputStream) payload;
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							"Unsupported message payload type: %s. The supported payload types "
									+ "are: java.io.File, byte[], and java.io.InputStream.",
							payload.getClass().getName()));
		}
		return result;
	}
}
