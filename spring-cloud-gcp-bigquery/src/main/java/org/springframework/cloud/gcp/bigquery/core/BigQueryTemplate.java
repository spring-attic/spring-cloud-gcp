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

package org.springframework.cloud.gcp.bigquery.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo.WriteDisposition;
import com.google.cloud.bigquery.JobStatus.State;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.WriteChannelConfiguration;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.DefaultManagedTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Helper class which simplifies common operations done in BigQuery.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public class BigQueryTemplate implements BigQueryOperations {

	private final BigQuery bigQuery;

	private final String datasetName;

	private final TaskScheduler taskScheduler;

	private boolean autoDetectSchema = true;

	private WriteDisposition writeDisposition = WriteDisposition.WRITE_APPEND;

	private Duration jobPollInterval = Duration.ofSeconds(2);

	/**
	 * Creates the {@link BigQuery} template.
	 *
	 * @param bigQuery the underlying client object used to interface with BigQuery
	 * @param datasetName the name of the dataset in which all operations will take place
	 */
	public BigQueryTemplate(BigQuery bigQuery, String datasetName) {
		this(bigQuery, datasetName, new DefaultManagedTaskScheduler());
	}

	/**
	 * Creates the {@link BigQuery} template.
	 *
	 * @param bigQuery the underlying client object used to interface with BigQuery
	 * @param datasetName the name of the dataset in which all operations will take place
	 * @param taskScheduler the {@link TaskScheduler} used to poll for the status of
	 *     long-running BigQuery operations
	 */
	public BigQueryTemplate(BigQuery bigQuery, String datasetName, TaskScheduler taskScheduler) {
		Assert.notNull(bigQuery, "BigQuery client object must not be null.");
		Assert.notNull(datasetName, "Dataset name must not be null");
		Assert.notNull(taskScheduler, "TaskScheduler must not be null");

		this.bigQuery = bigQuery;
		this.datasetName = datasetName;
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Sets whether BigQuery should attempt to autodetect the schema of the data when loading
	 * data into an empty table for the first time. If set to false, the schema must be
	 * defined explicitly for the table before load.
	 * @param autoDetectSchema whether data schema should be autodetected from the structure
	 *     of the data. Default is true.
	 */
	public void setAutoDetectSchema(boolean autoDetectSchema) {
		this.autoDetectSchema = autoDetectSchema;
	}

	/**
	 * Sets the {@link WriteDisposition} which specifies how data should be inserted into
	 * BigQuery tables.
	 * @param writeDisposition whether to append to or truncate (overwrite) data in the
	 *     BigQuery table. Default is {@code WriteDisposition.WRITE_APPEND} to append data to
	 *     a table.
	 */
	public void setWriteDisposition(WriteDisposition writeDisposition) {
		Assert.notNull(writeDisposition, "BigQuery write disposition must not be null.");
		this.writeDisposition = writeDisposition;
	}

	/**
	 * Sets the {@link Duration} amount of time to wait between successive polls on the status
	 * of a BigQuery job.
	 * @param jobPollInterval the {@link Duration} poll interval for BigQuery job status
	 *     polling
	 */
	public void setJobPollInterval(Duration jobPollInterval) {
		Assert.notNull(jobPollInterval, "BigQuery job polling interval must not be null");
		this.jobPollInterval = jobPollInterval;
	}

	@Override
	public ListenableFuture<Job> writeDataToTable(
			String tableName, InputStream inputStream, FormatOptions dataFormatOptions) {
		TableId tableId = TableId.of(datasetName, tableName);

		WriteChannelConfiguration writeChannelConfiguration = WriteChannelConfiguration
				.newBuilder(tableId)
				.setFormatOptions(dataFormatOptions)
				.setAutodetect(this.autoDetectSchema)
				.setWriteDisposition(this.writeDisposition)
				.build();

		TableDataWriteChannel writer = bigQuery.writer(writeChannelConfiguration);

		try (OutputStream sink = Channels.newOutputStream(writer)) {
			// Write data from data input file to BigQuery
			StreamUtils.copy(inputStream, sink);
		}
		catch (IOException e) {
			throw new BigQueryException("Failed to write data to BigQuery tables.", e);
		}

		if (writer.getJob() == null) {
			throw new BigQueryException(
					"Failed to initialize the BigQuery write job.");
		}

		return createJobFuture(writer.getJob());
	}

	private SettableListenableFuture<Job> createJobFuture(Job pendingJob) {
		// Prepare the polling task for the ListenableFuture result returned to end-user
		SettableListenableFuture<Job> result = new SettableListenableFuture<>();

		ScheduledFuture<?> scheduledFuture = taskScheduler.scheduleAtFixedRate(() -> {
			Job job = pendingJob.reload();
			if (State.DONE.equals(job.getStatus().getState())) {
				if (job.getStatus().getError() != null) {
					result.setException(
							new BigQueryException(job.getStatus().getError().getMessage()));
				}
				else {
					result.set(job);
				}
			}
		}, this.jobPollInterval);

		result.addCallback(
				response -> scheduledFuture.cancel(true),
				response -> {
					pendingJob.cancel();
					scheduledFuture.cancel(true);
				});

		return result;
	}
}
