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

package com.google.cloud.spring.autoconfigure.pubsub;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import com.google.api.core.ApiClock;
import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.retrying.RetrySettings.Builder;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.pubsub.v1.stub.PublisherStubSettings;
import com.google.cloud.pubsub.v1.stub.SubscriberStubSettings;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.core.DefaultCredentialsProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.core.UserAgentHeaderProvider;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubException;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.core.publisher.PubSubPublisherTemplate;
import com.google.cloud.spring.pubsub.core.subscriber.PubSubSubscriberTemplate;
import com.google.cloud.spring.pubsub.support.DefaultPublisherFactory;
import com.google.cloud.spring.pubsub.support.DefaultSubscriberFactory;
import com.google.cloud.spring.pubsub.support.PublisherFactory;
import com.google.cloud.spring.pubsub.support.SubscriberFactory;
import com.google.cloud.spring.pubsub.support.converter.PubSubMessageConverter;
import org.threeten.bp.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Auto-config for Pub/Sub.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
 * @author Maurice Zeijen
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.pubsub.enabled", matchIfMissing = true)
@ConditionalOnClass(PubSubTemplate.class)
@EnableConfigurationProperties(GcpPubSubProperties.class)
public class GcpPubSubAutoConfiguration {

	private final GcpPubSubProperties gcpPubSubProperties;

	private final GcpProjectIdProvider finalProjectIdProvider;

	private final CredentialsProvider finalCredentialsProvider;

	private final HeaderProvider headerProvider = new UserAgentHeaderProvider(this.getClass());

