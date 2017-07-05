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

import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.FixedExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
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
	@ConditionalOnMissingBean
	public PubSubTemplate pubsubTemplate(PublisherFactory publisherFactory) {
		return new PubSubTemplate(publisherFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public SubscriberFactory defaultSubscriberFactory(GcpProperties gcpProperties,
			GoogleCredentials credentials,
			@Qualifier("publisherExecutorProvider") ExecutorProvider executorProvider) {
		return new DefaultSubscriberFactory(gcpProperties.getProjectId(),
				executorProvider, () -> credentials);
	}

	@Bean
	@ConditionalOnMissingBean
	public PublisherFactory defaultPublisherFactory(GcpProperties gcpProperties,
			GoogleCredentials credentials,
			@Qualifier("subscriberExecutorProvider") ExecutorProvider subscriberProvider) {
		return new DefaultPublisherFactory(gcpProperties.getProjectId(),
				subscriberProvider, () -> credentials);
	}
}
