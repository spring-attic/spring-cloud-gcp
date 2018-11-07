/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.example;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.api.gax.paging.Page;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.StringPayload;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class LoggingSampleApplicationTests {

	private static final String LOG_FILTER_FORMAT = "trace:%s";

	@Autowired
	private GcpProjectIdProvider projectIdProvider;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@LocalServerPort
	private int port;

	private Logging logClient;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Spanner Google Cloud Logging integration tests are disabled. "
						+ "Please use '-Dit.logging=true' to enable them. ",
				System.getProperty("it.logging"), is("true"));
	}

	@Before
	public void setupLogging() {
		this.logClient = LoggingOptions.getDefaultInstance().getService();
	}

	@Test
	public void testLogRecordedInStackDriver() {
		String url = String.format("http://localhost:%s/log", this.port);
		String traceHeader = "gcp-logging-test-" + Instant.now().toEpochMilli();

		HttpHeaders headers = new HttpHeaders();
		headers.add("x-cloud-trace-context", traceHeader);
		ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
				url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();

		String logFilter = String.format(LOG_FILTER_FORMAT, traceHeader);

		await().atMost(60, TimeUnit.SECONDS)
				.pollInterval(2, TimeUnit.SECONDS)
				.untilAsserted(() -> {
					Page<LogEntry> logEntryPage = this.logClient.listLogEntries(
							Logging.EntryListOption.filter(logFilter));
					ImmutableList<LogEntry> logEntries = ImmutableList.copyOf(logEntryPage.iterateAll());

					List<String> logContents = logEntries.stream()
							.map(logEntry -> ((StringPayload) logEntry.getPayload()).getData())
							.collect(Collectors.toList());

					assertThat(logContents).containsExactlyInAnyOrder(
							"This line was written to the log.",
							"This line was also written to the log with the same Trace ID.");

					for (LogEntry logEntry : logEntries) {
						assertThat(logEntry.getLogName()).isEqualTo("spring.log");
						assertThat(logEntry.getResource().getLabels())
								.containsEntry("project_id", this.projectIdProvider.getProjectId());
					}
				});
	}
}
