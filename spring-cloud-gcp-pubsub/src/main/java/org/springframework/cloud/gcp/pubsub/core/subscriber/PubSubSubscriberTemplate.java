/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.pubsub.core.subscriber;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;

import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.SimplePubSubMessageConverter;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link PubSubSubscriberOperations}.
 *
 * <p>The main Google Cloud Pub/Sub integration component for consuming
 * messages from subscriptions asynchronously or by pulling.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Doug Hoard
 *
 * @since 1.1
 */
public class PubSubSubscriberTemplate implements PubSubSubscriberOperations {

	private final SubscriberFactory subscriberFactory;

	private final SubscriberStub subscriberStub;

	private PubSubMessageConverter pubSubMessageConverter = new SimplePubSubMessageConverter();

	/**
	 * Default {@link PubSubSubscriberTemplate} constructor
	 * @param subscriberFactory the {@link Subscriber} factory
	 * to subscribe to subscriptions or pull messages.
	 */
	public PubSubSubscriberTemplate(SubscriberFactory subscriberFactory) {
		Assert.notNull(subscriberFactory, "The subscriberFactory can't be null.");

		this.subscriberFactory = subscriberFactory;
		this.subscriberStub = this.subscriberFactory.createSubscriberStub();
	}

	public PubSubMessageConverter getMessageConverter() {
		return this.pubSubMessageConverter;
	}

	public void setMessageConverter(PubSubMessageConverter pubSubMessageConverter) {
		Assert.notNull(pubSubMessageConverter, "The pubSubMessageConverter can't be null.");

		this.pubSubMessageConverter = pubSubMessageConverter;
	}

	@Override
	@Deprecated
	public Subscriber subscribe(String subscription, MessageReceiver messageReceiver) {
		Assert.hasText(subscription, "The subscription can't be null or empty.");
		Assert.notNull(messageReceiver, "The messageReceiver can't be null.");

		Subscriber subscriber =
				this.subscriberFactory.createSubscriber(subscription, messageReceiver);
		subscriber.startAsync();
		return subscriber;
	}

	@Override
	public Subscriber subscribe(String subscription,
			Consumer<BasicAcknowledgeablePubsubMessage> messageConsumer) {
		Assert.notNull(messageConsumer, "The messageConsumer can't be null.");

		Subscriber subscriber =
				this.subscriberFactory.createSubscriber(subscription,
						(message, ackReplyConsumer) -> messageConsumer.accept(
								new PushedAcknowledgeablePubsubMessage(message, ackReplyConsumer, subscription)));
		subscriber.startAsync();
		return subscriber;
	}

	@Override
	public <T> Subscriber subscribeAndConvert(String subscription,
			Consumer<ConvertedBasicAcknowledgeablePubsubMessage<T>> messageConsumer, Class<T> payloadType) {
		Assert.notNull(messageConsumer, "The messageConsumer can't be null.");

		return this.subscribe(subscription,
				(message) -> messageConsumer.accept(new ConvertedPushedAcknowledgeablePubsubMessage<T>(message,
						this.pubSubMessageConverter.fromPubSubMessage(message.getPubsubMessage(), payloadType))));
	}

	/**
	 * Pulls messages synchronously, on demand, using the pull request in argument.
	 * @param pullRequest pull request containing the subscription name
	 * @return the list of {@link AcknowledgeablePubsubMessage} containing the ack ID, subscription
	 * and acknowledger
	 */
	private List<AcknowledgeablePubsubMessage> pull(PullRequest pullRequest) {
		Assert.notNull(pullRequest, "The pull request can't be null.");

		PullResponse pullResponse =	this.subscriberStub.pullCallable().call(pullRequest);
		List<AcknowledgeablePubsubMessage> receivedMessages =
				pullResponse.getReceivedMessagesList().stream()
						.map(message -> new PulledAcknowledgeablePubsubMessage(message.getMessage(),
									message.getAckId(),
									pullRequest.getSubscription()))
						.collect(Collectors.toList());

		return receivedMessages;
	}

	@Override
	public List<AcknowledgeablePubsubMessage> pull(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		Assert.hasText(subscription, "The subscription can't be null or empty.");

		if (maxMessages != null) {
			Assert.isTrue(maxMessages > 0, "The maxMessages must be greater than 0.");
		}

		return pull(this.subscriberFactory.createPullRequest(subscription, maxMessages,
				returnImmediately));
	}

	@Override
	public <T> List<ConvertedAcknowledgeablePubsubMessage<T>> pullAndConvert(String subscription, Integer maxMessages,
			Boolean returnImmediately, Class<T> payloadType) {
		List<AcknowledgeablePubsubMessage> ackableMessages = this.pull(subscription, maxMessages, returnImmediately);

		return ackableMessages.stream().map(
				m -> new ConvertedPulledAcknowledgeablePubsubMessage<>(m,
						this.pubSubMessageConverter.fromPubSubMessage(m.getPubsubMessage(), payloadType))
		).collect(Collectors.toList());
	}

