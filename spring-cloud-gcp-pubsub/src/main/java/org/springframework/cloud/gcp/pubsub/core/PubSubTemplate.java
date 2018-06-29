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

import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberTemplate;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.PubSubAcknowledger;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * Default implementation of {@link PubSubTemplate}.
 *
 * <p>The main Google Cloud Pub/Sub integration component for publishing to topics and
 * consuming messages from subscriptions asynchronously or by pulling.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class PubSubTemplate implements PubSubOperations {

	private final PubSubPublisherTemplate pubSubPublisherTemplate;

	private final PubSubSubscriberTemplate pubSubSubscriberTemplate;

	/**
	 * Constructs the instance from a {@link PubSubPublisherTemplate} and a
	 * {@link PubSubSubscriberTemplate}, to which all operations will be delegated.
	 * @param pubSubPublisherTemplate the object to which all publishing operations will be
	 * delegated
	 * @param pubSubSubscriberTemplate the object to which all subscriber operations will be
	 * delegated
	 *
	 * @since 1.1.0
	 */
	public PubSubTemplate(
			PubSubPublisherTemplate pubSubPublisherTemplate,
			PubSubSubscriberTemplate pubSubSubscriberTemplate) {
		this.pubSubPublisherTemplate = pubSubPublisherTemplate;
		this.pubSubSubscriberTemplate = pubSubSubscriberTemplate;
	}

	/**
	 * A convenience constructor that creates the {@link PubSubPublisherTemplate} using
	 * provided {@link com.google.cloud.pubsub.v1.Publisher} and the
	 * {@link PubSubSubscriberTemplate} using the provided
	 * {@link com.google.cloud.pubsub.v1.Subscriber} and then calls the
	 * {@link PubSubTemplate#PubSubTemplate(PubSubPublisherTemplate, PubSubSubscriberTemplate)}
	 * constructor.
	 * @param publisherFactory the {@link com.google.cloud.pubsub.v1.Publisher} factory to
	 * publish to topics
	 * @param subscriberFactory the {@link com.google.cloud.pubsub.v1.Subscriber} factory to
	 * subscribe to subscriptions
	 */
	public PubSubTemplate(PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory) {
		this(new PubSubPublisherTemplate(publisherFactory), new PubSubSubscriberTemplate(subscriberFactory));
	}

	@Override
	public <T> ListenableFuture<String> publish(String topic, T payload,
			Map<String, String> headers) {
		return this.pubSubPublisherTemplate.publish(topic, payload, headers);
	}

	@Override
	public <T> ListenableFuture<String> publish(String topic, T payload) {
		return this.pubSubPublisherTemplate.publish(topic, payload);
	}

	@Override
	public ListenableFuture<String> publish(String topic, PubsubMessage pubsubMessage) {
		return this.pubSubPublisherTemplate.publish(topic, pubsubMessage);
	}

	@Override
	public Subscriber subscribe(String subscription, MessageReceiver messageHandler) {
		return this.pubSubSubscriberTemplate.subscribe(subscription, messageHandler);
	}

	@Override
	public List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		return this.pubSubSubscriberTemplate.pullAndAck(subscription, maxMessages, returnImmediately);
	}

	@Override
	public List<AcknowledgeablePubsubMessage> pull(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		return this.pubSubSubscriberTemplate.pull(subscription, maxMessages, returnImmediately);
	}

	@Override
	public PubsubMessage pullNext(String subscription) {
		return this.pubSubSubscriberTemplate.pullNext(subscription);
	}

	public PubSubPublisherTemplate getPubSubPublisherTemplate() {
		return pubSubPublisherTemplate;
	}

	public PubSubSubscriberTemplate getPubSubSubscriberTemplate() {
		return pubSubSubscriberTemplate;
	}

	public PubSubMessageConverter getMessageConverter() {
		return this.pubSubPublisherTemplate.getMessageConverter();
	}

	public PubSubPublisherTemplate setMessageConverter(
			PubSubMessageConverter messageConverter) {
		return this.pubSubPublisherTemplate.setMessageConverter(messageConverter);
	}

	public PublisherFactory getPublisherFactory() {
		return this.pubSubPublisherTemplate.getPublisherFactory();
	}

	public SubscriberFactory getSubscriberFactory() {
		return this.pubSubSubscriberTemplate.getSubscriberFactory();
	}

	public PubSubAcknowledger getAcknowledger() {
		return this.pubSubSubscriberTemplate.getAcknowledger();
	}
}
