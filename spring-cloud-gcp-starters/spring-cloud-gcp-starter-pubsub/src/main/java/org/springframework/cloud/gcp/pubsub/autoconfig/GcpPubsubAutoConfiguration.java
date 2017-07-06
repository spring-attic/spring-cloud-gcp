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

import java.util.concurrent.Executors;

import com.google.api.gax.grpc.ChannelProvider;
import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.FixedExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminSettings;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.core.PubsubTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author João André Martins
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
public class GcpPubsubAutoConfiguration {

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
	public ChannelProvider subscriberChannelProvider(GoogleCredentials credentials) {
		return SubscriptionAdminSettings.defaultChannelProviderBuilder()
				.setCredentialsProvider(() -> credentials)
				.setClientLibHeader(DEFAULT_SOURCE_NAME,
						this.getClass().getPackage().getImplementationVersion())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean(name = "publisherChannelProvider")
	public ChannelProvider publisherChannelProvider(GoogleCredentials credentials) {
		return TopicAdminSettings
				.defaultChannelProviderBuilder()
				.setCredentialsProvider(() -> credentials)
				.setClientLibHeader(DEFAULT_SOURCE_NAME,
						this.getClass().getPackage().getImplementationVersion())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public PubsubTemplate pubsubTemplate(PublisherFactory publisherFactory) {
		return new PubsubTemplate(publisherFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriberFactory defaultSubscriberFactory(GcpProperties gcpProperties,
			@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider,
			@Qualifier("publisherChannelProvider") ChannelProvider channelProvider) {
		return new DefaultSubscriberFactory(gcpProperties.getProjectId(),
				executorProvider, channelProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public PublisherFactory defaultPublisherFactory(GcpProperties gcpProperties,
			@Qualifier("subscriberExecutorProvider") ExecutorProvider subscriberProvider,
			@Qualifier("subscriberChannelProvider") ChannelProvider channelProvider) {
		return new DefaultPublisherFactory(gcpProperties.getProjectId(),
				subscriberProvider, channelProvider);
	}
}
