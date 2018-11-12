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

package org.springframework.cloud.gcp.autoconfigure.logging.it;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.paging.Page;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.gcp.autoconfigure.sql.GcpCloudSqlAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.storage.GcpStorageAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.awaitility.Awaitility.await;

/**
 * @author João André Martins
 * @author Daniel Zou
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = {"spring.main.banner-mode=off"}
)
@RunWith(SpringRunner.class)
public class StackdriverLoggingIntegrationTests {

	private static final Log LOGGER = LogFactory.getLog(StackdriverLoggingIntegrationTests.class);

	@Autowired
	private GcpProjectIdProvider projectIdProvider;

	@Autowired
	private CredentialsProvider credentialsProvider;

	@Autowired
	private TestRestTemplate testRestTemplate;

	private static final LocalDateTime NOW = LocalDateTime.now();

	@BeforeClass
	public static void enableTests() {
		assumeThat(System.getProperty("it.logging")).isEqualTo("true");
	}

	@Test
	public void test() throws InterruptedException, IOException {
		HttpHeaders headers = new HttpHeaders();
		headers.add("x-cloud-trace-context", "everything-zen");
		ResponseEntity<String> responseEntity = this.testRestTemplate.exchange(
				"/", HttpMethod.GET, new HttpEntity<>(headers), String.class);
		assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();

		CredentialsProvider credentialsProvider = this.credentialsProvider;
		Logging logClient = LoggingOptions.newBuilder()
				.setCredentials(credentialsProvider.getCredentials())
				.build().getService();

		await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
			Page<LogEntry> page = logClient.listLogEntries(
					Logging.EntryListOption.filter("textPayload:\"#$%^&" + NOW + "\" AND"
							+ " logName=\"projects/" + this.projectIdProvider.getProjectId()
							+ "/logs/spring.log\""));

			ImmutableList<LogEntry> logEntries = ImmutableList.copyOf(page.getValues());
			assertThat(logEntries).hasSize(1);

			LogEntry entry = logEntries.get(0);
			assertThat(entry.getTrace()).matches(
					"projects/" + this.projectIdProvider.getProjectId() + "/traces/([a-z0-9]){32}");
		});
	}

	@RestController
	@SpringBootApplication(exclude = {
			GcpCloudSqlAutoConfiguration.class,
			GcpStorageAutoConfiguration.class,
			DataSourceAutoConfiguration.class,
			SecurityAutoConfiguration.class
	})
	static class LoggingApplication {

		@GetMapping("/")
		public String log() {
			LOGGER.error("#$%^&" + NOW);
			return "Log sent.";
		}
	}
}