	@Override
	public List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages,
			Boolean returnImmediately) {
		Assert.hasText(subscription, "The subscription can't be null or empty.");

		if (maxMessages != null) {
			Assert.isTrue(maxMessages > 0, "The maxMessages must be greater than 0.");
		}

		PullRequest pullRequest = this.subscriberFactory.createPullRequest(
				subscription, maxMessages, returnImmediately);

		List<AcknowledgeablePubsubMessage> ackableMessages = pull(pullRequest);

		ack(ackableMessages);

		return ackableMessages.stream().map(AcknowledgeablePubsubMessage::getPubsubMessage)
				.collect(Collectors.toList());
	}

	@Override
	public PubsubMessage pullNext(String subscription) {
		List<PubsubMessage> receivedMessageList = pullAndAck(subscription, 1, true);

		return receivedMessageList.size() > 0 ?	receivedMessageList.get(0) : null;
	}

	public SubscriberFactory getSubscriberFactory() {
		return this.subscriberFactory;
	}

	@Override
	public void ack(Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages can't be null.");

		groupAcknowledgeableMessages(acknowledgeablePubsubMessages).forEach(this::ack);
	}

	@Override
	public void nack(Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages can't be null.");

		groupAcknowledgeableMessages(acknowledgeablePubsubMessages).forEach(this::nack);
	}

	@Override
	public void modifyAckDeadline(Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages,
			int ackDeadlineSeconds) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages can't be null.");
		Assert.isTrue(ackDeadlineSeconds >= 0, "The ackDeadlineSeconds can't be less than 0.");

		groupAcknowledgeableMessages(acknowledgeablePubsubMessages)
				.forEach((sub, ackIds) -> modifyAckDeadline(sub, ackIds, ackDeadlineSeconds));
	}

	/**
	 * Groups {@link AcknowledgeablePubsubMessage} messages by subscription.
	 * @return a map from subscription to list of ack IDs.
	 */
	private Map<String, List<String>> groupAcknowledgeableMessages(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		return acknowledgeablePubsubMessages.stream()
				.collect(Collectors.groupingBy(AcknowledgeablePubsubMessage::getSubscriptionName,
						Collectors.mapping(AcknowledgeablePubsubMessage::getAckId, Collectors.toList())));
	}

	private void ack(String subscriptionName, Collection<String> ackIds) {
		AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		this.subscriberStub.acknowledgeCallable().call(acknowledgeRequest);
	}

	private void nack(String subscriptionName, Collection<String> ackIds) {
		modifyAckDeadline(subscriptionName, ackIds, 0);
	}

	private void modifyAckDeadline(String subscriptionName, Collection<String> ackIds, int ackDeadlineSeconds) {
		ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest.newBuilder()
				.setAckDeadlineSeconds(ackDeadlineSeconds)
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		this.subscriberStub.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
	}


	private static abstract class AbstractBasicAcknowledgeablePubsubMessage
			implements BasicAcknowledgeablePubsubMessage {

		private final PubsubMessage message;

		private final String subscriptionName;

		AbstractBasicAcknowledgeablePubsubMessage(PubsubMessage message, String subscriptionName) {
			this.message = message;
			this.subscriptionName = subscriptionName;
		}

		@Override
		public PubsubMessage getPubsubMessage() {
			return this.message;
		}

		@Override
		public String getSubscriptionName() {
			return this.subscriptionName;
		}
	}

	private class PulledAcknowledgeablePubsubMessage extends AbstractBasicAcknowledgeablePubsubMessage
			implements AcknowledgeablePubsubMessage {

		private final String ackId;

		PulledAcknowledgeablePubsubMessage(PubsubMessage message, String ackId,
				String subscriptionName) {
			super(message, subscriptionName);
			this.ackId = ackId;
		}

		@Override
		public String getAckId() {
			return this.ackId;
		}

		@Override
		public void ack() {
			PubSubSubscriberTemplate.this.ack(Collections.singleton(this));
		}

		@Override
		public void nack() {
			modifyAckDeadline(0);
		}

		@Override
		public void modifyAckDeadline(int ackDeadlineSeconds) {
			Assert.isTrue(ackDeadlineSeconds >= 0, "The ackDeadlineSeconds can't be less than 0.");

			PubSubSubscriberTemplate.this.modifyAckDeadline(Collections.singleton(this), ackDeadlineSeconds);
		}

		@Override
		public String toString() {
			return "PulledAcknowledgeablePubsubMessage{" +
					"message=" + getPubsubMessage() +
					", subscriptionName='" + getSubscriptionName() + '\'' +
					", ackId='" + this.ackId + '\'' +
					'}';
		}
	}

	private static class PushedAcknowledgeablePubsubMessage extends AbstractBasicAcknowledgeablePubsubMessage {

		private final AckReplyConsumer ackReplyConsumer;

		PushedAcknowledgeablePubsubMessage(PubsubMessage message, AckReplyConsumer ackReplyConsumer,
				String subscriptionName) {
			super(message, subscriptionName);
			this.ackReplyConsumer = ackReplyConsumer;
		}

		@Override
		public void ack() {
			this.ackReplyConsumer.ack();
		}

		@Override
		public void nack() {
			this.ackReplyConsumer.nack();
		}

		@Override
		public String toString() {
			return "PushedAcknowledgeablePubsubMessage{" +
					"message=" + getPubsubMessage() +
					", subscriptionName='" + getSubscriptionName() + '\'' +
					'}';
		}
	}

	private class ConvertedPulledAcknowledgeablePubsubMessage<T> extends PulledAcknowledgeablePubsubMessage
			implements ConvertedAcknowledgeablePubsubMessage<T> {

		private final T payload;

		ConvertedPulledAcknowledgeablePubsubMessage(AcknowledgeablePubsubMessage message, T payload) {
			super(message.getPubsubMessage(), message.getAckId(), message.getSubscriptionName());
			this.payload = payload;
		}

		@Override
		public T getPayload() {
			return this.payload;
		}
	}

	private static class ConvertedPushedAcknowledgeablePubsubMessage<T> extends PushedAcknowledgeablePubsubMessage
			implements ConvertedBasicAcknowledgeablePubsubMessage<T> {

		private final T payload;

		ConvertedPushedAcknowledgeablePubsubMessage(BasicAcknowledgeablePubsubMessage message, T payload) {
			super(message.getPubsubMessage(), message, message.getSubscriptionName());
			this.payload = payload;
		}

		@Override
		public T getPayload() {
			return this.payload;
		}
	}

}
