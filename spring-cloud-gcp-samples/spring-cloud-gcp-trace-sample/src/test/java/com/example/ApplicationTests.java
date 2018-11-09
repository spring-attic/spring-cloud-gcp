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

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.Logging;
import com.google.cloud.logging.LoggingOptions;
import com.google.cloud.logging.Payload.StringPayload;
import com.google.common.collect.ImmutableList;
import com.google.devtools.cloudtrace.v1.GetTraceRequest;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceServiceGrpc;
import com.google.devtools.cloudtrace.v1.TraceServiceGrpc.TraceServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import org.awaitility.Duration;
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
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Verifies that the logged Traces on the sample application appear in StackDriver.
 *
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { Application.class })
public class ApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private GcpProjectIdProvider projectIdProvider;

	@Autowired
	private CredentialsProvider credentialsProvider;

	private String url;

	private TestRestTemplate testRestTemplate;

	private Logging logClient;

	private TraceServiceBlockingStub traceServiceStub;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Google Cloud Trace integration tests are disabled. "
						+ "Please use '-Dit.trace=true' to enable them. ",
				System.getProperty("it.trace"), is("true"));
	}

	@Before
	public void setupTraceClient() throws IOException {
		this.url = String.format("http://localhost:%d/", this.port);

		// Create a new RestTemplate here because the auto-wired instance has built-in instrumentation
		// which interferes with us setting the 'x-cloud-trace-context' header.
		this.testRestTemplate = new TestRestTemplate();

		this.logClient = LoggingOptions.newBuilder()
				.setProjectId(this.projectIdProvider.getProjectId())
				.setCredentials(this.credentialsProvider.getCredentials())
				.build()
				.getService();

		ManagedChannel channel = ManagedChannelBuilder
				.forTarget("cloudtrace.googleapis.com")
				.build();

		this.traceServiceStub = TraceServiceGrpc.newBlockingStub(channel)
				.withCallCredentials(MoreCallCredentials.from(this.credentialsProvider.getCredentials()));
	}

	@Test
	public void testTracesAreLoggedCorrectly() {
		HttpHeaders headers = new HttpHeaders();

		String uuidString = UUID.randomUUID().toString().replaceAll("-", "");

		headers.add("x-cloud-trace-context", uuidString);
		this.testRestTemplate.exchange(this.url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

		GetTraceRequest getTraceRequest = GetTraceRequest.newBuilder()
				.setProjectId(this.projectIdProvider.getProjectId())
				.setTraceId(uuidString)
				.build();

		String logFilter = String.format(
				"trace=projects/%s/traces/%s", this.projectIdProvider.getProjectId(), uuidString);

		await().atMost(60, TimeUnit.SECONDS)
				.pollInterval(Duration.TWO_SECONDS)
				.ignoreExceptions()
				.untilAsserted(() -> {

			Trace trace = this.traceServiceStub.getTrace(getTraceRequest);
			assertThat(trace.getTraceId()).isEqualTo(uuidString);
			assertThat(trace.getSpansCount()).isEqualTo(8);

			ImmutableList<LogEntry> logEntries = ImmutableList.copyOf(
					this.logClient.listLogEntries(Logging.EntryListOption.filter(logFilter)).iterateAll());

			List<String> logContents = logEntries.stream()
					.map(logEntry -> ((StringPayload) logEntry.getPayload()).getData())
					.collect(Collectors.toList());

			assertThat(logContents).contains("starting busy work");
			assertThat(logContents).contains("finished busy work");
		});
	}
}
