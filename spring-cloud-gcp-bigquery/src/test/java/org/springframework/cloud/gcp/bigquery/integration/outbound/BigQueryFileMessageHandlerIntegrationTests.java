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

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo.WriteDisposition;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.bigquery.integration.BigQuerySpringMessageHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class BigQueryFileMessageHandlerIntegrationTests {

	private static final String DATASET_NAME = "test_dataset";

	private static final String TABLE_NAME = "test_table";

	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	@Autowired
	private BigQuery bigquery;

	@Autowired
	private BigQueryFileMessageHandler messageHandler;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"BigQuery integration tests are disabled. "
						+ "Please use '-Dit.bigquery=true' to enable them.",
				System.getProperty("it.bigquery"), is("true"));
	}

	@Before
	public void setup() {
		// Clear the previous dataset before beginning the test.
		this.bigquery.delete(TableId.of(DATASET_NAME, TABLE_NAME));
	}

	@Test
	public void testLoadFile() throws InterruptedException, ExecutionException {
		HashMap<String, Object> messageHeaders = new HashMap<>();
		this.messageHandler.setDatasetName(DATASET_NAME);
		this.messageHandler.setTableName(TABLE_NAME);
		this.messageHandler.setFormatOptions(FormatOptions.csv());

		Message<File> message = MessageBuilder.createMessage(
				new File("src/test/resources/data.csv"),
				new MessageHeaders(messageHeaders));

		ListenableFuture<Job> jobFuture =
				(ListenableFuture<Job>) this.messageHandler.handleRequestMessage(message);

		// Assert that a BigQuery polling task is scheduled successfully.
		await().atMost(Duration.FIVE_SECONDS)
				.untilAsserted(
						() -> assertThat(
								this.taskScheduler.getScheduledThreadPoolExecutor().getQueue()).hasSize(1));
		jobFuture.get();

		QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration
				.newBuilder("SELECT * FROM test_dataset.test_table").build();
		TableResult result = this.bigquery.query(queryJobConfiguration);

		assertThat(result.getTotalRows()).isEqualTo(1);
		assertThat(
				result.getValues().iterator().next().get("State").getStringValue()).isEqualTo("Alabama");

		// This asserts that the BigQuery job polling task is no longer in the scheduler.
		assertThat(this.taskScheduler.getScheduledThreadPoolExecutor().getQueue()).hasSize(0);
	}

	@Test
	public void testLoadFile_sync() throws InterruptedException {
		this.messageHandler.setSync(true);

		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(BigQuerySpringMessageHeaders.DATASET_NAME, DATASET_NAME);
		messageHeaders.put(BigQuerySpringMessageHeaders.TABLE_NAME, TABLE_NAME);
		messageHeaders.put(BigQuerySpringMessageHeaders.FORMAT_OPTIONS, FormatOptions.csv());

		Message<File> message = MessageBuilder.createMessage(
				new File("src/test/resources/data.csv"),
				new MessageHeaders(messageHeaders));

		Job job = (Job) this.messageHandler.handleRequestMessage(message);
		assertThat(job).isNotNull();

		QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration
				.newBuilder("SELECT * FROM test_dataset.test_table").build();
		TableResult result = this.bigquery.query(queryJobConfiguration);
		assertThat(result.getTotalRows()).isEqualTo(1);
	}

	@Test
	public void testLoadFile_cancel() {
		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(BigQuerySpringMessageHeaders.DATASET_NAME, DATASET_NAME);
		messageHeaders.put(BigQuerySpringMessageHeaders.TABLE_NAME, TABLE_NAME);
		messageHeaders.put(BigQuerySpringMessageHeaders.FORMAT_OPTIONS, FormatOptions.csv());

		Message<File> message = MessageBuilder.createMessage(
				new File("src/test/resources/data.csv"),
				new MessageHeaders(messageHeaders));

		ListenableFuture<Job> jobFuture =
				(ListenableFuture<Job>) this.messageHandler.handleRequestMessage(message);
		jobFuture.cancel(true);

		await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
			// This asserts that the BigQuery job polling task is no longer in the scheduler after cancel.
			assertThat(this.taskScheduler.getScheduledThreadPoolExecutor().getQueue()).hasSize(0);
		});
	}

	@EnableIntegration
	@Configuration
	static class BigQueryTestConfiguration {

		@Bean
		public BigQuery bigQuery() {
			return BigQueryOptions.getDefaultInstance().getService();
		}

		@Bean
		public BigQueryFileMessageHandler messageHandler(BigQuery bigQuery) {
			BigQueryFileMessageHandler messageHandler = new BigQueryFileMessageHandler(bigQuery);
			messageHandler.setWriteDisposition(WriteDisposition.WRITE_TRUNCATE);
			return messageHandler;
		}
	}
}
