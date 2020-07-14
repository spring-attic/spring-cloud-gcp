/*
 * Copyright 2017-2020 the original author or authors.
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.api.MetricDescriptor;
import com.google.cloud.monitoring.v3.MetricServiceClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for the metrics sample app.
 *
 * @author Eddú Meléndez
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = MetricsApplication.class)
public class MetricsSampleApplicationTests {

	@Autowired
	private GcpProjectIdProvider projectIdProvider;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@LocalServerPort
	private int port;

	private MetricServiceClient metricClient;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Google Cloud Monitoring integration tests are disabled. "
						+ "Please use '-Dit.metrics=true' to enable them. ",
				System.getProperty("it.metrics"), is("true"));
	}

	@Before
	public void setupLogging() throws IOException {
		this.metricClient = MetricServiceClient.create();
	}

	@Test
	public void testMetricRecordedInStackdriver() {
		String projectId = this.projectIdProvider.getProjectId();

		String id = "integration_test_" + UUID.randomUUID().toString().replace('-', '_');
		String url = String.format("http://localhost:%s/%s", this.port, id);

		ResponseEntity<String> responseEntity = this.testRestTemplate.postForEntity(url, null, String.class);
		assertThat(responseEntity.getStatusCode().is2xxSuccessful()).isTrue();

		String metricType = "custom.googleapis.com/" + id;
		String metricName = "projects/" + projectId + "/metricDescriptors/" + metricType;

		await().atMost(4, TimeUnit.MINUTES)
				.pollInterval(5, TimeUnit.SECONDS)
				.ignoreExceptionsMatching(e -> e.getMessage().contains("Could not find descriptor for metric"))
				.untilAsserted(() -> {
					MetricDescriptor metricDescriptor = this.metricClient.getMetricDescriptor(metricName);
					assertThat(metricDescriptor.getName()).isEqualTo(metricName);
					assertThat(metricDescriptor.getType()).isEqualTo(metricType);
				});
	}

}
