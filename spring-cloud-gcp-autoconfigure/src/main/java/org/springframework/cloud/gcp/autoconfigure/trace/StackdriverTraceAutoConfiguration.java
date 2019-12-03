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

package org.springframework.cloud.gcp.autoconfigure.trace;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import brave.http.HttpClientParser;
import brave.http.HttpServerParser;
import brave.propagation.Propagation;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import io.grpc.CallOptions;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import zipkin2.CheckResult;
import zipkin2.Span;
import zipkin2.propagation.stackdriver.StackdriverTracePropagation;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.Reporter;
import zipkin2.reporter.ReporterMetrics;
import zipkin2.reporter.Sender;
import zipkin2.reporter.stackdriver.StackdriverEncoder;
import zipkin2.reporter.stackdriver.StackdriverSender;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.trace.sleuth.StackdriverHttpClientParser;
import org.springframework.cloud.gcp.autoconfigure.trace.sleuth.StackdriverHttpServerParser;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;
import org.springframework.cloud.sleuth.autoconfig.SleuthProperties;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.web.TraceHttpAutoConfiguration;
import org.springframework.cloud.sleuth.sampler.SamplerAutoConfiguration;
import org.springframework.cloud.sleuth.sampler.SamplerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
@Configuration
@EnableConfigurationProperties(
		{ SamplerProperties.class, GcpTraceProperties.class, SleuthProperties.class })
@ConditionalOnProperty(value = { "spring.sleuth.enabled", "spring.cloud.gcp.trace.enabled" }, matchIfMissing = true)
@ConditionalOnClass(StackdriverSender.class)
@AutoConfigureBefore(TraceAutoConfiguration.class)
@Import(SamplerAutoConfiguration.class)
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

	private GcpProjectIdProvider finalProjectIdProvider;

	private CredentialsProvider finalCredentialsProvider;

	private UserAgentHeaderProvider headerProvider = new UserAgentHeaderProvider(this.getClass());

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

	@Bean
	@Primary
	public SleuthProperties stackdriverSleuthProperties(SleuthProperties sleuthProperties) {
		sleuthProperties.setSupportsJoin(false);
		sleuthProperties.setTraceId128(true);
		return sleuthProperties;
	}

	@Bean
	@ConditionalOnMissingBean(name = "traceSenderThreadPool")
	public ThreadPoolTaskScheduler traceSenderThreadPool(GcpTraceProperties traceProperties) {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(traceProperties.getNumExecutorThreads());
		scheduler.setThreadNamePrefix("gcp-trace-sender");
		scheduler.setDaemon(true);
		return scheduler;
	}

	@Bean
	@ConditionalOnMissingBean(name = "traceExecutorProvider")
	public ExecutorProvider traceExecutorProvider(@Qualifier("traceSenderThreadPool") ThreadPoolTaskScheduler scheduler) {
		return FixedExecutorProvider.create(scheduler.getScheduledExecutor());
	}

	@Bean(destroyMethod = "shutdownNow")
	@ConditionalOnMissingBean(name = "stackdriverSenderChannel")
	public ManagedChannel stackdriverSenderChannel() {
		return ManagedChannelBuilder.forTarget("cloudtrace.googleapis.com")
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
	public Propagation.Factory stackdriverPropagation() {
		return StackdriverTracePropagation.FACTORY;
	}

	/**
	 * Configuration for Sleuth.
	 */
	@Configuration
	@ConditionalOnProperty(name = "spring.sleuth.http.enabled",
			havingValue = "true", matchIfMissing = true)
	@AutoConfigureBefore(TraceHttpAutoConfiguration.class)
	public static class StackdriverTraceHttpAutoconfiguration {
		@Bean
		@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled",
				havingValue = "false", matchIfMissing = true)
		@ConditionalOnMissingBean
		HttpClientParser stackdriverHttpClientParser() {
			return new StackdriverHttpClientParser();
		}

		@Bean
		@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled",
				havingValue = "false", matchIfMissing = true)
		@ConditionalOnMissingBean
		HttpServerParser stackdriverHttpServerParser() {
			return new StackdriverHttpServerParser();
		}
	}
}
