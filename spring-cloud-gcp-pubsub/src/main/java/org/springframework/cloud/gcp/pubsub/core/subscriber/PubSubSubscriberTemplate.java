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
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.protobuf.Empty;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;

import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

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

	/**
	 * Default {@link PubSubSubscriberTemplate} constructor
	 *
	 * @param subscriberFactory the {@link Subscriber} factory
	 *                          to subscribe to subscriptions or pull messages.
	 */
	public PubSubSubscriberTemplate(SubscriberFactory subscriberFactory) {
		Assert.notNull(subscriberFactory, "The subscriberFactory can't be null.");

		this.subscriberFactory = subscriberFactory;
		this.subscriberStub = this.subscriberFactory.createSubscriberStub();
	}

	@Override
	public Subscriber subscribe(String subscription, MessageReceiver messageReceiver) {
		Subscriber subscriber =
				this.subscriberFactory.createSubscriber(subscription, messageReceiver);
		subscriber.startAsync();
		return subscriber;
	}

	/**
	 * Pulls messages synchronously, on demand, using the pull request in argument.
	 *
	 * @param pullRequest pull request containing the subscription name
	 * @return the list of {@link AcknowledgeablePubsubMessage} containing the ack ID, subscription
	 * and acknowledger
	 */
	private List<AcknowledgeablePubsubMessage> pull(PullRequest pullRequest) {
		Assert.notNull(pullRequest, "The pull request cannot be null.");

		PullResponse pullResponse = this.subscriberStub.pullCallable().call(pullRequest);
		List<AcknowledgeablePubsubMessage> receivedMessages =
				pullResponse.getReceivedMessagesList().stream()
						.map(message -> new PulledAcknowledgeablePubsubMessage(message.getMessage(),
								message.getAckId(),
								pullRequest.getSubscription()))
						.collect(Collectors.toList());

		return receivedMessages;
	}

	@Override
	public List<AcknowledgeablePubsubMessage> pull(
			String subscription, Integer maxMessages, Boolean returnImmediately) {
		return pull(this.subscriberFactory.createPullRequest(subscription, maxMessages,
				returnImmediately));
	}

	@Override
	public List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages, Boolean returnImmediately) {
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

		return receivedMessageList.size() > 0 ? receivedMessageList.get(0) : null;
	}

	public SubscriberFactory getSubscriberFactory() {
		return this.subscriberFactory;
	}

	@Override
	public ListenableFuture<String> ack(Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages cannot be null.");

		groupAcknowledgeableMessages(acknowledgeablePubsubMessages).forEach(this::ack);

		// TODO
		return null;
	}

	@Override
	public ListenableFuture<String> nack(Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages cannot be null.");

		groupAcknowledgeableMessages(acknowledgeablePubsubMessages).forEach(this::nack);

		// TODO
		return null;
	}

	@Override
	public ListenableFuture<String> modifyAckDeadline(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages, int ackDeadlineSeconds) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages cannot be null.");
		Assert.isTrue(ackDeadlineSeconds >= 0, "The ackDeadlineSeconds must not be negative.");

		groupAcknowledgeableMessages(acknowledgeablePubsubMessages)
				.forEach((sub, ackIds) -> modifyAckDeadline(sub, ackIds, ackDeadlineSeconds));

		// TODO
		return null;
	}

	/**
	 * Groups {@link AcknowledgeablePubsubMessage} messages by subscription.
	 *
	 * @return a map from subscription to list of ack IDs.
	 */
	private Map<String, List<String>> groupAcknowledgeableMessages(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		return acknowledgeablePubsubMessages.stream()
				.collect(Collectors.groupingBy(AcknowledgeablePubsubMessage::getSubscriptionName,
						Collectors.mapping(AcknowledgeablePubsubMessage::getAckId, Collectors.toList())));
	}

	private ListenableFuture<String> ack(String subscriptionName, Collection<String> ackIds) {
		AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		ApiFuture<Empty> apiFuture = this.subscriberStub.acknowledgeCallable().futureCall(acknowledgeRequest);

		final SettableListenableFuture<String> future = new SettableListenableFuture<>();

		ApiFutures.addCallback(apiFuture,
				new ApiFutureCallback<Empty>() {

					@Override
					public void onFailure(Throwable t) {
						future.setException(t);
					}

					@Override
					public void onSuccess(Empty empty) {

					}
				});

		return future;
	}

	private ListenableFuture<String> nack(String subscriptionName, Collection<String> ackIds) {
		return modifyAckDeadline(subscriptionName, ackIds, 0);
	}

	private ListenableFuture<String> modifyAckDeadline(
			String subscriptionName, Collection<String> ackIds, int ackDeadlineSeconds) {
		ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest.newBuilder()
				.setAckDeadlineSeconds(ackDeadlineSeconds)
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		ApiFuture<Empty> apiFuture =
				this.subscriberStub.modifyAckDeadlineCallable().futureCall(modifyAckDeadlineRequest);

		final SettableListenableFuture<String> future = new SettableListenableFuture<>();

		ApiFutures.addCallback(apiFuture,
				new ApiFutureCallback<Empty>() {

					@Override
					public void onFailure(Throwable t) {
						future.setException(t);
					}

					@Override
					public void onSuccess(Empty empty) {

					}
				});

		return future;
	}


	private class PulledAcknowledgeablePubsubMessage implements AcknowledgeablePubsubMessage {

		private final PubsubMessage message;

		private final String ackId;

		private final String subscriptionName;

		PulledAcknowledgeablePubsubMessage(PubsubMessage message, String ackId, String subscriptionName) {
			this.message = message;
			this.ackId = ackId;
			this.subscriptionName = subscriptionName;
		}

		@Override
		public PubsubMessage getPubsubMessage() {
			return this.message;
		}

		@Override
		public String getAckId() {
			return this.ackId;
		}

		@Override
		public String getSubscriptionName() {
			return this.subscriptionName;
		}

		@Override
		public ListenableFuture<String> ack() {
			return PubSubSubscriberTemplate.this.ack(Collections.singleton(this));
		}

		@Override
		public ListenableFuture<String> nack() {
			return modifyAckDeadline(0);
		}

		@Override
		public ListenableFuture<String> modifyAckDeadline(int ackDeadlineSeconds) {
			return PubSubSubscriberTemplate.this.modifyAckDeadline(
					Collections.singleton(this), ackDeadlineSeconds);
		}

		@Override
		public String toString() {
			return "PulledAcknowledgeablePubsubMessage{" +
					"message=" + this.message +
					", ackId='" + this.ackId + '\'' +
					", subscriptionName='" + this.subscriptionName + '\'' +
					'}';
		}
	}
}
