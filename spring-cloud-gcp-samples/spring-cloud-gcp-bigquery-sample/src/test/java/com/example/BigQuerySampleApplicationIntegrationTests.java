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

package com.example;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests the {@link BigQuerySampleApplication} POST endpoints.
 *
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = BigQuerySampleApplication.class, properties = "spring.cloud.gcp.bigquery.datasetName=test_dataset")
public class BigQuerySampleApplicationIntegrationTests {

	private static final String DATASET_NAME = "test_dataset";

	private static final String TABLE_NAME = "bigquery_sample_test_table";

	@Autowired
	BigQuery bigQuery;

	@Autowired
	TestRestTemplate restTemplate;

	@Value("classpath:test.csv")
	Resource csvFile;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"BigQuery integration tests are disabled. "
						+ "Please use '-Dit.bigquery=true' to enable them.",
				System.getProperty("it.bigquery"), is("true"));
	}

	@Before
	@After
	public void cleanupTestEnvironment() {
		// Clear the previous dataset before beginning the test.
		this.bigQuery.delete(TableId.of(DATASET_NAME, TABLE_NAME));
	}

	@Test
	public void testFileUpload() throws InterruptedException, IOException {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("file", csvFile);
		map.add("tableName", TABLE_NAME);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.MULTIPART_FORM_DATA);

		HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String> response =
				this.restTemplate.postForEntity("/uploadFile", request, String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

		QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration
				.newBuilder("SELECT * FROM " + DATASET_NAME + "." + TABLE_NAME)
				.build();

		TableResult queryResult = this.bigQuery.query(queryJobConfiguration);
		assertThat(queryResult.getTotalRows()).isEqualTo(3);

		List<String> names = StreamSupport.stream(queryResult.getValues().spliterator(), false)
				.map(valueList -> valueList.get(0).getStringValue())
				.collect(Collectors.toList());
		assertThat(names).containsExactlyInAnyOrder("Nathaniel", "Diaz", "Johnson");
	}

	@Test
	public void testCsvDataUpload() throws InterruptedException {
		LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("csvText", "name,age,location\nBob,24,Wyoming");
		map.add("tableName", TABLE_NAME);

		HttpHeaders headers = new HttpHeaders();
		HttpEntity<LinkedMultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);
		ResponseEntity<String> response =
				this.restTemplate.postForEntity("/uploadCsvText", request, String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

		QueryJobConfiguration queryJobConfiguration = QueryJobConfiguration
				.newBuilder("SELECT * FROM " + DATASET_NAME + "." + TABLE_NAME)
				.build();

		TableResult queryResult = this.bigQuery.query(queryJobConfiguration);
		assertThat(queryResult.getTotalRows()).isEqualTo(1);

		FieldValueList row = queryResult.getValues().iterator().next();
		assertThat(row.get(0).getStringValue()).isEqualTo("Bob");
		assertThat(row.get(1).getLongValue()).isEqualTo(24);
		assertThat(row.get(2).getStringValue()).isEqualTo("Wyoming");
	}
}
