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

package org.springframework.cloud.gcp.pubsub.core;

import java.util.List;
import java.util.Map;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.PubSubAcknowledger;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.SimplePubSubMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Default implementation of {@link PubSubPublishOperations} and {@link PubSubSubscriptionOperations}.
 *
 * <p>The main Google Cloud Pub/Sub integration component for publishing to topics and consuming
 * messages from subscriptions asynchronously or by pulling.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Doug Hoard
 * @since 1.0
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(PubSubTemplate.class);

	private PubSubMessageConverter messageConverter = new SimplePubSubMessageConverter();

	private final PubSubPublishTemplate pubSubPublishTemplate;

	private final PubSubSubscriptionTemplate pubSubSubscriptionTemplate;

	/**
	 * Default {@link PubSubTemplate} constructor that uses {@link SimplePubSubMessageConverter}
	 * to serialize and deserialize payloads.
	 * @param publisherFactory the {@link com.google.cloud.pubsub.v1.Publisher} factory to
	 * publish to topics.
	 * @param subscriberFactory the {@link com.google.cloud.pubsub.v1.Subscriber} factory
	 * to subscribe to subscriptions.
	 */
	public PubSubTemplate(PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory) {
		Assert.notNull(publisherFactory, "A valid PublisherFactory is required.");
		Assert.notNull(subscriberFactory, "A valid SubscriberFactory is required.");

		this.pubSubPublishTemplate = new PubSubPublishTemplate(publisherFactory);
		this.pubSubSubscriptionTemplate = new PubSubSubscriptionTemplate(subscriberFactory);
	}

	public PubSubPublishTemplate getPubSubPublishTemplate() {
		return this.pubSubPublishTemplate;
	}

	public PubSubSubscriptionTemplate getPubSubSubscriptionTemplate() {
		return this.pubSubSubscriptionTemplate;
	}

	public PubSubMessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public PubSubTemplate setMessageConverter(PubSubMessageConverter messageConverter) {
		Assert.notNull(messageConverter, "A valid Pub/Sub message converter is required.");

		this.messageConverter = messageConverter;
		this.pubSubPublishTemplate.setMessageConverter(messageConverter);
		this.pubSubSubscriptionTemplate.setMessageConverter(messageConverter);

		return this;
	}

	/**
	 * Uses the configured message converter to first convert the payload and headers to a
	 * {@code PubsubMessage} and then publish it.
	 */
	@Override
	public <T> ListenableFuture<String> publish(String topic, T payload,
			Map<String, String> headers) {
		return this.pubSubPublishTemplate.publish(topic, payload, headers);
	}

	@Override
	public <T> ListenableFuture<String> publish(String topic, T payload) {
		return this.pubSubPublishTemplate.publish(topic, payload, null);
	}

	@Override
	public ListenableFuture<String> publish(final String topic, PubsubMessage pubsubMessage) {
		return this.pubSubPublishTemplate.publish(topic, pubsubMessage);
	}

	@Override
	public Subscriber subscribe(String subscription, MessageReceiver messageHandler) {
		return this.pubSubSubscriptionTemplate.subscribe(subscription, messageHandler);
	}

	@Override
	public List<AcknowledgeablePubsubMessage> pull(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		return this.pubSubSubscriptionTemplate.pull(subscription, maxMessages, returnImmediately);
	}

	@Override
	public List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		return this.pubSubSubscriptionTemplate.pullAndAck(subscription, maxMessages, returnImmediately);
	}

	@Override
	public PubsubMessage pullNext(String subscription) {
		return this.pubSubSubscriptionTemplate.pullNext(subscription);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	public PublisherFactory getPublisherFactory() {
		return this.pubSubPublishTemplate.getPublisherFactory();
	}

	public SubscriberFactory getSubscriberFactory() {
		return this.pubSubSubscriptionTemplate.getSubscriberFactory();
	}

	public PubSubAcknowledger getAcknowledger() {
		return this.pubSubSubscriptionTemplate.getAcknowledger();
	}
}