	public GcpPubSubAutoConfiguration(GcpPubSubProperties gcpPubSubProperties,
			GcpProjectIdProvider gcpProjectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {
		this.gcpPubSubProperties = gcpPubSubProperties;
		this.finalProjectIdProvider = (gcpPubSubProperties.getProjectId() != null)
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
	@ConditionalOnMissingBean(name = "pubsubPublisherThreadPool")
	public ThreadPoolTaskScheduler pubsubPublisherThreadPool() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(this.gcpPubSubProperties.getPublisher().getExecutorThreads());
		scheduler.setThreadNamePrefix("gcp-pubsub-publisher");
		scheduler.setDaemon(true);
		return scheduler;
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherExecutorProvider")
	public ExecutorProvider publisherExecutorProvider(
			@Qualifier("pubsubPublisherThreadPool") ThreadPoolTaskScheduler scheduler) {
		return FixedExecutorProvider.create(scheduler.getScheduledExecutor());
	}

	@Bean
	@ConditionalOnMissingBean(name = "pubsubSubscriberThreadPool")
	public ThreadPoolTaskScheduler pubsubSubscriberThreadPool() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(this.gcpPubSubProperties.getSubscriber().getExecutorThreads());
		scheduler.setThreadNamePrefix("gcp-pubsub-subscriber");
		scheduler.setDaemon(true);
		return scheduler;
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberExecutorProvider")
	public ExecutorProvider subscriberExecutorProvider(
			@Qualifier("pubsubSubscriberThreadPool") ThreadPoolTaskScheduler scheduler) {
		return FixedExecutorProvider.create(scheduler.getScheduledExecutor());
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubPublisherTemplate pubSubPublisherTemplate(PublisherFactory publisherFactory,
			ObjectProvider<PubSubMessageConverter> pubSubMessageConverter) {
		PubSubPublisherTemplate pubSubPublisherTemplate = new PubSubPublisherTemplate(publisherFactory);
		pubSubMessageConverter.ifUnique(pubSubPublisherTemplate::setMessageConverter);
		return pubSubPublisherTemplate;
	}

	@Bean
	@ConditionalOnMissingBean(name = "pubSubAcknowledgementExecutor")
	public Executor pubSubAcknowledgementExecutor() {
		ThreadPoolTaskExecutor ackExecutor = new ThreadPoolTaskExecutor();
		ackExecutor.setMaxPoolSize(this.gcpPubSubProperties.getSubscriber().getMaxAcknowledgementThreads());
		ackExecutor.setThreadNamePrefix("gcp-pubsub-ack-executor");
		ackExecutor.setDaemon(true);
		return ackExecutor;
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubSubscriberTemplate pubSubSubscriberTemplate(SubscriberFactory subscriberFactory,
			ObjectProvider<PubSubMessageConverter> pubSubMessageConverter,
			@Qualifier("pubSubAsynchronousPullExecutor") ObjectProvider<Executor> asyncPullExecutor,
			@Qualifier("pubSubAcknowledgementExecutor") Executor ackExecutor) {
		PubSubSubscriberTemplate pubSubSubscriberTemplate = new PubSubSubscriberTemplate(subscriberFactory);
		pubSubMessageConverter.ifUnique(pubSubSubscriberTemplate::setMessageConverter);
		pubSubSubscriberTemplate.setAckExecutor(ackExecutor);
		asyncPullExecutor.ifAvailable(pubSubSubscriberTemplate::setAsyncPullExecutor);
		return pubSubSubscriberTemplate;
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubTemplate pubSubTemplate(PubSubPublisherTemplate pubSubPublisherTemplate,
			PubSubSubscriberTemplate pubSubSubscriberTemplate) {
		return new PubSubTemplate(pubSubPublisherTemplate, pubSubSubscriberTemplate);
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

		boolean shouldBuild = ifSet(flowControl.getLimitExceededBehavior(), builder::setLimitExceededBehavior);
		shouldBuild |= ifSet(flowControl.getMaxOutstandingElementCount(), builder::setMaxOutstandingElementCount);
		shouldBuild |= ifSet(flowControl.getMaxOutstandingRequestBytes(), builder::setMaxOutstandingRequestBytes);

		return shouldBuild ? builder.build() : null;
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
			@Qualifier("subscriberTransportChannelProvider") TransportChannelProvider subscriberTransportChannelProvider) {
		DefaultSubscriberFactory factory = new DefaultSubscriberFactory(this.finalProjectIdProvider);
		factory.setExecutorProvider(executorProvider);
		factory.setCredentialsProvider(this.finalCredentialsProvider);
		factory.setHeaderProvider(this.headerProvider);
		factory.setChannelProvider(subscriberTransportChannelProvider);
		systemExecutorProvider.ifAvailable(factory::setSystemExecutorProvider);
		flowControlSettings.ifAvailable(factory::setFlowControlSettings);
		apiClock.ifAvailable(factory::setApiClock);
		retrySettings.ifAvailable(factory::setSubscriberStubRetrySettings);
		if (this.gcpPubSubProperties.getSubscriber().getMaxAckExtensionPeriod() != null) {
			factory.setMaxAckExtensionPeriod(Duration.ofSeconds(
					this.gcpPubSubProperties.getSubscriber().getMaxAckExtensionPeriod()));
		}
		if (this.gcpPubSubProperties.getSubscriber().getParallelPullCount() != null) {
			factory.setParallelPullCount(
					this.gcpPubSubProperties.getSubscriber().getParallelPullCount());
		}
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

		FlowControlSettings flowControlSettings = buildFlowControlSettings(batching.getFlowControl());
		if (flowControlSettings != null) {
			builder.setFlowControlSettings(flowControlSettings);
		}

		boolean shouldBuild = ifSet(batching.getDelayThresholdSeconds(), x -> builder.setDelayThreshold(Duration.ofSeconds(x)));
		shouldBuild |= ifSet(batching.getElementCountThreshold(), builder::setElementCountThreshold);
		shouldBuild |= ifSet(batching.getEnabled(), builder::setIsEnabled);
		shouldBuild |= ifSet(batching.getRequestByteThreshold(), builder::setRequestByteThreshold);

		return shouldBuild ? builder.build() : null;
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherRetrySettings")
	public RetrySettings publisherRetrySettings() {
		return buildRetrySettings(this.gcpPubSubProperties.getPublisher().getRetry());
	}

	private RetrySettings buildRetrySettings(GcpPubSubProperties.Retry retryProperties) {
		Builder builder = RetrySettings.newBuilder();

		boolean shouldBuild = ifSet(retryProperties.getInitialRetryDelaySeconds(), x -> builder.setInitialRetryDelay(Duration.ofSeconds(x)));
		shouldBuild |= ifSet(retryProperties.getInitialRpcTimeoutSeconds(), x -> builder.setInitialRpcTimeout(Duration.ofSeconds(x)));
		shouldBuild |= ifSet(retryProperties.getJittered(), builder::setJittered);
		shouldBuild |= ifSet(retryProperties.getMaxAttempts(), builder::setMaxAttempts);
		shouldBuild |= ifSet(retryProperties.getMaxRetryDelaySeconds(), x -> builder.setMaxRetryDelay(Duration.ofSeconds(x)));
		shouldBuild |= ifSet(retryProperties.getMaxRpcTimeoutSeconds(), x -> builder.setMaxRpcTimeout(Duration.ofSeconds(x)));
		shouldBuild |= ifSet(retryProperties.getRetryDelayMultiplier(), builder::setRetryDelayMultiplier);
		shouldBuild |= ifSet(retryProperties.getTotalTimeoutSeconds(), x -> builder.setTotalTimeout(Duration.ofSeconds(x)));
		shouldBuild |= ifSet(retryProperties.getRpcTimeoutMultiplier(), builder::setRpcTimeoutMultiplier);

		return shouldBuild ? builder.build() : null;
	}

	private <T> boolean ifSet(T property, Consumer<T> consumer) {
		if (property != null) {
			consumer.accept(property);
			return true;
		}
		return false;
	}

	@Bean
	@ConditionalOnMissingBean
	public PublisherFactory defaultPublisherFactory(
			@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("publisherBatchSettings") ObjectProvider<BatchingSettings> batchingSettings,
			@Qualifier("publisherRetrySettings") ObjectProvider<RetrySettings> retrySettings,
			@Qualifier("publisherTransportChannelProvider") TransportChannelProvider publisherTransportChannelProvider) {
		DefaultPublisherFactory factory = new DefaultPublisherFactory(this.finalProjectIdProvider);
		factory.setExecutorProvider(executorProvider);
		factory.setCredentialsProvider(this.finalCredentialsProvider);
		factory.setHeaderProvider(this.headerProvider);
		factory.setChannelProvider(publisherTransportChannelProvider);
		retrySettings.ifAvailable(factory::setRetrySettings);
		batchingSettings.ifAvailable(factory::setBatchingSettings);
		factory.setEnableMessageOrdering(gcpPubSubProperties.getPublisher().getEnableMessageOrdering());
		factory.setEndpoint(gcpPubSubProperties.getPublisher().getEndpoint());
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
			@Qualifier("publisherTransportChannelProvider") TransportChannelProvider publisherTransportChannelProvider) {
		try {
			return TopicAdminSettings.newBuilder()
					.setCredentialsProvider(this.finalCredentialsProvider)
					.setHeaderProvider(this.headerProvider)
					.setTransportChannelProvider(publisherTransportChannelProvider)
					.build();
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating TopicAdminSettings.", ioe);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriptionAdminClient subscriptionAdminClient(
			@Qualifier("subscriberTransportChannelProvider") TransportChannelProvider subscriberTransportChannelProvider) {
		try {
			return SubscriptionAdminClient.create(
					SubscriptionAdminSettings.newBuilder()
							.setCredentialsProvider(this.finalCredentialsProvider)
							.setHeaderProvider(this.headerProvider)
							.setTransportChannelProvider(subscriberTransportChannelProvider)
							.build());
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating SubscriptionAdminClient.", ioe);
		}
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberTransportChannelProvider")
	public TransportChannelProvider subscriberTransportChannelProvider() {
		return SubscriberStubSettings.defaultGrpcTransportProviderBuilder()
				.setKeepAliveTime(Duration.ofMinutes(this.gcpPubSubProperties.getKeepAliveIntervalMinutes()))
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherTransportChannelProvider")
	public TransportChannelProvider publisherTransportChannelProvider() {
		return PublisherStubSettings.defaultGrpcTransportProviderBuilder()
				.setKeepAliveTime(Duration.ofMinutes(this.gcpPubSubProperties.getKeepAliveIntervalMinutes()))
				.build();
	}

}
