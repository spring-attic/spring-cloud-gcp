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

package org.springframework.cloud.gcp.pubsub.autoconfig;

import java.io.IOException;
import java.util.concurrent.Executors;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;
import com.google.api.gax.grpc.ChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubException;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;

/**
 * @author João André Martins
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
public class GcpPubSubAutoConfiguration {

	public static final String DEFAULT_SOURCE_NAME = "spring";

	@Value("${spring.cloud.gcp.pubsub.subscriber.executorThreads:4}")
	private int subscriberExecutorThreads;

	@Value("${spring.cloud.gcp.pubsub.publisher.executorThreads:4}")
	private int publisherExecutorThreads;

	@Bean
	@ConditionalOnMissingBean(name = "publisherExecutorProvider")
	public ExecutorProvider publisherExecutorProvider() {
		return FixedExecutorProvider.create(
				Executors.newScheduledThreadPool(this.publisherExecutorThreads));
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberExecutorProvider")
	public ExecutorProvider subscriberExecutorProvider() {
		return FixedExecutorProvider.create(
				Executors.newScheduledThreadPool(this.subscriberExecutorThreads));
	}

	@Bean
	@ConditionalOnMissingBean(name = "subscriberChannelProvider")
	public ChannelProvider subscriberChannelProvider() {
		return SubscriptionAdminSettings.defaultGrpcChannelProviderBuilder()
				.setClientLibHeader(DEFAULT_SOURCE_NAME,
						this.getClass().getPackage().getImplementationVersion())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherChannelProvider")
	public ChannelProvider publisherChannelProvider() {
		return TopicAdminSettings
				.defaultGrpcChannelProviderBuilder()
				.setClientLibHeader(DEFAULT_SOURCE_NAME,
						this.getClass().getPackage().getImplementationVersion())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "pubSubMessageConverter")
	public MessageConverter pubsubMessageConverter() {
		return new StringMessageConverter();
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubTemplate pubSubTemplate(GcpProjectIdProvider projectIdProvider, PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory, SubscriptionAdminClient subscriptionAdminClient,
			@Qualifier("pubSubMessageConverter") MessageConverter pubsubMessageConverter) {
		return new PubSubTemplate(projectIdProvider, publisherFactory, subscriberFactory,
				subscriptionAdminClient.getStub(), pubsubMessageConverter);
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriberFactory defaultSubscriberFactory(GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider,
			@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("publisherChannelProvider") ChannelProvider channelProvider) {
		return new DefaultSubscriberFactory(projectIdProvider, executorProvider, channelProvider,
				credentialsProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public PublisherFactory defaultPublisherFactory(GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider,
			@Qualifier("subscriberExecutorProvider") ExecutorProvider subscriberProvider,
			@Qualifier("subscriberChannelProvider") ChannelProvider channelProvider) {
		return new DefaultPublisherFactory(projectIdProvider, subscriberProvider, channelProvider,
				credentialsProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public PubSubAdmin pubSubAdmin(GcpProjectIdProvider projectIdProvider,
			TopicAdminClient topicAdminClient,
			SubscriptionAdminClient subscriptionAdminClient) {
		return new PubSubAdmin(projectIdProvider, topicAdminClient, subscriptionAdminClient);
	}

	@Bean
	@ConditionalOnMissingBean
	public TopicAdminClient topicAdminClient(CredentialsProvider credentialsProvider) {
		try {
			return TopicAdminClient.create(
					TopicAdminSettings.defaultBuilder()
							.setCredentialsProvider(credentialsProvider)
							.build());
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating TopicAdminClient.", ioe);
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriptionAdminClient subscriptionAdminClient(
			CredentialsProvider credentialsProvider) {
		try {
			return SubscriptionAdminClient.create(
					SubscriptionAdminSettings.defaultBuilder()
							.setCredentialsProvider(credentialsProvider)
							.build());
		}
		catch (IOException ioe) {
			throw new PubSubException("An error occurred while creating SubscriptionAdminClient.",
					ioe);
		}
	}
}
