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

package com.google.cloud.spring.autoconfigure.trace;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import brave.TracingCustomizer;
import brave.baggage.BaggagePropagation;
import brave.handler.SpanHandler;
import brave.http.HttpRequestParser;
import brave.http.HttpTracingCustomizer;
import brave.propagation.B3Propagation;
import brave.propagation.Propagation;
import brave.propagation.stackdriver.StackdriverTracePropagation;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.cloud.spring.autoconfigure.trace.sleuth.StackdriverHttpRequestParser;
import com.google.cloud.spring.core.DefaultCredentialsProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.core.UserAgentHeaderProvider;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import zipkin2.CheckResult;
import zipkin2.Span;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.ReporterMetrics;
import zipkin2.reporter.Sender;
import zipkin2.reporter.brave.AsyncZipkinSpanHandler;
import zipkin2.reporter.stackdriver.StackdriverEncoder;
import zipkin2.reporter.stackdriver.StackdriverSender;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.sleuth.autoconfig.brave.BraveAutoConfiguration;
import org.springframework.cloud.sleuth.autoconfig.brave.instrument.web.BraveHttpConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Config for Stackdriver Trace.
 *
 * @author Ray Tsang
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Tim Ysewyn
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ GcpTraceProperties.class })
@ConditionalOnProperty(value = { "spring.sleuth.enabled", "spring.cloud.gcp.trace.enabled" }, matchIfMissing = true)
@ConditionalOnClass(StackdriverSender.class)
@AutoConfigureBefore(BraveAutoConfiguration.class)
public class StackdriverTraceAutoConfiguration {

	private static final Log LOGGER = LogFactory.getLog(StackdriverTraceAutoConfiguration.class);

	/**
	 * Stackdriver reporter bean name. Name of the bean matters for supporting multiple tracing
	 * systems.
	 */
	public static final String REPORTER_BEAN_NAME = "stackdriverReporter";

	/**
	 * Stackdriver sender bean name. Name of the bean matters for supporting multiple tracing
	 * systems.
	 */
	public static final String SENDER_BEAN_NAME = "stackdriverSender";

	/**
	 * Stackdriver span handler bean name. Name of the bean matters for supporting multiple tracing systems.
	 */
	public static final String SPAN_HANDLER_BEAN_NAME = "stackdriverSpanHandler";

	/**
	 * Stackdriver customizer bean name. Name of the bean matters for supporting multiple tracing systems.
	 */
	public static final String CUSTOMIZER_BEAN_NAME = "stackdriverTracingCustomizer";

	private GcpProjectIdProvider finalProjectIdProvider;

	private CredentialsProvider finalCredentialsProvider;

	private UserAgentHeaderProvider headerProvider = new UserAgentHeaderProvider(this.getClass());

	private ThreadPoolTaskScheduler defaultTraceSenderThreadPool;

	public StackdriverTraceAutoConfiguration(GcpProjectIdProvider gcpProjectIdProvider,
			CredentialsProvider credentialsProvider,
			GcpTraceProperties gcpTraceProperties) throws IOException {
		this.finalProjectIdProvider = (gcpTraceProperties.getProjectId() != null)
				? gcpTraceProperties::getProjectId
				: gcpProjectIdProvider;
		this.finalCredentialsProvider =
				gcpTraceProperties.getCredentials().hasKey()
						? new DefaultCredentialsProvider(gcpTraceProperties)
						: credentialsProvider;
	}

	@Bean(CUSTOMIZER_BEAN_NAME)
	@ConditionalOnMissingBean(name = CUSTOMIZER_BEAN_NAME)
	public TracingCustomizer stackdriverTracingCustomizer(@Qualifier(SPAN_HANDLER_BEAN_NAME) SpanHandler spanHandler) {
		return builder -> builder
					.supportsJoin(false)
					.traceId128Bit(true)
					.addSpanHandler(spanHandler);
	}

	@Bean(SPAN_HANDLER_BEAN_NAME)
	@ConditionalOnMissingBean(name = SPAN_HANDLER_BEAN_NAME)
	public SpanHandler stackdriverSpanHandler(@Qualifier(REPORTER_BEAN_NAME) Reporter<Span> stackdriverReporter) {
		SpanHandler spanHandler = AsyncZipkinSpanHandler.create(stackdriverReporter);
		return spanHandler;
	}

	@Bean
	@ConditionalOnMissingBean
	ReporterMetrics sleuthReporterMetrics() {
		return ReporterMetrics.NOOP_METRICS;
	}

