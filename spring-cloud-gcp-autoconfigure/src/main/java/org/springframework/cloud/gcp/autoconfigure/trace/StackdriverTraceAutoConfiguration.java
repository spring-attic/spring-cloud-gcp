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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import brave.Tracing;
import brave.http.HttpClientParser;
import brave.http.HttpServerParser;
import brave.propagation.CurrentTraceContext;
import brave.propagation.Propagation;
import brave.sampler.Sampler;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.v1.TraceServiceSettings;
import com.google.cloud.trace.v1.consumer.FlushableTraceConsumer;
import com.google.cloud.trace.v1.consumer.ScheduledBufferingTraceConsumer;
import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.cloud.trace.v1.util.RoughTraceSizer;
import com.google.cloud.trace.v1.util.Sizer;
import com.google.devtools.cloudtrace.v1.Trace;
import zipkin2.Span;
import zipkin2.reporter.Reporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.gcp.trace.TraceServiceClientTraceConsumer;
import org.springframework.cloud.gcp.trace.sleuth.LabelExtractor;
import org.springframework.cloud.gcp.trace.sleuth.SpanTranslator;
import org.springframework.cloud.gcp.trace.sleuth.StackdriverHttpClientParser;
import org.springframework.cloud.gcp.trace.sleuth.StackdriverHttpServerParser;
import org.springframework.cloud.gcp.trace.sleuth.StackdriverTraceReporter;
import org.springframework.cloud.sleuth.SpanAdjuster;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.sampler.ProbabilityBasedSampler;
import org.springframework.cloud.sleuth.sampler.SamplerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * @author Ray Tsang
 * @author João André Martins
 * @author Mike Eltsufin
 */
@Configuration
@EnableConfigurationProperties({ SamplerProperties.class, GcpTraceProperties.class })
@ConditionalOnProperty(value = "spring.cloud.gcp.trace.enabled", matchIfMissing = true)
@ConditionalOnClass(TraceConsumer.class)
@Import({ StackdriverTraceAutoConfiguration.TraceConsumerConfiguration.class,
		StackdriverTraceAutoConfiguration.StackdriverTraceHttpAutoconfiguration.class })
@AutoConfigureBefore({ TraceAutoConfiguration.class })
public class StackdriverTraceAutoConfiguration {

	@Autowired(required = false)
	List<SpanAdjuster> spanAdjusters = new ArrayList<>();

	private GcpProjectIdProvider finalProjectIdProvider;

	private CredentialsProvider finalCredentialsProvider;

	private HeaderProvider headerProvider = new UsageTrackingHeaderProvider(this.getClass());

