/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.trace;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import brave.Span;
import brave.Tracing;
import brave.http.HttpClientParser;
import brave.http.HttpServerParser;
import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.RequestMetadataCallback;
import com.google.devtools.cloudtrace.v1.PatchTracesRequest;
import com.google.devtools.cloudtrace.v1.Trace;
import com.google.devtools.cloudtrace.v1.TraceServiceGrpc;
import com.google.devtools.cloudtrace.v1.TraceSpan;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.awaitility.Duration;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import zipkin2.Call;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.Sender;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.SleuthProperties;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SleuthLogAutoConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests for auto-config.
 *
 * @author Ray Tsang
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Tim Ysewyn
 */
public class StackdriverTraceAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					StackdriverTraceAutoConfigurationTests.MockConfiguration.class,
					StackdriverTraceAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					TraceAutoConfiguration.class,
					SleuthLogAutoConfiguration.class,
					RefreshAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.project-id=proj",
					"spring.sleuth.sampler.probability=1.0");

	@Test
	public void test() {
		this.contextRunner.run((context) -> {
			SleuthProperties sleuthProperties = context.getBean(SleuthProperties.class);
			assertThat(sleuthProperties.isTraceId128()).isTrue();
			assertThat(sleuthProperties.isSupportsJoin()).isFalse();
			assertThat(context.getBean(HttpClientParser.class)).isNotNull();
			assertThat(context.getBean(HttpServerParser.class)).isNotNull();
			assertThat(context.getBean(Sender.class)).isNotNull();
			assertThat(context.getBean(ManagedChannel.class)).isNotNull();
		});
	}

	@Test
	public void supportsMultipleReporters() {
		this.contextRunner
				.withUserConfiguration(MultipleReportersConfig.class)
				.run((context) -> {
			SleuthProperties sleuthProperties = context.getBean(SleuthProperties.class);
			assertThat(sleuthProperties.isTraceId128()).isTrue();
			assertThat(sleuthProperties.isSupportsJoin()).isFalse();
			assertThat(context.getBean(HttpClientParser.class)).isNotNull();
			assertThat(context.getBean(HttpServerParser.class)).isNotNull();
			assertThat(context.getBean(ManagedChannel.class)).isNotNull();
			assertThat(context.getBeansOfType(Sender.class)).hasSize(2);
			assertThat(context.getBeansOfType(Sender.class)).containsKeys("stackdriverSender",
					"otherSender");
			assertThat(context.getBeansOfType(Reporter.class)).hasSize(2);
			assertThat(context.getBeansOfType(Reporter.class)).containsKeys("stackdriverReporter",
					"otherReporter");

			Span span = context.getBean(Tracing.class).tracer().nextSpan().name("foo")
					.tag("foo", "bar").start();
			span.finish();
			String traceId = span.context().traceIdString();

			MultipleReportersConfig.GcpTraceService gcpTraceService
					= context.getBean(MultipleReportersConfig.GcpTraceService.class);
			await().atMost(10, TimeUnit.SECONDS)
					.pollInterval(Duration.ONE_SECOND)
					.untilAsserted(() -> {
						assertThat(gcpTraceService.hasTraceFor(traceId)).isTrue();

						Trace trace = gcpTraceService.getTraceFor(traceId);
						assertThat(trace.getProjectId()).isEqualTo("proj");
						assertThat(trace.getSpansCount()).isEqualTo(1);

						TraceSpan traceSpan = trace.getSpans(0);
						assertThat(traceSpan.getName()).isEqualTo("foo");
						assertThat(traceSpan.getLabelsMap()).containsKey("foo");
						assertThat(traceSpan.getLabelsMap()).containsValue("bar");
					});

			MultipleReportersConfig.OtherSender sender
					= (MultipleReportersConfig.OtherSender) context.getBean("otherSender");
			await().atMost(10, TimeUnit.SECONDS)
					.untilAsserted(() -> assertThat(sender.isSpanSent()).isTrue());
		});
	}

	/**
	 * Spring config for tests.
	 */
	static class MockConfiguration {

		// We'll fake a successful call to GCP for the validation of our "credentials"
		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> {
				Credentials creds = mock(Credentials.class);
				doAnswer((Answer<Void>)
					(invocationOnMock) -> {
						RequestMetadataCallback callback =
								(RequestMetadataCallback) invocationOnMock.getArguments()[2];
						callback.onSuccess(Collections.emptyMap());
						return null;
					})
				.when(creds)
				.getRequestMetadata(any(), any(), any());
				return creds;
			};
		}
	}

	/**
	 * Spring config for tests with multiple reporters.
	 */
	static class MultipleReportersConfig {

		private static final String GRPC_SERVER_NAME = "in-process-grpc-server-name";

		@Bean(destroyMethod = "shutdownNow")
		Server server(GcpTraceService gcpTraceService) throws IOException {
			return InProcessServerBuilder.forName(GRPC_SERVER_NAME)
					.addService(gcpTraceService)
					.directExecutor()
					.build().start();
		}

		@Bean
		GcpTraceService gcpTraceService() {
			return new GcpTraceService();
		}

		@Bean(destroyMethod = "shutdownNow")
		ManagedChannel stackdriverSenderChannel() {
			return InProcessChannelBuilder.forName(GRPC_SERVER_NAME).directExecutor().build();
		}

		@Bean
		Reporter<zipkin2.Span> otherReporter(OtherSender otherSender) {
			return AsyncReporter.create(otherSender);
		}

		@Bean
		OtherSender otherSender() {
			return new OtherSender();
		}

		/**
		 * Custom sender for verification.
		 */
		static class OtherSender extends Sender {

			private boolean spanSent = false;

			boolean isSpanSent() {
				return this.spanSent;
			}

			@Override
			public Encoding encoding() {
				return Encoding.JSON;
			}

			@Override
			public int messageMaxBytes() {
				return Integer.MAX_VALUE;
			}

			@Override
			public int messageSizeInBytes(List<byte[]> encodedSpans) {
				return encoding().listSizeInBytes(encodedSpans);
			}

			@Override
			public Call<Void> sendSpans(List<byte[]> encodedSpans) {
				this.spanSent = true;
				return Call.create(null);
			}

		}

		/**
		 * Used as implementation on the in-process gRPC server for verification.
		 */
		static class GcpTraceService extends TraceServiceGrpc.TraceServiceImplBase {

			private Map<String, Trace> traces = new HashMap<>();

			boolean hasTraceFor(String traceId) {
				return this.traces.containsKey(traceId);
			}

			Trace getTraceFor(String traceId) {
				return this.traces.get(traceId);
			}

			@Override
			public void patchTraces(PatchTracesRequest request, StreamObserver<Empty> responseObserver) {
				request.getTraces().getTracesList().forEach((trace) -> this.traces.put(trace.getTraceId(), trace));
				responseObserver.onNext(Empty.getDefaultInstance());
				responseObserver.onCompleted();
			}
		}

	}
}
