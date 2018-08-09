/*
 *  Copyright 2017-2018 original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.trace;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import brave.http.HttpClientParser;
import brave.http.HttpServerParser;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.rpc.HeaderProvider;
import io.grpc.CallOptions;
import io.grpc.auth.MoreCallCredentials;
import zipkin2.Span;
import zipkin2.codec.BytesEncoder;
import zipkin2.propagation.stackdriver.StackdriverTracePropagation;
import zipkin2.reporter.Sender;
import zipkin2.reporter.stackdriver.StackdriverEncoder;
import zipkin2.reporter.stackdriver.StackdriverSender;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gcp.autoconfigure.trace.sleuth.StackdriverHttpClientParser;
import org.springframework.cloud.gcp.autoconfigure.trace.sleuth.StackdriverHttpServerParser;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.sleuth.autoconfig.SleuthProperties;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.instrument.web.TraceHttpAutoConfiguration;
import org.springframework.cloud.sleuth.sampler.ProbabilityBasedSampler;
import org.springframework.cloud.sleuth.sampler.SamplerProperties;
import org.springframework.cloud.sleuth.zipkin2.ZipkinAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author Ray Tsang
 * @author João André Martins
 * @author Mike Eltsufin
 */
@Configuration
@EnableConfigurationProperties(
		{ SamplerProperties.class, GcpTraceProperties.class, SleuthProperties.class })
@ConditionalOnProperty(value = "spring.cloud.gcp.trace.enabled", matchIfMissing = true)
@ConditionalOnClass(StackdriverSender.class)
@AutoConfigureBefore({ ZipkinAutoConfiguration.class, TraceAutoConfiguration.class})
public class StackdriverTraceAutoConfiguration {

	private GcpProjectIdProvider finalProjectIdProvider;

	private CredentialsProvider finalCredentialsProvider;

	private HeaderProvider headerProvider = new UsageTrackingHeaderProvider(this.getClass());

	public StackdriverTraceAutoConfiguration(GcpProjectIdProvider gcpProjectIdProvider,
			CredentialsProvider credentialsProvider,
			GcpTraceProperties gcpTraceProperties) throws IOException {
		this.finalProjectIdProvider = gcpTraceProperties.getProjectId() != null
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
	@ConditionalOnMissingBean(name = "traceExecutorProvider")
	public ExecutorProvider traceExecutorProvider(GcpTraceProperties traceProperties) {
		return FixedExecutorProvider.create(
				Executors.newScheduledThreadPool(traceProperties.getNumExecutorThreads()));
	}

	@Bean
	@ConditionalOnMissingBean
	public Sender stackdriverSender(GcpTraceProperties traceProperties,
			@Qualifier("traceExecutorProvider") ExecutorProvider executorProvider)
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

		return StackdriverSender.newBuilder()
				.projectId(this.finalProjectIdProvider.getProjectId())
				.callOptions(callOptions)
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public BytesEncoder<Span> spanBytesEncoder() {
		return StackdriverEncoder.V1;
	}

	@Bean
	@ConditionalOnMissingBean
	public Propagation.Factory stackdriverPropagation() {
		return StackdriverTracePropagation.FACTORY;
	}

	@Configuration
	@ConditionalOnClass(RefreshScope.class)
	protected static class RefreshScopedProbabilityBasedSamplerConfiguration {
		@Bean
		@RefreshScope
		@ConditionalOnMissingBean
		public Sampler defaultTraceSampler(SamplerProperties config) {
			return new ProbabilityBasedSampler(config);
		}
	}

	@Configuration
	@ConditionalOnMissingClass("org.springframework.cloud.context.config.annotation.RefreshScope")
	protected static class NonRefreshScopeProbabilityBasedSamplerConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public Sampler defaultTraceSampler(SamplerProperties config) {
			return new ProbabilityBasedSampler(config);
		}
	}

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
