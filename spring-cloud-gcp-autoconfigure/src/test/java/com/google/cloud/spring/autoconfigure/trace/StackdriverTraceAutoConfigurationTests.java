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

package com.google.cloud.spring.autoconfigure.trace;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import brave.TracingCustomizer;
import brave.handler.SpanHandler;
import brave.http.HttpRequestParser;
import brave.http.HttpTracingCustomizer;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.auth.Credentials;
import com.google.auth.RequestMetadataCallback;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.devtools.cloudtrace.v2.BatchWriteSpansRequest;
import com.google.devtools.cloudtrace.v2.Span;
import com.google.devtools.cloudtrace.v2.TraceServiceGrpc;
import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import zipkin2.Call;
import zipkin2.CheckResult;
import zipkin2.codec.Encoding;
import zipkin2.codec.SpanBytesEncoder;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
					StackdriverTraceAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					BraveAutoConfiguration.class,
					RefreshAutoConfiguration.class))
			.withUserConfiguration(StackdriverTraceAutoConfigurationTests.MockConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.project-id=proj",
					"spring.sleuth.sampler.probability=1.0");

	@Test
	public void test() {
		this.contextRunner
				.withBean(
						StackdriverTraceAutoConfiguration.SPAN_HANDLER_BEAN_NAME,
						SpanHandler.class,
						() ->  SpanHandler.NOOP)
				.run(context -> {
			assertThat(context.getBean(HttpRequestParser.class)).isNotNull();
			assertThat(context.getBean(HttpTracingCustomizer.class)).isNotNull();
			assertThat(context.getBean(StackdriverTraceAutoConfiguration.SENDER_BEAN_NAME, Sender.class)).isNotNull();
			assertThat(context.getBean(ManagedChannel.class)).isNotNull();
		});
	}

	@Test
	public void supportsMultipleReporters() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(
						BraveAutoConfiguration.class,
						StackdriverTraceAutoConfiguration.class,
						GcpContextAutoConfiguration.class,
						RefreshAutoConfiguration.class))
				.withUserConfiguration(MultipleSpanHandlersConfig.class)
				.run(context -> {
			assertThat(context.getBean(HttpRequestParser.class)).isNotNull();
			assertThat(context.getBean(HttpTracingCustomizer.class)).isNotNull();
			assertThat(context.getBean(ManagedChannel.class)).isNotNull();
			assertThat(context.getBeansOfType(Sender.class)).hasSize(2);
			assertThat(context.getBeansOfType(Sender.class)).containsKeys("stackdriverSender",
					"otherSender");
			assertThat(context.getBeansOfType(SpanHandler.class)).containsKeys("stackdriverSpanHandler",
					"otherSpanHandler");

			org.springframework.cloud.sleuth.Span span = context.getBean(Tracer.class).nextSpan().name("foo")
					.tag("foo", "bar").start();
			span.end();
			String spanId = span.context().spanId();

			MultipleSpanHandlersConfig.GcpTraceService gcpTraceService
					= context.getBean(MultipleSpanHandlersConfig.GcpTraceService.class);
			await().atMost(10, TimeUnit.SECONDS)
					.pollInterval(Duration.ofSeconds(1))
					.untilAsserted(() -> {
						assertThat(gcpTraceService.hasSpan(spanId)).isTrue();

						Span traceSpan = gcpTraceService.getSpan(spanId);
						assertThat(traceSpan.getDisplayName().getValue()).isEqualTo("foo");
						assertThat(traceSpan.getAttributes().getAttributeMapMap()).containsKey("foo");
						assertThat(traceSpan.getAttributes().getAttributeMapMap().get("foo").getStringValue()
								.getValue()).isEqualTo("bar");
					});

			MultipleSpanHandlersConfig.OtherSender sender
					= (MultipleSpanHandlersConfig.OtherSender) context.getBean("otherSender");
			await().atMost(10, TimeUnit.SECONDS)
					.untilAsserted(() -> assertThat(sender.isSpanSent()).isTrue());
		});
	}

	@Test
	public void testAsyncReporterHealthCheck() {
		Sender senderMock = mock(Sender.class);
		when(senderMock.check()).thenReturn(CheckResult.OK);
		when(senderMock.encoding()).thenReturn(SpanBytesEncoder.PROTO3.encoding());

		this.contextRunner
				.withBean(
						StackdriverTraceAutoConfiguration.SENDER_BEAN_NAME,
						Sender.class,
						() -> senderMock)
				.run(context -> {
					SpanHandler spanHandler = context.getBean(StackdriverTraceAutoConfiguration.SPAN_HANDLER_BEAN_NAME, SpanHandler.class);
					assertThat(spanHandler).isNotNull();
					verify(senderMock, times(1)).check();
				});
	}

	@Test
	public void defaultSchedulerUsedWhenNoneProvided() {
		this.contextRunner
				.withBean(
						StackdriverTraceAutoConfiguration.SPAN_HANDLER_BEAN_NAME,
						SpanHandler.class,
						() ->  SpanHandler.NOOP)
				.run(context -> {
					final ExecutorProvider executorProvider = context.getBean("traceExecutorProvider", ExecutorProvider.class);
					assertThat(executorProvider.getExecutor()).isNotNull();
				});
	}

	@Test
	public void customSchedulerUsedWhenAvailable() {
		ThreadPoolTaskScheduler threadPoolTaskSchedulerMock = mock(ThreadPoolTaskScheduler.class);
		ScheduledExecutorService scheduledExecutorServiceMock = mock(ScheduledExecutorService.class);
		when(threadPoolTaskSchedulerMock.getScheduledExecutor()).thenReturn(scheduledExecutorServiceMock);

		this.contextRunner
				.withBean(
						StackdriverTraceAutoConfiguration.SPAN_HANDLER_BEAN_NAME,
						SpanHandler.class,
						() ->  SpanHandler.NOOP)
				.withBean("traceSenderThreadPool", ThreadPoolTaskScheduler.class, () -> threadPoolTaskSchedulerMock)
				.run(context -> {
					final ExecutorProvider executorProvider = context.getBean("traceExecutorProvider", ExecutorProvider.class);
					assertThat(executorProvider.getExecutor()).isEqualTo(scheduledExecutorServiceMock);
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
					invocationOnMock -> {
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
	static class MultipleSpanHandlersConfig {

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
		TracingCustomizer otherTracingCustomizer(SpanHandler otherSpanHandler) {
			return builder -> builder.addSpanHandler(otherSpanHandler);
		}

		@Bean
		SpanHandler otherSpanHandler(OtherSender otherSender) {
			AsyncReporter reporter = AsyncReporter.create(otherSender);
			SpanHandler spanHandler = AsyncZipkinSpanHandler.create(reporter);
			return spanHandler;
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

			private Map<String, Span> traces = new HashMap<>();

			boolean hasSpan(String spanId) {
				return this.traces.containsKey(spanId);
			}

			Span getSpan(String spanId) {
				return this.traces.get(spanId);
			}

			@Override
			public void batchWriteSpans(BatchWriteSpansRequest request,
					StreamObserver<Empty> responseObserver) {
				request.getSpansList().forEach(span -> this.traces.put(span.getSpanId(), span));
				responseObserver.onNext(Empty.getDefaultInstance());
				responseObserver.onCompleted();
			}

		}

		static class OtherSpanHandler extends SpanHandler {

		}

	}
}