	@Bean
	@ConditionalOnMissingBean(name = "traceExecutorProvider")
	public ExecutorProvider traceExecutorProvider(GcpTraceProperties traceProperties, @Qualifier("traceSenderThreadPool") Optional<ThreadPoolTaskScheduler> userProvidedScheduler) {
		ThreadPoolTaskScheduler scheduler;
		if (userProvidedScheduler.isPresent()) {
			scheduler = userProvidedScheduler.get();
		}
		else {
			this.defaultTraceSenderThreadPool = new ThreadPoolTaskScheduler();
			scheduler = this.defaultTraceSenderThreadPool;
			scheduler.setPoolSize(traceProperties.getNumExecutorThreads());
			scheduler.setThreadNamePrefix("gcp-trace-sender");
			scheduler.setDaemon(true);
			scheduler.initialize();
		}
		return FixedExecutorProvider.create(scheduler.getScheduledExecutor());
	}

	@Bean(destroyMethod = "shutdownNow")
	@ConditionalOnMissingBean(name = "stackdriverSenderChannel")
	public ManagedChannel stackdriverSenderChannel() {
		return ManagedChannelBuilder.forTarget("dns:///cloudtrace.googleapis.com")
				.userAgent(this.headerProvider.getUserAgent())
				.build();
	}

	@Bean(REPORTER_BEAN_NAME)
	@ConditionalOnMissingBean(name = REPORTER_BEAN_NAME)
	public Reporter<Span> stackdriverReporter(ReporterMetrics reporterMetrics,
			GcpTraceProperties trace, @Qualifier(SENDER_BEAN_NAME) Sender sender) {

		AsyncReporter<Span> asyncReporter = AsyncReporter.builder(sender)
				// historical constraint. Note: AsyncReporter supports memory bounds
				.queuedMaxSpans(1000)
				.messageTimeout(trace.getMessageTimeout(), TimeUnit.SECONDS)
				.metrics(reporterMetrics).build(StackdriverEncoder.V2);

		CheckResult checkResult = asyncReporter.check();
		if (!checkResult.ok()) {
			LOGGER.warn(
					"Error when performing Stackdriver AsyncReporter health check.", checkResult.error());
		}

		return asyncReporter;
	}

	@Bean(SENDER_BEAN_NAME)
	@ConditionalOnMissingBean(name = SENDER_BEAN_NAME)
	public Sender stackdriverSender(GcpTraceProperties traceProperties,
			@Qualifier("traceExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("stackdriverSenderChannel") ManagedChannel channel)
			throws IOException {
		CallOptions callOptions = CallOptions.DEFAULT
				.withCallCredentials(
						MoreCallCredentials.from(
								this.finalCredentialsProvider.getCredentials()))
				.withExecutor(executorProvider.getExecutor());

		if (traceProperties.getAuthority() != null) {
			callOptions = callOptions.withAuthority(traceProperties.getAuthority());
		}

		if (traceProperties.getCompression() != null) {
			callOptions = callOptions.withCompression(traceProperties.getCompression());
		}

		if (traceProperties.getDeadlineMs() != null) {
			callOptions = callOptions.withDeadlineAfter(traceProperties.getDeadlineMs(), TimeUnit.MILLISECONDS);
		}

		if (traceProperties.getMaxInboundSize() != null) {
			callOptions = callOptions.withMaxInboundMessageSize(traceProperties.getMaxInboundSize());
		}

		if (traceProperties.getMaxOutboundSize() != null) {
			callOptions = callOptions.withMaxOutboundMessageSize(traceProperties.getMaxOutboundSize());
		}

		if (traceProperties.isWaitForReady() != null) {
			if (Boolean.TRUE.equals(traceProperties.isWaitForReady())) {
				callOptions = callOptions.withWaitForReady();
			}
			else {
				callOptions = callOptions.withoutWaitForReady();
			}
		}

		return StackdriverSender.newBuilder(channel)
				.projectId(this.finalProjectIdProvider.getProjectId())
				.callOptions(callOptions)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public BaggagePropagation.FactoryBuilder baggagePropagationFactoryBuilder() {
		Propagation.Factory primary = B3Propagation.newFactoryBuilder().injectFormat(B3Propagation.Format.MULTI).build();
		return BaggagePropagation.newFactoryBuilder(StackdriverTracePropagation.newFactory(primary));
	}

	@PreDestroy
	public void closeScheduler() {
		if (this.defaultTraceSenderThreadPool != null) {
			this.defaultTraceSenderThreadPool.shutdown();
		}
	}

	/**
	 * Configuration for Sleuth.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnProperty(name = "spring.sleuth.http.enabled",
			havingValue = "true", matchIfMissing = true)
	@AutoConfigureBefore(BraveHttpConfiguration.class)
	public static class StackdriverTraceHttpAutoconfiguration {
		@Bean
		@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled", havingValue = "false", matchIfMissing = true)
		@ConditionalOnMissingBean
		HttpRequestParser stackdriverHttpRequestParser() {
			return new StackdriverHttpRequestParser();
		}

		@Bean
		@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled", havingValue = "false", matchIfMissing = true)
		@ConditionalOnMissingBean
		HttpTracingCustomizer stackdriverHttpTracingCustomizer(HttpRequestParser stackdriverHttpRequestParser) {
			return builder -> builder.clientRequestParser(stackdriverHttpRequestParser);
		}
	}
}
