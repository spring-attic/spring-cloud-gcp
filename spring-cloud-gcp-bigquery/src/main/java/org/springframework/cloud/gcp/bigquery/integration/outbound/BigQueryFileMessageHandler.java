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

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * A {@link org.springframework.messaging.MessageHandler} which handles sending and
 * loading files to a BigQuery table.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public class BigQueryFileMessageHandler extends AbstractReplyProducingMessageHandler {

	private final BigQuery bigQuery;

	private final TaskScheduler taskScheduler;

	private final EvaluationContext evaluationContext;

	private Expression datasetNameExpr;

	private Expression tableNameExpr;

	private FormatOptions formatOptions;

	private boolean autoDetectSchema = true;

	private WriteDisposition writeDisposition = WriteDisposition.WRITE_APPEND;

	private Duration timeout = Duration.ofMinutes(5);

	private boolean sync = false;

	public BigQueryFileMessageHandler(BigQuery bigQuery, TaskScheduler taskScheduler) {
		Assert.notNull(bigQuery, "BigQuery client must not be null.");
		this.bigQuery = bigQuery;
		this.taskScheduler = taskScheduler;
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	/**
	 * Sets the SpEL expression to evaluate to determine the default dataset name if the
	 * dataset name is omitted from message headers.
	 * @param datasetNameExpression name of the BigQuery dataset
	 */
	public void setDatasetNameExpression(Expression datasetNameExpression) {
		Assert.notNull(datasetNameExpression, "Dataset name expression can't be null.");
		this.datasetNameExpr = datasetNameExpression;
	}

	/**
	 * Sets the default BigQuery dataset name to use if it is omitted from message headers.
	 * @param datasetName name of the BigQuery dataset
	 */
	public void setDatasetName(String datasetName) {
		this.datasetNameExpr = new LiteralExpression(datasetName);
	}

	/**
	 * Sets the SpEL expression to evaluate to determine the default table name if the
	 * table name is omitted from message headers.
	 * @param tableNameExpression name of the BigQuery dataset
	 */
	public void setTableNameExpression(Expression tableNameExpression) {
		this.tableNameExpr = tableNameExpression;
	}

	/**
	 * Sets the default BigQuery table name to use if it is omitted from message headers.
	 * @param tableName name of the BigQuery dataset
	 */
	public void setTableName(String tableName) {
		this.tableNameExpr = new LiteralExpression(tableName);
	}

	/**
	 * Sets the default {@link FormatOptions} to use if it is omitted from message headers.
	 * The {@link FormatOptions} describe the type/format of data files being loaded.
	 * @param formatOptions the format of the data file being loaded
	 */
	public void setFormatOptions(FormatOptions formatOptions) {
		this.formatOptions = formatOptions;
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
	 * A {@code boolean} indicating if the {@link BigQueryFileMessageHandler} should synchronously
	 * wait for each file to be successfully loaded to BigQuery.
	 * @param sync whether {@link BigQueryFileMessageHandler} should wait synchronously for jobs to
	 * 		complete
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	@Override
	protected ListenableFuture handleRequestMessage(Message<?> message) {
		String datasetName =
				message.getHeaders().containsKey(BigQuerySpringMessageHeaders.DATASET_NAME)
						? message.getHeaders().get(BigQuerySpringMessageHeaders.DATASET_NAME, String.class)
						: this.datasetNameExpr.getValue(this.evaluationContext, message, String.class);

		String tableName =
				message.getHeaders().containsKey(BigQuerySpringMessageHeaders.TABLE_NAME)
						? message.getHeaders().get(BigQuerySpringMessageHeaders.TABLE_NAME, String.class)
						: this.tableNameExpr.getValue(this.evaluationContext, message, String.class);

		FormatOptions formatOptions =
				message.getHeaders().containsKey(BigQuerySpringMessageHeaders.FORMAT_OPTIONS)
						? message.getHeaders().get(
								BigQuerySpringMessageHeaders.FORMAT_OPTIONS, FormatOptions.class)
						: this.formatOptions;

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

		// Prepare the polling task for the ListenableFuture result returned to end-user
		SettableListenableFuture result = new SettableListenableFuture();
		ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> {
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
		}, Duration.ofSeconds(2));

		result.addCallback(
				response -> scheduledFuture.cancel(true),
				response -> {
					writer.getJob().cancel();
					scheduledFuture.cancel(true);
				});

		try {
			if (this.sync) {
				result.get(this.timeout.getSeconds(), TimeUnit.SECONDS);
			}
		}
		catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new MessageHandlingException(
					message, "Failed to wait for BigQuery job to complete in message handler: " + this, e);
		}

		return result;
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
