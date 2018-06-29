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
import java.util.stream.Collectors;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.PubSubAcknowledger;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.SimplePubSubMessageConverter;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link PubSubPublishOperations}.
 *
 * <p>The main Google Cloud Pub/Sub integration component for consuming
 * messages from subscriptions asynchronously or by pulling.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Doug Hoard
 * @since 1.1
 */
public class PubSubSubscriptionTemplate implements PubSubSubscriptionOperations, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(PubSubSubscriptionTemplate.class);

	private PubSubMessageConverter messageConverter = new SimplePubSubMessageConverter();

	private final SubscriberFactory subscriberFactory;

	private final SubscriberStub subscriberStub;

	private final PubSubAcknowledger acknowledger;

	/**
	 * Default {@link PubSubSubscriptionTemplate} constructor that uses {@link SimplePubSubMessageConverter}
	 * to serialize and deserialize payloads.
	 * @param subscriberFactory the {@link Subscriber} factory
	 * to subscribe to subscriptions.
	 */
	public PubSubSubscriptionTemplate(SubscriberFactory subscriberFactory) {
		Assert.notNull(subscriberFactory, "A valid SubscriberFactory is required.");

		this.subscriberFactory = subscriberFactory;
		this.subscriberStub = this.subscriberFactory.createSubscriberStub();
		this.acknowledger = this.subscriberFactory.createAcknowledger();
	}

	public PubSubMessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public PubSubSubscriptionTemplate setMessageConverter(PubSubMessageConverter messageConverter) {
		Assert.notNull(messageConverter, "A valid Pub/Sub message converter is required.");
		this.messageConverter = messageConverter;
		return this;
	}

	@Override
	public Subscriber subscribe(String subscription, MessageReceiver messageHandler) {
		Subscriber subscriber =
				this.subscriberFactory.createSubscriber(subscription, messageHandler);
		subscriber.startAsync();
		return subscriber;
	}

	/**
	 * Pulls messages synchronously, on demand, using the pull request in argument.
	 * @param pullRequest pull request containing the subscription name
	 * @return the list of {@link AcknowledgeablePubsubMessage} containing the ack ID, subscription
	 * and acknowledger
	 */
	private List<AcknowledgeablePubsubMessage> pull(PullRequest pullRequest) {
		Assert.notNull(pullRequest, "The pull request cannot be null.");

		PullResponse pullResponse =	this.subscriberStub.pullCallable().call(pullRequest);
		List<AcknowledgeablePubsubMessage> receivedMessages =
				pullResponse.getReceivedMessagesList().stream()
						.map(message -> {
							return new AcknowledgeablePubsubMessage(message.getMessage(),
									message.getAckId(),
									pullRequest.getSubscription(),
									this.acknowledger);
						})
						.collect(Collectors.toList());

		return receivedMessages;
	}

	@Override
	public List<AcknowledgeablePubsubMessage> pull(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		return pull(this.subscriberFactory.createPullRequest(subscription, maxMessages,
				returnImmediately));
	}

	@Override
	public List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		PullRequest pullRequest = this.subscriberFactory.createPullRequest(
				subscription, maxMessages, returnImmediately);

		List<AcknowledgeablePubsubMessage> ackableMessages = pull(pullRequest);

		this.acknowledger.ack(ackableMessages.stream()
				.map(AcknowledgeablePubsubMessage::getAckId)
				.collect(Collectors.toList()), pullRequest.getSubscription());

		return ackableMessages.stream().map(AcknowledgeablePubsubMessage::getMessage)
				.collect(Collectors.toList());
	}

	@Override
	public PubsubMessage pullNext(String subscription) {
		List<PubsubMessage> receivedMessageList = pullAndAck(subscription, 1, true);

		return receivedMessageList.size() > 0 ?	receivedMessageList.get(0) : null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	public SubscriberFactory getSubscriberFactory() {
		return this.subscriberFactory;
	}

	public PubSubAcknowledger getAcknowledger() {
		return this.acknowledger;
	}
}
