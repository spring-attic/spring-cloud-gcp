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
import com.google.cloud.pubsub.spi.v1.TopicAdminSettings;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author João André Martins
 */
@Configuration
@ComponentScan(basePackageClasses =
		org.springframework.cloud.gcp.core.autoconfig.GcpContextAutoConfiguration.class)
public class GcpPubsubAutoConfiguration {

	public static final String DEFAULT_SOURCE_NAME = "spring";
	public static final int DEFAULT_EXECUTOR_THREADS = 4;

	@Bean
	@ConditionalOnMissingBean
	public ExecutorProvider executorProviderForPublisher() {
		return FixedExecutorProvider.create(
				Executors.newScheduledThreadPool(DEFAULT_EXECUTOR_THREADS));
	}

	@Bean
	@ConditionalOnMissingBean
	public ExecutorProvider executorProviderForSubscriber() {
		return FixedExecutorProvider.create(
				Executors.newScheduledThreadPool(DEFAULT_EXECUTOR_THREADS));
	}

	@Bean
	@ConditionalOnClass({GcpProperties.class, GoogleCredentials.class})
	@ConditionalOnMissingBean
	public PubSubTemplate pubsubTemplate(PublisherFactory publisherFactory) {
		return new PubSubTemplate(publisherFactory);
	}

	@Bean
	@ConditionalOnClass({GcpProperties.class, GoogleCredentials.class})
	@ConditionalOnMissingBean
	public SubscriberFactory defaultSubscriberFactory(GcpProperties gcpProperties,
			GoogleCredentials credentials,
			@Qualifier("executorProviderForSubscriber") ExecutorProvider executorProvider) {
		return new DefaultSubscriberFactory(
				gcpProperties.getProjectId(), credentials, executorProvider);
	}

	@Bean
	@ConditionalOnClass({GcpProperties.class, GoogleCredentials.class})
	@ConditionalOnMissingBean
	public PublisherFactory defaultPublisherFactory(GcpProperties gcpProperties,
			GoogleCredentials credentials,
			@Qualifier("executorProviderForPublisher") ExecutorProvider executorProvider) {
		return new DefaultPublisherFactory(gcpProperties.getProjectId(),
				executorProvider,
				TopicAdminSettings
						.defaultChannelProviderBuilder()
						.setCredentialsProvider(() -> credentials)
						.setClientLibHeader(DEFAULT_SOURCE_NAME,
								this.getClass().getPackage().getImplementationVersion())
						.build());
	}
}