	public StackdriverTraceAutoConfiguration(GcpProjectIdProvider gcpProjectIdProvider,
			CredentialsProvider credentialsProvider,
			GcpTraceProperties gcpTraceProperties) throws IOException {
		this.finalProjectIdProvider = gcpTraceProperties.getProjectId() != null
				? gcpTraceProperties::getProjectId
				: gcpProjectIdProvider;
		this.finalCredentialsProvider = gcpTraceProperties.getCredentials().getLocation() != null
				? new DefaultCredentialsProvider(gcpTraceProperties)
				: credentialsProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	Tracing tracing(@Value("${spring.zipkin.service.name:${spring.application.name:default}}") String serviceName,
			Propagation.Factory factory,
			CurrentTraceContext currentTraceContext,
			Reporter<zipkin2.Span> reporter,
			Sampler sampler) {
		return Tracing.newBuilder()
				.sampler(sampler)
				.traceId128Bit(true)
				.supportsJoin(false)
				.localServiceName(serviceName)
				.propagationFactory(factory)
				.currentTraceContext(currentTraceContext)
				.spanReporter(adjustedReporter(reporter)).build();
	}

	private Reporter<zipkin2.Span> adjustedReporter(Reporter<zipkin2.Span> delegate) {
		return span -> {
			Span spanToAdjust = span;
			for (SpanAdjuster spanAdjuster : this.spanAdjusters) {
				spanToAdjust = spanAdjuster.adjust(spanToAdjust);
			}
			delegate.report(spanToAdjust);
		};
	}

	@Bean
	@ConditionalOnMissingBean
	public Reporter<Span> reporter(
			GcpProjectIdProvider projectIdProvider,
			FlushableTraceConsumer traceConsumer,
			SpanTranslator spanTranslator) {
		return new StackdriverTraceReporter(projectIdProvider.getProjectId(), traceConsumer, spanTranslator);
	}

	@Bean
	@ConditionalOnMissingBean
	public SpanTranslator spanTranslator(LabelExtractor labelExtractor) {
		return new SpanTranslator(labelExtractor);
	}

	@Bean
	@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled", havingValue = "false", matchIfMissing = true)
	@ConditionalOnMissingBean
	public LabelExtractor traceLabelExtractor() {
		return new LabelExtractor();
	}

	@Bean
	@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled", havingValue = "true")
	@ConditionalOnMissingBean
	public LabelExtractor traceLabelExtractor(TraceKeys traceKeys) {
		return new LabelExtractor(traceKeys);
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
	@ConditionalOnProperty(name = "spring.sleuth.http.enabled", havingValue = "true", matchIfMissing = true)
	public static class StackdriverTraceHttpAutoconfiguration {
		@Bean
		@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled", havingValue = "false", matchIfMissing = true)
		@ConditionalOnMissingBean
		HttpClientParser stackdriverHttpClientParser() {
			return new StackdriverHttpClientParser();
		}

		@Bean
		@ConditionalOnProperty(name = "spring.sleuth.http.legacy.enabled", havingValue = "false", matchIfMissing = true)
		@ConditionalOnMissingBean
		HttpServerParser stackdriverHttpServerParser() {
			return new StackdriverHttpServerParser();
		}
	}

	@Configuration
	@ConditionalOnMissingBean(FlushableTraceConsumer.class)
	public class TraceConsumerConfiguration {
		@Bean
		@ConditionalOnMissingBean(name = "traceExecutorProvider")
		public ExecutorProvider traceExecutorProvider(GcpTraceProperties gcpTraceProperties) {
			return FixedExecutorProvider.create(
					Executors.newScheduledThreadPool(gcpTraceProperties.getExecutorThreads()));
		}

		@Bean
		@ConditionalOnMissingBean
		public TraceServiceClient traceServiceClient(
				@Qualifier("traceExecutorProvider") ExecutorProvider executorProvider)
				throws IOException {
			return TraceServiceClient.create(
					TraceServiceSettings.newBuilder()
							.setCredentialsProvider(StackdriverTraceAutoConfiguration.this.finalCredentialsProvider)
							.setExecutorProvider(executorProvider)
							.setHeaderProvider(StackdriverTraceAutoConfiguration.this.headerProvider)
							.build());
		}

		@Bean
		@ConditionalOnMissingBean(name = "scheduledBufferingExecutorService")
		public ScheduledExecutorService scheduledBufferingExecutorService() {
			return Executors.newSingleThreadScheduledExecutor();
		}

		@Bean
		@ConditionalOnMissingBean(name = "traceServiceClientTraceConsumer")
		public TraceServiceClientTraceConsumer traceServiceClientTraceConsumer(
				TraceServiceClient traceServiceClient) {
			return new TraceServiceClientTraceConsumer(
					StackdriverTraceAutoConfiguration.this.finalProjectIdProvider.getProjectId(),
					traceServiceClient);

		}

		@Bean
		@ConditionalOnMissingBean
		public Sizer<Trace> traceSizer() {
			return new RoughTraceSizer();
		}

		@Bean
		@ConditionalOnMissingBean(name = "traceConsumerExecutorService")
		public ScheduledExecutorService traceConsumerExecutorService(
				GcpTraceProperties gcpTraceProperties) {
			return Executors.newScheduledThreadPool(gcpTraceProperties.getExecutorThreads());
		}

		@Primary
		@Bean
		@ConditionalOnMissingBean(name = "traceConsumer")
		public FlushableTraceConsumer traceConsumer(
				TraceServiceClientTraceConsumer traceServiceClientTraceConsumer,
				Sizer<Trace> traceSizer,
				@Qualifier("scheduledBufferingExecutorService") ScheduledExecutorService executorService,
				GcpTraceProperties gcpTraceProperties) {
			return new ScheduledBufferingTraceConsumer(
					traceServiceClientTraceConsumer,
					traceSizer, gcpTraceProperties.getBufferSizeBytes(),
					gcpTraceProperties.getScheduledDelaySeconds(), executorService);
		}
	}
}
