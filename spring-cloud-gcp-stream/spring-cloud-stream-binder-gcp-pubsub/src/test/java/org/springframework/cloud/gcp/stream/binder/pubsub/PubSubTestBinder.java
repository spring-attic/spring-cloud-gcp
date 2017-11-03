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

package org.springframework.cloud.gcp.stream.binder.pubsub;

import java.util.concurrent.Executors;

import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.core.FixedExecutorProvider;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.DefaultPublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.DefaultSubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubProducerProperties;
import org.springframework.cloud.gcp.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;
import org.springframework.cloud.stream.binder.AbstractTestBinder;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedProducerProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.integration.codec.kryo.PojoCodec;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Andreas Berger
 */
public class PubSubTestBinder extends AbstractTestBinder<PubSubMessageChannelBinder,
		ExtendedConsumerProperties<PubSubConsumerProperties>,
		ExtendedProducerProperties<PubSubProducerProperties>> {

	private final PubSubAdmin pubSubAdmin;

	public PubSubTestBinder(String project, boolean supportsHeadersNatively,
			PubSubSupport pubSubSupport) {
		GcpProjectIdProvider projectIdProvider = () -> project;

		this.pubSubAdmin = new PubSubAdmin(projectIdProvider,
				pubSubSupport.getTopicAdminClient(),
				pubSubSupport.getSubscriptionAdminClient());

		ExecutorProvider executor =
				FixedExecutorProvider.create(Executors.newScheduledThreadPool(5));

		PublisherFactory publisherFactory = new DefaultPublisherFactory(
				projectIdProvider,
				executor,
				pubSubSupport.getChannelProvider(),
				pubSubSupport.getCredentialsProvider());

		SubscriberFactory subscriberFactory = new DefaultSubscriberFactory(
				projectIdProvider,
				executor,
				pubSubSupport.getChannelProvider(),
				pubSubSupport.getCredentialsProvider());

		PubSubMessageChannelBinder binder = new PubSubMessageChannelBinder(
				supportsHeadersNatively,
				new String[0],
				new PubSubChannelProvisioner(this.pubSubAdmin),
				new PubSubTemplate(publisherFactory, subscriberFactory));

		GenericApplicationContext context = new GenericApplicationContext();
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(1);
		scheduler.afterPropertiesSet();
		context.getBeanFactory().registerSingleton(
				IntegrationContextUtils.TASK_SCHEDULER_BEAN_NAME, scheduler);
		context.refresh();
		binder.setApplicationContext(context);
		binder.setCodec(new PojoCodec());
		this.setBinder(binder);

	}

	@Override
	public void cleanup() {
		this.pubSubAdmin.listSubscriptions().forEach(subscription -> {
			System.out.println("Deleting subscription: " + subscription.getName());
			this.pubSubAdmin.deleteSubscription(
					subscription.getNameAsSubscriptionName().getSubscription());
		});
		this.pubSubAdmin.listTopics().forEach(topic -> {
			System.out.println("Deleting topic: " + topic.getName());
			this.pubSubAdmin.deleteTopic(topic.getNameAsTopicName().getTopic());
		});
	}
}
