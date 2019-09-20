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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobStatus;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.bigquery.core.BigQueryTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.gcp.bigquery.integration.BigQueryTestConfiguration.DATASET_NAME;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = BigQueryTestConfiguration.class)
public class BigQueryTemplateIntegrationTests {

	private static final String TABLE_NAME = "template_test_table";

	@Autowired
	BigQuery bigQuery;

	@Autowired
	BigQueryTemplate bigQueryTemplate;

	@Before
	public void setup() {
		// Clear the previous dataset before beginning the test.
		this.bigQuery.delete(TableId.of(DATASET_NAME, TABLE_NAME));
	}

	@Test
	public void testLoadFile() throws IOException, ExecutionException, InterruptedException {
		File file = new File("src/test/resources/data.csv");
		InputStream fileInputStream = new BufferedInputStream(new FileInputStream(file));

		ListenableFuture<Job> bigQueryJobFuture =
				bigQueryTemplate.writeDataToTable(TABLE_NAME, fileInputStream, FormatOptions.csv());

		Job job = bigQueryJobFuture.get();
		assertThat(job.getStatus().getState()).isEqualTo(JobStatus.State.DONE);

		QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration
				.newBuilder("SELECT * FROM test_dataset.test_table").build();
		TableResult result = this.bigQuery.query(queryJobConfiguration);

		assertThat(result.getTotalRows()).isEqualTo(1);
		assertThat(
				result.getValues().iterator().next().get("State").getStringValue()).isEqualTo("Alabama");
	}

	@Test
	public void testLoadBytes() throws IOException, ExecutionException, InterruptedException {
		byte[] byteArray =
				"CountyId,State,County\n1001,Alabama,Autauga County\n".getBytes();
		ByteArrayInputStream byteStream = new ByteArrayInputStream(byteArray);

		ListenableFuture<Job> bigQueryJobFuture =
				bigQueryTemplate.writeDataToTable(TABLE_NAME, byteStream, FormatOptions.csv());

		Job job = bigQueryJobFuture.get();
		assertThat(job.getStatus().getState()).isEqualTo(JobStatus.State.DONE);

		QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration
				.newBuilder("SELECT * FROM test_dataset.test_table").build();
		TableResult result = this.bigQuery.query(queryJobConfiguration);

		assertThat(result.getTotalRows()).isEqualTo(1);
		assertThat(
				result.getValues().iterator().next().get("State").getStringValue()).isEqualTo("Alabama");
	}
}
