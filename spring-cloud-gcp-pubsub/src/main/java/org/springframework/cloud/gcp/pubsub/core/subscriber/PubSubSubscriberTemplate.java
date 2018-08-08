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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.Empty;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;

import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
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
								new PushedAcknowledgeablePubsubMessage(
										this.subscriberFactory.getProjectId(),
										message,
										ackReplyConsumer,
										subscription)));
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
		Assert.notNull(pullRequest, "The pull request can't be null.");

		PullResponse pullResponse = this.subscriberStub.pullCallable().call(pullRequest);
		List<AcknowledgeablePubsubMessage> receivedMessages =
				pullResponse.getReceivedMessagesList().stream()
						.map(message -> new PulledAcknowledgeablePubsubMessage(
								this.subscriberFactory.getProjectId(),
								message.getMessage(),
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
	public ListenableFuture<Empty> ack(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages cannot be null.");

		Map<String, List<String>> acknowledgeablePubsubMessagesMap =
				groupAcknowledgeablePubsubMessages(acknowledgeablePubsubMessages);

		String subscriptionName = acknowledgeablePubsubMessagesMap.keySet().iterator().next();
		List<String> ackIds = acknowledgeablePubsubMessagesMap.get(subscriptionName);

		SettableListenableFuture<Empty> settableListenableFuture = new SettableListenableFuture<>();

		ApiFuture<Empty> apiFuture = ack(subscriptionName, ackIds);
		ApiFutures.addCallback(apiFuture, new ApiFutureCallback<Empty>() {
			@Override
			public void onFailure(Throwable throwable) {
				settableListenableFuture.setException(throwable);
			}

			@Override
			public void onSuccess(Empty empty) {
				settableListenableFuture.set(empty);
			}
		});

		return settableListenableFuture;
	}

	@Override
	public ListenableFuture<Empty> nack(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		return modifyAckDeadline(acknowledgeablePubsubMessages, 0);
	}

	@Override
	public ListenableFuture<Empty> modifyAckDeadline(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages, int ackDeadlineSeconds) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages cannot be null.");
		Assert.isTrue(ackDeadlineSeconds >= 0, "The ackDeadlineSeconds must not be negative.");

		Map<String, List<String>> acknowledgeablePubsubMessagesMap =
				groupAcknowledgeablePubsubMessages(acknowledgeablePubsubMessages);

		String subscriptionName = acknowledgeablePubsubMessagesMap.keySet().iterator().next();
		List<String> ackIds = acknowledgeablePubsubMessagesMap.get(subscriptionName);

		SettableListenableFuture<Empty> settableListenableFuture = new SettableListenableFuture<>();

		ApiFuture<Empty> apiFuture = modifyAckDeadline(subscriptionName, ackIds, ackDeadlineSeconds);
		ApiFutures.addCallback(apiFuture, new ApiFutureCallback<Empty>() {
			@Override
			public void onFailure(Throwable throwable) {
				settableListenableFuture.setException(throwable);
			}

			@Override
			public void onSuccess(Empty empty) {
				settableListenableFuture.set(empty);
			}
		});

		return settableListenableFuture;
	}

	/**
	 * Groups the Collection of {@link AcknowledgeablePubsubMessage} ack ids based
	 * on subscription name, validating that all messages have matching a project
	 * id that matches this template and all subscriptions names are matching.
	 * @return a map from subscription to list of ack IDs.
	 */
	private Map<String, List<String>> groupAcknowledgeablePubsubMessages(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		List<String> ackIds = new LinkedList<>();

		String subscriptionName = null;

		for (AcknowledgeablePubsubMessage acknowledgeablePubsubMessage : acknowledgeablePubsubMessages) {
			if (subscriptionName == null) {
				subscriptionName = acknowledgeablePubsubMessage.getSubscriptionName();
			}

			Assert.isTrue(
						subscriptionName.equals(acknowledgeablePubsubMessage.getSubscriptionName()),
						"The acknowledgeablePubsubMessages can't contain different subscriptions");

			Assert.isTrue(
					this.subscriberFactory.getProjectId().equals(acknowledgeablePubsubMessage.getProjectId()),
					"The acknowledgeablePubsubMessages doesn't match associated projectId.");

			ackIds.add(acknowledgeablePubsubMessage.getAckId());
		}

		Map<String, List<String>> acknowledgeablePubsubMessageMap = new HashMap<>();
		acknowledgeablePubsubMessageMap.put(subscriptionName, ackIds);

		return acknowledgeablePubsubMessageMap;
	}

	private ApiFuture<Empty> ack(String subscriptionName, Collection<String> ackIds) {
		AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		return this.subscriberStub.acknowledgeCallable().futureCall(acknowledgeRequest);
	}

	private ApiFuture<Empty> nack(String subscriptionName, Collection<String> ackIds) {
		return modifyAckDeadline(subscriptionName, ackIds, 0);
	}

	private ApiFuture<Empty> modifyAckDeadline(
			String subscriptionName, Collection<String> ackIds, int ackDeadlineSeconds) {
		ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest.newBuilder()
				.setAckDeadlineSeconds(ackDeadlineSeconds)
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		return this.subscriberStub.modifyAckDeadlineCallable().futureCall(modifyAckDeadlineRequest);
	}


	private static abstract class AbstractBasicAcknowledgeablePubsubMessage
			implements BasicAcknowledgeablePubsubMessage {

		private final String projectId;

		private final PubsubMessage message;

		private final String subscriptionName;

		AbstractBasicAcknowledgeablePubsubMessage(String projectId, PubsubMessage message, String subscriptionName) {
			this.projectId = projectId;
			this.message = message;
			this.subscriptionName = subscriptionName;
		}

		@Override
		public String getProjectId() {
			return this.projectId;
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

		PulledAcknowledgeablePubsubMessage(String projectId, PubsubMessage message, String ackId,
				String subscriptionName) {
			super(projectId, message, subscriptionName);
			this.ackId = ackId;
		}

		@Override
		public String getAckId() {
			return this.ackId;
		}

		@Override
		public ListenableFuture<Empty> ack() {
			return PubSubSubscriberTemplate.this.ack(Collections.singleton(this));
		}

		@Override
		public ListenableFuture<Empty> nack() {
			return modifyAckDeadline(0);
		}

		@Override
		public ListenableFuture<Empty> modifyAckDeadline(int ackDeadlineSeconds) {
			return PubSubSubscriberTemplate.this.modifyAckDeadline(Collections.singleton(this), ackDeadlineSeconds);
		}

		@Override
		public String toString() {
			return "PulledAcknowledgeablePubsubMessage{" +
					"message=" + getPubsubMessage() +
					", subscriptionName='" + getSubscriptionName() + '\'' +
					", projectId='" + getProjectId() + '\'' +
					", ackId='" + this.ackId + '\'' +
					'}';
		}
	}

	private static class PushedAcknowledgeablePubsubMessage extends AbstractBasicAcknowledgeablePubsubMessage {

		private final AckReplyConsumer ackReplyConsumer;

		PushedAcknowledgeablePubsubMessage(String projectId, PubsubMessage message, AckReplyConsumer ackReplyConsumer,
				String subscriptionName) {
			super(projectId, message, subscriptionName);
			this.ackReplyConsumer = ackReplyConsumer;
		}

		@Override
		public ListenableFuture<Empty> ack() {
			Executor executor = MoreExecutors.directExecutor();
			SettableListenableFuture<Empty> settableListenableFuture = new SettableListenableFuture<>();
			executor.execute(() -> {
				try {
					this.ackReplyConsumer.ack();
					settableListenableFuture.set(Empty.getDefaultInstance());
				}
				catch (Throwable throwable) {
					settableListenableFuture.setException(throwable);
				}
			});

			return settableListenableFuture;
		}

		@Override
		public ListenableFuture<Empty> nack() {
			Executor executor = MoreExecutors.directExecutor();
			SettableListenableFuture<Empty> settableListenableFuture = new SettableListenableFuture<>();
			executor.execute(() -> {
				try {
					this.ackReplyConsumer.nack();
					settableListenableFuture.set(Empty.getDefaultInstance());
				}
				catch (Throwable throwable) {
					settableListenableFuture.setException(throwable);
				}
			});

			return settableListenableFuture;
		}

		@Override
		public String toString() {
			return "PushedAcknowledgeablePubsubMessage{" +
					"message=" + getPubsubMessage() +
					", projectId='" + getProjectId() + '\'' +
					", subscriptionName='" + getSubscriptionName() + '\'' +
					'}';
		}
	}

}
