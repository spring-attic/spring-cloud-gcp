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
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.v1.TraceServiceSettings;
import com.google.cloud.trace.v1.consumer.ScheduledBufferingTraceConsumer;
import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.cloud.trace.v1.util.RoughTraceSizer;
import com.google.cloud.trace.v1.util.Sizer;
import com.google.devtools.cloudtrace.v1.Trace;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.gcp.trace.TraceServiceClientTraceConsumer;
import org.springframework.cloud.gcp.trace.sleuth.LabelExtractor;
import org.springframework.cloud.gcp.trace.sleuth.StackdriverTraceSpanListener;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.SpanAdjuster;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SpanLogger;
import org.springframework.cloud.sleuth.sampler.PercentageBasedSampler;
import org.springframework.cloud.sleuth.sampler.SamplerProperties;
import org.springframework.cloud.sleuth.trace.DefaultTracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

/**
 * @author Ray Tsang
 * @author João André Martins
 * @author Mike Eltsufin
 */
@Configuration
@EnableConfigurationProperties({ SamplerProperties.class, GcpTraceProperties.class })
@ConditionalOnProperty(value = "spring.cloud.gcp.trace.enabled", matchIfMissing = true)
@ConditionalOnClass(TraceConsumer.class)
@Import(StackdriverTraceAutoConfiguration.TraceConsumerConfiguration.class)
@AutoConfigureBefore(TraceAutoConfiguration.class)
public class StackdriverTraceAutoConfiguration {

	private GcpProjectIdProvider finalProjectIdProvider;

	private CredentialsProvider finalCredentialsProvider;

	private HeaderProvider headerProvider =	new UsageTrackingHeaderProvider(this.getClass());

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
	public Sampler defaultTraceSampler(SamplerProperties config) {
		return new PercentageBasedSampler(config);
	}

	@Bean
	@ConditionalOnMissingBean
	public Random randomForSpanIds() {
		return new SecureRandom();
	}

	@Bean
	@ConditionalOnMissingBean
	public SpanReporter traceSpanReporter(Environment environment,
			LabelExtractor labelExtractor,
			List<SpanAdjuster> spanAdjusters, TraceConsumer traceConsumer) {
		String instanceId = IdUtils.getDefaultInstanceId(environment);
		return new StackdriverTraceSpanListener(instanceId,
				this.finalProjectIdProvider.getProjectId(), labelExtractor,	spanAdjusters,
				traceConsumer);
	}

	@Bean
	@ConditionalOnMissingBean(Tracer.class)
	public Tracer sleuthTracer(Sampler sampler, Random random,
			SpanNamer spanNamer, SpanLogger spanLogger,
			SpanReporter spanReporter, TraceKeys traceKeys) {
		return new DefaultTracer(sampler, random, spanNamer, spanLogger,
				spanReporter, true, traceKeys);
	}

	@Bean
	@ConditionalOnMissingBean
	public LabelExtractor traceLabelExtractor(TraceKeys traceKeys) {
		return new LabelExtractor(traceKeys);
	}

	@Configuration
	@ConditionalOnMissingBean(TraceConsumer.class)
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
		public TraceConsumer traceConsumer(
				TraceServiceClientTraceConsumer traceServiceClientTraceConsumer,
				Sizer<Trace> traceSizer,
				@Qualifier("scheduledBufferingExecutorService")
						ScheduledExecutorService executorService,
				GcpTraceProperties gcpTraceProperties) {
			return new ScheduledBufferingTraceConsumer(
					traceServiceClientTraceConsumer,
					traceSizer, gcpTraceProperties.getBufferSizeBytes(),
					gcpTraceProperties.getScheduledDelaySeconds(), executorService);
		}
	}
}
