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

package org.springframework.cloud.gcp.bigquery.integration;

import java.io.File;
import java.util.HashMap;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobInfo.WriteDisposition;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

public class BigQueryFileMessageHandlerIntegrationTests {

	private static final String DATASET_NAME = "test_dataset";

	private static final String TABLE_NAME = "test_table";

	private BigQuery bigquery;

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
		this.bigquery = BigQueryOptions.getDefaultInstance().getService();

		this.messageHandler = new BigQueryFileMessageHandler(this.bigquery);
		this.messageHandler.setWriteDisposition(WriteDisposition.WRITE_EMPTY);

		// Clear the previous dataset before beginning the test.
		this.bigquery.delete(TableId.of(DATASET_NAME, TABLE_NAME));
	}

	@Test
	public void testMessageHandlerLoadFile() throws InterruptedException {
		HashMap<String, Object> messageHeaders = new HashMap<>();
		messageHeaders.put(BigQuerySpringMessageHeaders.DATASET_NAME, "test_dataset");
		messageHeaders.put(BigQuerySpringMessageHeaders.TABLE_NAME, "test_table");
		messageHeaders.put(BigQuerySpringMessageHeaders.FORMAT_OPTIONS, FormatOptions.csv());

		Message<File> message = MessageBuilder.createMessage(
				new File("src/test/resources/data.csv"),
				new MessageHeaders(messageHeaders));

		Job job = (Job) this.messageHandler.handleRequestMessage(message);
		job.waitFor();

		QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration
				.newBuilder("SELECT * FROM test_dataset.test_table").build();
		TableResult result = this.bigquery.query(queryJobConfiguration);

		assertThat(result.getTotalRows()).isEqualTo(1);
		assertThat(
				result.getValues().iterator().next().get("State").getStringValue()).isEqualTo("Alabama");
	}

}
