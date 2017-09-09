/*
 *  Copyright 2017 original author or authors.
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
package org.springframework.cloud.gcp.trace.autoconfig;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.FixedExecutorProvider;
import com.google.cloud.trace.Tracer;
import com.google.cloud.trace.v1.TraceServiceClient;
import com.google.cloud.trace.v1.TraceServiceSettings;
import com.google.cloud.trace.v1.consumer.FlushableTraceConsumer;
import com.google.cloud.trace.v1.consumer.ScheduledBufferingTraceConsumer;
import com.google.cloud.trace.v1.consumer.TraceConsumer;
import com.google.cloud.trace.v1.util.RoughTraceSizer;
import com.google.cloud.trace.v1.util.Sizer;
import com.google.devtools.cloudtrace.v1.Trace;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.IdUtils;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.trace.GcpTraceProperties;
import org.springframework.cloud.gcp.trace.TraceServiceClientTraceConsumer;
import org.springframework.cloud.gcp.trace.sleuth.LabelExtractor;
import org.springframework.cloud.gcp.trace.sleuth.StackdriverTraceSpanListener;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.SpanAdjuster;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.SpanReporter;
import org.springframework.cloud.sleuth.TraceKeys;
import org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration;
import org.springframework.cloud.sleuth.log.SpanLogger;
import org.springframework.cloud.sleuth.sampler.PercentageBasedSampler;
import org.springframework.cloud.sleuth.sampler.SamplerProperties;
import org.springframework.cloud.sleuth.trace.DefaultTracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@EnableConfigurationProperties({ SamplerProperties.class, GcpTraceProperties.class })
@ConditionalOnProperty(value = "spring.cloud.gcp.trace.enabled", matchIfMissing = true)
@AutoConfigureBefore(TraceAutoConfiguration.class)
public class StackdriverTraceAutoConfiguration {
	@Bean
	@ConditionalOnMissingBean
	public Sampler defaultTraceSampler(SamplerProperties config) {
		return new PercentageBasedSampler(config);
	}

	@Bean
	@ConditionalOnMissingBean
	public LabelExtractor traceLabelExtractor() {
		return new LabelExtractor();
	}

	@Bean
	@ConditionalOnMissingBean(name = "traceExecutorProvider")
	public ExecutorProvider traceExecutorProvider(GcpTraceProperties gcpTraceProperties) {
		return FixedExecutorProvider.create(
				Executors.newScheduledThreadPool(gcpTraceProperties.getExecutorThreads()));
	}

	@Bean
	@ConditionalOnMissingBean
	public TraceServiceClient traceServiceClient(CredentialsProvider credentialsProvider,
			@Qualifier("traceExecutorProvider") ExecutorProvider executorProvider) throws IOException {
		return TraceServiceClient.create(TraceServiceSettings.defaultBuilder()
				.setCredentialsProvider(credentialsProvider)
				.setExecutorProvider(executorProvider)
				.build());
	}

	@Bean
	@ConditionalOnMissingBean
	public Sizer<Trace> traceSizer() {
		return new RoughTraceSizer();
	}

	@Bean
	@ConditionalOnMissingBean(name = "syncTraceConsumer")
	public TraceConsumer syncTraceConsumer(GcpProjectIdProvider gcpProjectIdProvider,
			TraceServiceClient traceServiceClient) {
		return new TraceServiceClientTraceConsumer(
				gcpProjectIdProvider.getProjectId(), traceServiceClient);
	}

	@Bean
	@ConditionalOnMissingBean(name = "asyncTraceConsumerExecutorService")
	public ScheduledExecutorService asyncTraceConsumerExecutorService(GcpTraceProperties gcpTraceProperties) {
		return Executors.newScheduledThreadPool(gcpTraceProperties.getExecutorThreads());
	}

	@Bean
	@ConditionalOnMissingBean(name = "asyncTraceConsumer")
	public FlushableTraceConsumer asyncTraceConsumer(
			@Qualifier("syncTraceConsumer") TraceConsumer syncTraceConsumer,
			Sizer<Trace> traceSizer,
			@Qualifier("asyncTraceConsumerExecutorService") ScheduledExecutorService executorService,
			GcpTraceProperties gcpTraceProperties) {
		ScheduledBufferingTraceConsumer scheduledBufferingTraceConsumer = new ScheduledBufferingTraceConsumer(
				syncTraceConsumer, traceSizer, gcpTraceProperties.getBufferSizeBytes(),
				gcpTraceProperties.getScheduledDelaySeconds(), executorService);

		return scheduledBufferingTraceConsumer;
	}

	@Bean
	@ConditionalOnMissingBean
	public Random randomForSpanIds() {
		return new SecureRandom();
	}

	@Bean
	@ConditionalOnMissingBean
	public SpanReporter traceSpanReporter(Environment environment, GcpProjectIdProvider gcpProjectIdProvider,
			LabelExtractor labelExtractor,
			List<SpanAdjuster> spanAdjusters, @Qualifier("asyncTraceConsumer") TraceConsumer traceConsumer) {
		String instanceId = IdUtils.getDefaultInstanceId(environment);
		return new StackdriverTraceSpanListener(instanceId, gcpProjectIdProvider.getProjectId(), labelExtractor,
				spanAdjusters, traceConsumer);
	}

	@Bean
	@ConditionalOnMissingBean(Tracer.class)
	public DefaultTracer stackdriverTracer(Sampler sampler, Random random,
			SpanNamer spanNamer, SpanLogger spanLogger,
			SpanReporter spanReporter, TraceKeys traceKeys) {
		return new DefaultTracer(sampler, random, spanNamer, spanLogger,
				spanReporter, true, traceKeys);
	}

}
