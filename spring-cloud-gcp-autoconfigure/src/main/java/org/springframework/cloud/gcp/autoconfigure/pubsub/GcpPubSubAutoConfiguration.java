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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import java.io.IOException;
import java.util.concurrent.Executors;

import com.google.api.core.ApiClock;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.RetrySettings.Builder;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import org.threeten.bp.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubException;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.pubsub.enabled", matchIfMissing = true)
@ConditionalOnClass(PubSubTemplate.class)
@EnableConfigurationProperties(GcpPubSubProperties.class)
public class GcpPubSubAutoConfiguration {

	private final GcpPubSubProperties gcpPubSubProperties;

	private final GcpProjectIdProvider finalProjectIdProvider;

	private final CredentialsProvider finalCredentialsProvider;

	private final HeaderProvider headerProvider = new UsageTrackingHeaderProvider(this.getClass());

	public GcpPubSubAutoConfiguration(GcpPubSubProperties gcpPubSubProperties,
			GcpProjectIdProvider gcpProjectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {
		this.gcpPubSubProperties = gcpPubSubProperties;
		this.finalProjectIdProvider = gcpPubSubProperties.getProjectId() != null
				? gcpPubSubProperties::getProjectId
				: gcpProjectIdProvider;

		if (gcpPubSubProperties.getEmulatorHost() == null
				|| "false".equals(gcpPubSubProperties.getEmulatorHost())) {
			this.finalCredentialsProvider = gcpPubSubProperties.getCredentials().hasKey()
					? new DefaultCredentialsProvider(gcpPubSubProperties)
					: credentialsProvider;
		}
		else {
			// Since we cannot create a general NoCredentialsProvider if the emulator host is enabled
			// (because it would also be used for the other components), we have to create one here
			// for this particular case.
			this.finalCredentialsProvider = NoCredentialsProvider.create();
		}
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherExecutorProvider")
	public ExecutorProvider publisherExecutorProvider() {
		return FixedExecutorProvider.create(Executors.newScheduledThreadPool(
				this.gcpPubSubProperties.getPublisher().getExecutorThreads()));
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberExecutorProvider")
	public ExecutorProvider subscriberExecutorProvider() {
		return FixedExecutorProvider.create(Executors.newScheduledThreadPool(
				this.gcpPubSubProperties.getSubscriber().getExecutorThreads()));
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubMessageConverter pubSubMessageConverter() {
		return new JacksonPubSubMessageConverter(
				this.gcpPubSubProperties.getTrustedPackages());
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubTemplate pubSubTemplate(PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory,
			PubSubMessageConverter pubSubMessageConverter) {
		return new PubSubTemplate(publisherFactory, subscriberFactory)
				.setMessageConverter(pubSubMessageConverter);
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberRetrySettings")
	public RetrySettings subscriberRetrySettings() {
		return buildRetrySettings(this.gcpPubSubProperties.getSubscriber().getRetry());
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberFlowControlSettings")
	public FlowControlSettings subscriberFlowControlSettings() {
		return buildFlowControlSettings(
				this.gcpPubSubProperties.getSubscriber().getFlowControl());
	}

	private FlowControlSettings buildFlowControlSettings(
			GcpPubSubProperties.FlowControl flowControl) {
		FlowControlSettings.Builder builder = FlowControlSettings.newBuilder();
		flowControl.getLimitExceededBehavior()
				.ifPresent(builder::setLimitExceededBehavior);
		flowControl.getMaxOutstandingElementCount()
				.ifPresent(builder::setMaxOutstandingElementCount);
		flowControl.getMaxOutstandingRequestBytes()
				.ifPresent(builder::setMaxOutstandingRequestBytes);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriberFactory defaultSubscriberFactory(
			@Qualifier("subscriberExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("subscriberSystemExecutorProvider")
			ObjectProvider<ExecutorProvider> systemExecutorProvider,
			@Qualifier("subscriberFlowControlSettings")
					ObjectProvider<FlowControlSettings> flowControlSettings,
			@Qualifier("subscriberApiClock") ObjectProvider<ApiClock> apiClock,
			@Qualifier("subscriberRetrySettings") ObjectProvider<RetrySettings> retrySettings,
			TransportChannelProvider transportChannelProvider) {
		DefaultSubscriberFactory factory = new DefaultSubscriberFactory(this.finalProjectIdProvider);
		factory.setExecutorProvider(executorProvider);
		factory.setCredentialsProvider(this.finalCredentialsProvider);
		factory.setHeaderProvider(this.headerProvider);
		factory.setChannelProvider(transportChannelProvider);
		systemExecutorProvider.ifAvailable(factory::setSystemExecutorProvider);
		flowControlSettings.ifAvailable(factory::setFlowControlSettings);
		apiClock.ifAvailable(factory::setApiClock);
		retrySettings.ifAvailable(factory::setSubscriberStubRetrySettings);
		this.gcpPubSubProperties.getSubscriber().getMaxAckDurationSeconds()
				.ifPresent(x -> factory.setMaxAckDurationPeriod(Duration.ofSeconds(x)));
		this.gcpPubSubProperties.getSubscriber().getParallelPullCount()
				.ifPresent(factory::setParallelPullCount);
		if (this.gcpPubSubProperties.getSubscriber()
				.getPullEndpoint() != null) {
			factory.setPullEndpoint(
					this.gcpPubSubProperties.getSubscriber().getPullEndpoint());
		}
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherBatchSettings")
	public BatchingSettings publisherBatchSettings() {
		BatchingSettings.Builder builder = BatchingSettings.newBuilder();
		GcpPubSubProperties.Batching batching = this.gcpPubSubProperties.getPublisher()
				.getBatching();
		batching.getDelayThresholdSeconds()
				.ifPresent(x -> builder.setDelayThreshold(Duration.ofSeconds(x)));
		batching.getElementCountThreshold().ifPresent(builder::setElementCountThreshold);
		batching.getIsEnabled().ifPresent(builder::setIsEnabled);
		batching.getRequestByteThreshold().ifPresent(builder::setRequestByteThreshold);
		builder.setFlowControlSettings(
				buildFlowControlSettings(batching.getFlowControl()));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherRetrySettings")
	public RetrySettings publisherRetrySettings() {
		return buildRetrySettings(this.gcpPubSubProperties.getPublisher().getRetry());
	}

	private RetrySettings buildRetrySettings(GcpPubSubProperties.Retry retryProperties) {
		Builder builder = RetrySettings.newBuilder();
		retryProperties.getInitialRetryDelaySeconds()
				.ifPresent(x -> builder.setInitialRetryDelay(Duration.ofSeconds(x)));
		retryProperties.getInitialRpcTimeoutSeconds()
				.ifPresent(x -> builder.setInitialRpcTimeout(Duration.ofSeconds(x)));
		retryProperties.getJittered().ifPresent(builder::setJittered);
		retryProperties.getMaxAttempts().ifPresent(builder::setMaxAttempts);
		retryProperties.getMaxRetryDelaySeconds()
				.ifPresent(x -> builder.setMaxRetryDelay(Duration.ofSeconds(x)));
		retryProperties.getMaxRpcTimeoutSeconds()
				.ifPresent(x -> builder.setMaxRpcTimeout(Duration.ofSeconds(x)));
		retryProperties.getRetryDelayMultiplier()
				.ifPresent(builder::setRetryDelayMultiplier);
		retryProperties.getTotalTimeoutSeconds()
				.ifPresent(x -> builder.setTotalTimeout(Duration.ofSeconds(x)));
		retryProperties.getRpcTimeoutMultiplier()
				.ifPresent(builder::setRpcTimeoutMultiplier);
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public PublisherFactory defaultPublisherFactory(
			@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("publisherBatchSettings") ObjectProvider<BatchingSettings> batchingSettings,
			@Qualifier("publisherRetrySettings") ObjectProvider<RetrySettings> retrySettings,
			TransportChannelProvider transportChannelProvider) {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(this.finalProjectIdProvider);
		factory.setExecutorProvider(executorProvider);
		factory.setCredentialsProvider(this.finalCredentialsProvider);
		factory.setHeaderProvider(this.headerProvider);
		factory.setChannelProvider(transportChannelProvider);
		retrySettings.ifAvailable(factory::setRetrySettings);
		batchingSettings.ifAvailable(factory::setBatchingSettings);
		return factory;
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubAdmin pubSubAdmin(TopicAdminClient topicAdminClient,
			SubscriptionAdminClient subscriptionAdminClient) {
		return new PubSubAdmin(this.finalProjectIdProvider, topicAdminClient,
				subscriptionAdminClient);
	}

	@Bean
	@ConditionalOnMissingBean
	public TopicAdminClient topicAdminClient(
			TopicAdminSettings topicAdminSettings) {
		try {
			return TopicAdminClient.create(topicAdminSettings);
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating TopicAdminClient.", ioe);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public TopicAdminSettings topicAdminSettings(
			TransportChannelProvider transportChannelProvider) {
		try {
			return TopicAdminSettings.newBuilder()
					.setCredentialsProvider(this.finalCredentialsProvider)
					.setHeaderProvider(this.headerProvider)
					.setTransportChannelProvider(transportChannelProvider)
					.build();
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating TopicAdminSettings.", ioe);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriptionAdminClient subscriptionAdminClient(
			TransportChannelProvider transportChannelProvider) {
		try {
			return SubscriptionAdminClient.create(
					SubscriptionAdminSettings.newBuilder()
							.setCredentialsProvider(this.finalCredentialsProvider)
							.setHeaderProvider(this.headerProvider)
							.setTransportChannelProvider(transportChannelProvider)
							.build());
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating SubscriptionAdminClient.", ioe);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public TransportChannelProvider transportChannelProvider() {
		return InstantiatingGrpcChannelProvider.newBuilder().build();
	}
}
