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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.JobInfo.WriteDisposition;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.WriteChannelConfiguration;

import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * A {@link org.springframework.messaging.MessageHandler} which handles sending and
 * loading files to a BigQuery table.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public class BigQueryFileMessageHandler extends AbstractReplyProducingMessageHandler {

	private final BigQuery bigQuery;

	private String datasetName;

	private String tableName;

	private FormatOptions formatOptions;

	private boolean autoDetectSchema = true;

	private WriteDisposition writeDisposition = WriteDisposition.WRITE_APPEND;

	public BigQueryFileMessageHandler(BigQuery bigQuery) {
		Assert.notNull(bigQuery, "BigQuery client must not be null.");
		this.bigQuery = bigQuery;
	}

	/**
	 * Sets the default BigQuery dataset name to use if it is omitted from message headers.
	 * @param datasetName name of the BigQuery dataset
	 */
	public void setDatasetName(String datasetName) {
		this.datasetName = datasetName;
	}

	/**
	 * Sets the default BigQuery table name to use if it is omitted from message headers.
	 * @param tableName name of the BigQuery dataset
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
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

	@Override
	protected Object handleRequestMessage(Message<?> message) {
		try {
			String datasetName =
					message.getHeaders().containsKey(BigQuerySpringMessageHeaders.DATASET_NAME)
							? message.getHeaders().get(BigQuerySpringMessageHeaders.DATASET_NAME, String.class)
							: this.datasetName;

			String tableName =
					message.getHeaders().containsKey(BigQuerySpringMessageHeaders.TABLE_NAME)
							? message.getHeaders().get(BigQuerySpringMessageHeaders.TABLE_NAME, String.class)
							: this.tableName;

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
			InputStream inputStream = convertToInputStream(message.getPayload());
			OutputStream sink = Channels.newOutputStream(writer);

			try {
				StreamUtils.copy(inputStream, sink);
			}
			finally {
				inputStream.close();
				sink.close();
			}

			return writer.getJob();
		}
		catch (FileNotFoundException e) {
			throw new MessageHandlingException(
					message, "Failed to find file to write to BigQuery in message handler: " + this, e);
		}
		catch (IOException e) {
			throw new MessageHandlingException(
					message, "Failed to write data to BigQuery tables in message handler: " + this, e);
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
