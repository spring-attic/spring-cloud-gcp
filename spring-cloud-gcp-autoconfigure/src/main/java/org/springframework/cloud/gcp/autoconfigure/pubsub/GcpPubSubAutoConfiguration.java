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
import java.util.function.Consumer;
import java.util.function.Function;

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
import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Daniel Zou
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
	public PubSubPublisherTemplate pubSubPublisherTemplate(PublisherFactory publisherFactory,
			ObjectProvider<PubSubMessageConverter> pubSubMessageConverter) {
		PubSubPublisherTemplate pubSubPublisherTemplate = new PubSubPublisherTemplate(publisherFactory);
		pubSubMessageConverter.ifUnique(pubSubPublisherTemplate::setMessageConverter);
		return pubSubPublisherTemplate;
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubSubscriberTemplate pubSubSubscriberTemplate(SubscriberFactory subscriberFactory,
			ObjectProvider<PubSubMessageConverter> pubSubMessageConverter) {
		PubSubSubscriberTemplate pubSubSubscriberTemplate = new PubSubSubscriberTemplate(subscriberFactory);
		pubSubMessageConverter.ifUnique(pubSubSubscriberTemplate::setMessageConverter);
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

		return ifNotNull(flowControl.getLimitExceededBehavior(), builder::setLimitExceededBehavior)
				.apply(ifNotNull(flowControl.getMaxOutstandingElementCount(),
						builder::setMaxOutstandingElementCount)
				.apply(ifNotNull(flowControl.getMaxOutstandingRequestBytes(),
						builder::setMaxOutstandingRequestBytes)
				.apply(false))) ? builder.build() : null;
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

		return ifNotNull(batching.getDelayThresholdSeconds(),
					x -> builder.setDelayThreshold(Duration.ofSeconds(x)))
				.apply(ifNotNull(batching.getElementCountThreshold(), builder::setElementCountThreshold)
				.apply(ifNotNull(batching.getEnabled(), builder::setIsEnabled)
				.apply(ifNotNull(batching.getRequestByteThreshold(), builder::setRequestByteThreshold)
				.apply(false)))) ? builder.build() : null;
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherRetrySettings")
	public RetrySettings publisherRetrySettings() {
		return buildRetrySettings(this.gcpPubSubProperties.getPublisher().getRetry());
	}

	private RetrySettings buildRetrySettings(GcpPubSubProperties.Retry retryProperties) {
		Builder builder = RetrySettings.newBuilder();

		return ifNotNull(retryProperties.getInitialRetryDelaySeconds(),
				x -> builder.setInitialRetryDelay(Duration.ofSeconds(x)))
				.apply(ifNotNull(retryProperties.getInitialRpcTimeoutSeconds(),
						x -> builder.setInitialRpcTimeout(Duration.ofSeconds(x)))
				.apply(ifNotNull(retryProperties.getJittered(), builder::setJittered)
				.apply(ifNotNull(retryProperties.getMaxAttempts(), builder::setMaxAttempts)
				.apply(ifNotNull(retryProperties.getMaxRetryDelaySeconds(),
						x -> builder.setMaxRetryDelay(Duration.ofSeconds(x)))
				.apply(ifNotNull(retryProperties.getMaxRpcTimeoutSeconds(),
						x -> builder.setMaxRpcTimeout(Duration.ofSeconds(x)))
				.apply(ifNotNull(retryProperties.getRetryDelayMultiplier(), builder::setRetryDelayMultiplier)
				.apply(ifNotNull(retryProperties.getTotalTimeoutSeconds(),
						x -> builder.setTotalTimeout(Duration.ofSeconds(x)))
				.apply(ifNotNull(retryProperties.getRpcTimeoutMultiplier(), builder::setRpcTimeoutMultiplier)
				.apply(false))))))))) ? builder.build() : null;
	}

	/**
	 * A helper method for applying properties to settings builders for purpose of seeing if at least
	 * one setting was set.
	 */
	private <T> Function<Boolean, Boolean> ifNotNull(T prop, Consumer<T> consumer) {
		return next -> {
			boolean wasSet = next;
			if (prop != null) {
				consumer.accept(prop);
				wasSet = true;
			}
			return wasSet;
		};
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
