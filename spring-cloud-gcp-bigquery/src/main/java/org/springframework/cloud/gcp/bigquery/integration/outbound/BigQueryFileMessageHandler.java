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
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;

import org.springframework.cloud.gcp.bigquery.core.BigQueryTemplate;
import org.springframework.cloud.gcp.bigquery.integration.BigQuerySpringMessageHeaders;
import org.springframework.core.io.Resource;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * A {@link org.springframework.messaging.MessageHandler} which handles sending and
 * loading files to a BigQuery table.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public class BigQueryFileMessageHandler extends AbstractReplyProducingMessageHandler {

	private final BigQueryTemplate bigQueryTemplate;

	private EvaluationContext evaluationContext;

	private Expression tableNameExpression;

	private Expression formatOptionsExpression;

	private Duration timeout = Duration.ofMinutes(5);

	private boolean sync = false;

	public BigQueryFileMessageHandler(BigQueryTemplate bigQueryTemplate) {
		Assert.notNull(bigQueryTemplate, "BigQueryTemplate must not be null.");
		this.bigQueryTemplate = bigQueryTemplate;

		this.tableNameExpression =
				new FunctionExpression<Message>(
						message -> message.getHeaders().get(BigQuerySpringMessageHeaders.TABLE_NAME));

		this.formatOptionsExpression =
				new FunctionExpression<Message>(
						message -> message.getHeaders().get(BigQuerySpringMessageHeaders.FORMAT_OPTIONS));
	}

	@Override
	protected void doInit() {
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	/**
	 * Sets the SpEL {@link Expression} to evaluate to determine the table name.
	 * @param tableNameExpression the SpEL expression used to evaluate the table name
	 */
	public void setTableNameExpression(Expression tableNameExpression) {
		Assert.notNull(tableNameExpression, "Table name expression must not be null.");
		this.tableNameExpression = tableNameExpression;
	}

	/**
	 * Sets the BigQuery table name to use. This overwrites any previous settings made
	 * by {@link #setTableNameExpression}.
	 * @param tableName name of the BigQuery table
	 */
	public void setTableName(String tableName) {
		this.tableNameExpression = new LiteralExpression(tableName);
	}

	/**
	 * Sets the SpEL {@link Expression} used to determine the {@link FormatOptions} for the handler.
	 * @param formatOptionsExpression the SpEL expression used to evaluate the {@link FormatOptions}
	 */
	public void setFormatOptionsExpression(Expression formatOptionsExpression) {
		Assert.notNull(formatOptionsExpression, "Format options expression cannot be null.");
		this.formatOptionsExpression = formatOptionsExpression;
	}

	/**
	 * Sets the handler's {@link FormatOptions} which describe the type/format of data files being
	 * loaded. This overwrites any previous settings made by {@link #setFormatOptionsExpression}.
	 * @param formatOptions the format of the data file being loaded
	 */
	public void setFormatOptions(FormatOptions formatOptions) {
		Assert.notNull(formatOptions, "Format options must not be null.");
		this.formatOptionsExpression = new ValueExpression<>(formatOptions);
	}

	/**
	 * Sets the {@link Duration} to wait for a file to be loaded into BigQuery before timing out
	 * when waiting synchronously.
	 * @param timeout the {@link Duration} timeout to wait for a file to load
	 */
	public void setTimeout(Duration timeout) {
		Assert.notNull(timeout, "Timeout duration must not be null.");
		this.timeout = timeout;
	}

	/**
	 * A {@code boolean} indicating if the {@link BigQueryFileMessageHandler} should synchronously
	 * wait for each file to be successfully loaded to BigQuery.
	 *
	 * <p>If set to true, the handler runs synchronously and returns {@link Job} for message
	 * responses. If set to false, the handler will return
	 * {@link org.springframework.util.concurrent.ListenableFuture} of the Job as the response
	 * for each message.
	 *
	 * @param sync whether {@link BigQueryFileMessageHandler} should wait synchronously for jobs to
	 * 		complete. Default is false (async).
	 */
	public void setSync(boolean sync) {
		this.sync = sync;
	}

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		String tableName =
				this.tableNameExpression.getValue(this.evaluationContext, message, String.class);
		FormatOptions formatOptions =
				this.formatOptionsExpression.getValue(this.evaluationContext, message, FormatOptions.class);

		Assert.notNull(tableName, "BigQuery table name must not be null.");
		Assert.notNull(formatOptions, "Data file formatOptions must not be null.");

		try (InputStream inputStream = convertToInputStream(message.getPayload())) {
			ListenableFuture<Job> jobFuture = this.bigQueryTemplate.writeDataToTable(tableName, inputStream,
					formatOptions);

			if (this.sync) {
				return jobFuture.get(this.timeout.getSeconds(), TimeUnit.SECONDS);
			}
			else {
				return jobFuture;
			}
		}
		catch (FileNotFoundException e) {
			throw new MessageHandlingException(
					message, "Failed to find file to write to BigQuery in message handler: " + this, e);
		}
		catch (IOException e) {
			throw new MessageHandlingException(
					message, "Failed to write data to BigQuery tables in message handler: " + this, e);
		}
		catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new MessageHandlingException(
					message, "Failed to wait for BigQuery Job to complete in message handler: " + this, e);
		}
	}

	private static InputStream convertToInputStream(Object payload) throws IOException {
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
		else if (payload instanceof Resource) {
			result = ((Resource) payload).getInputStream();
		}
		else {
			throw new IllegalArgumentException(
					String.format(
							"Unsupported message payload type: %s. The supported payload types "
									+ "are: java.io.File, byte[], org.springframework.core.io.Resource, "
									+ "and java.io.InputStream.",
							payload.getClass().getName()));
		}
		return result;
	}
}
