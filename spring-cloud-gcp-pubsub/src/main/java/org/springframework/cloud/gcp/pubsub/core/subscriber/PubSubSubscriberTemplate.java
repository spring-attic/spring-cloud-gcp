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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
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
import com.google.pubsub.v1.ProjectSubscriptionName;
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
 * @author Elena Felder
 *
 * @since 1.1
 */
public class PubSubSubscriberTemplate implements PubSubSubscriberOperations {

	private final SubscriberFactory subscriberFactory;

	private final SubscriberStub subscriberStub;

	private PubSubMessageConverter pubSubMessageConverter = new SimplePubSubMessageConverter();

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
								new PushedAcknowledgeablePubsubMessage(
										ProjectSubscriptionName.of(this.subscriberFactory.getProjectId(), subscription),
										message,
										ackReplyConsumer)));
		subscriber.startAsync();
		return subscriber;
	}

	@Override
	public <T> Subscriber subscribeAndConvert(String subscription,
			Consumer<ConvertedBasicAcknowledgeablePubsubMessage<T>> messageConsumer, Class<T> payloadType) {
		Assert.notNull(messageConsumer, "The messageConsumer can't be null.");

		Subscriber subscriber =
				this.subscriberFactory.createSubscriber(subscription,
						(message, ackReplyConsumer) -> messageConsumer.accept(
								new ConvertedPushedAcknowledgeablePubsubMessage<>(
										ProjectSubscriptionName.of(this.subscriberFactory.getProjectId(), subscription),
										message,
										this.getMessageConverter().fromPubSubMessage(message, payloadType),
										ackReplyConsumer)));
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
		return pullResponse.getReceivedMessagesList().stream()
						.map(message -> new PulledAcknowledgeablePubsubMessage(
								ProjectSubscriptionName.of(
										this.subscriberFactory.getProjectId(), pullRequest.getSubscription()),
								message.getMessage(),
								message.getAckId()))
						.collect(Collectors.toList());
	}

	@Override
	public List<AcknowledgeablePubsubMessage> pull(
			String subscription, Integer maxMessages, Boolean returnImmediately) {
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

		if (ackableMessages.size() > 0) {
			ack(ackableMessages);
		}

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

	/**
	 * Acknowledge messages in per-subscription batches.
	 * If any batch fails, the returned Future is marked as failed.
	 * If multiple batches fail, the returned Future will contain whichever exception was detected first.
	 * @param acknowledgeablePubsubMessages messages, potentially from different subscriptions.
	 * @return {@link ListenableFuture} indicating overall success or failure.
	 */
	@Override
	public ListenableFuture<Void> ack(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages can't be empty.");

		return doBatchedAsyncOperation(acknowledgeablePubsubMessages, this::ack);
	}

	/**
	 * Nack messages in per-subscription batches.
	 * If any batch fails, the returned Future is marked as failed.
	 * If multiple batches fail, the returned Future will contain whichever exception was detected first.
	 * @param acknowledgeablePubsubMessages messages, potentially from different subscriptions.
	 * @return {@link ListenableFuture} indicating overall success or failure.
	 */
	@Override
	public ListenableFuture<Void> nack(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages) {
		return modifyAckDeadline(acknowledgeablePubsubMessages, 0);
	}

	/**
	 * Modify multiple messages' ack deadline in per-subscription batches.
	 * If any batch fails, the returned Future is marked as failed.
	 * If multiple batches fail, the returned Future will contain whichever exception was detected first.
	 * @param acknowledgeablePubsubMessages messages, potentially from different subscriptions.
	 * @return {@link ListenableFuture} indicating overall success or failure.
	 */
	@Override
	public ListenableFuture<Void> modifyAckDeadline(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages, int ackDeadlineSeconds) {
		Assert.notEmpty(acknowledgeablePubsubMessages, "The acknowledgeablePubsubMessages can't be empty.");
		Assert.isTrue(ackDeadlineSeconds >= 0, "The ackDeadlineSeconds must not be negative.");


		return doBatchedAsyncOperation(acknowledgeablePubsubMessages,
				(String subscriptionName, List<String> ackIds) ->
						modifyAckDeadline(subscriptionName, ackIds, ackDeadlineSeconds));
	}

	private ApiFuture<Empty> ack(String subscriptionName, Collection<String> ackIds) {
		AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();
		return this.subscriberStub.acknowledgeCallable().futureCall(acknowledgeRequest);
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

	/**
	 * Perform Pub/Sub operations (ack/nack/modifyAckDeadline) in per-subscription batches.
	 * <p>The returned {@link ListenableFuture} will complete when either all batches completes successfully or when at
	 * least one fails.</p>
	 * <p>
	 * In case of multiple batch failures, which exception will be in the final {@link ListenableFuture} is
	 * non-deterministic.
	 * </p>
	 * @param acknowledgeablePubsubMessages messages, could be from different subscriptions.
	 * @param asyncOperation specific Pub/Sub operation to perform.
	 * @return {@link ListenableFuture} indicating overall success or failure.
	 */
	private ListenableFuture<Void> doBatchedAsyncOperation(
			Collection<AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages,
			BiFunction<String, List<String>, ApiFuture<Empty>> asyncOperation) {

		Map<ProjectSubscriptionName, List<String>> groupedMessages
				= acknowledgeablePubsubMessages.stream()
				.collect(
						Collectors.groupingBy(
								AcknowledgeablePubsubMessage::getProjectSubscriptionName,
								Collectors.mapping(AcknowledgeablePubsubMessage::getAckId, Collectors.toList())));

		Assert.state(groupedMessages.keySet().stream().map(ProjectSubscriptionName::getProject).distinct().count() == 1,
				"The project id of all messages must match.");

		SettableListenableFuture<Void> settableListenableFuture	= new SettableListenableFuture<>();
		int numExpectedFutures = groupedMessages.size();
		AtomicInteger numCompletedFutures = new AtomicInteger();

		groupedMessages.forEach((ProjectSubscriptionName psName, List<String> ackIds) -> {

			ApiFuture<Empty> ackApiFuture = asyncOperation.apply(psName.getSubscription(), ackIds);

			ApiFutures.addCallback(ackApiFuture, new ApiFutureCallback<Empty>() {
				@Override
				public void onFailure(Throwable throwable) {
					processResult(throwable);
				}

				@Override
				public void onSuccess(Empty empty) {
					processResult(null);
				}

				private void processResult(Throwable throwable) {
					if (throwable != null) {
						settableListenableFuture.setException(throwable);
					}
					else if (numCompletedFutures.incrementAndGet() == numExpectedFutures) {
						settableListenableFuture.set(null);
					}
				}
			}, MoreExecutors.directExecutor());
		});

		return settableListenableFuture;
	}

	private abstract static class AbstractBasicAcknowledgeablePubsubMessage
			implements BasicAcknowledgeablePubsubMessage {

		private final ProjectSubscriptionName projectSubscriptionName;

		private final PubsubMessage message;

		AbstractBasicAcknowledgeablePubsubMessage(
				ProjectSubscriptionName projectSubscriptionName, PubsubMessage message) {
			this.projectSubscriptionName = projectSubscriptionName;
			this.message = message;
		}

		@Override
		public ProjectSubscriptionName getProjectSubscriptionName() {
			return this.projectSubscriptionName;
		}

		@Override
		public PubsubMessage getPubsubMessage() {
			return this.message;
		}
	}

	private class PulledAcknowledgeablePubsubMessage extends AbstractBasicAcknowledgeablePubsubMessage
			implements AcknowledgeablePubsubMessage {

		private final String ackId;

		PulledAcknowledgeablePubsubMessage(ProjectSubscriptionName projectSubscriptionName,
				PubsubMessage message, String ackId) {
			super(projectSubscriptionName, message);
			this.ackId = ackId;
		}

		@Override
		public String getAckId() {
			return this.ackId;
		}

		@Override
		public ListenableFuture<Void> ack() {
			return PubSubSubscriberTemplate.this.ack(Collections.singleton(this));
		}

		@Override
		public ListenableFuture<Void> nack() {
			return modifyAckDeadline(0);
		}

		@Override
		public ListenableFuture<Void> modifyAckDeadline(int ackDeadlineSeconds) {
			return PubSubSubscriberTemplate.this.modifyAckDeadline(Collections.singleton(this), ackDeadlineSeconds);
		}

		@Override
		public String toString() {
			return "PulledAcknowledgeablePubsubMessage{" +
					"projectId='" + getProjectSubscriptionName().getProject() + '\'' +
					", subscriptionName='" + getProjectSubscriptionName().getSubscription() + '\'' +
					", message=" + getPubsubMessage() +
					", ackId='" + this.ackId + '\'' +
					'}';
		}
	}

	private static class PushedAcknowledgeablePubsubMessage extends AbstractBasicAcknowledgeablePubsubMessage {

		private final AckReplyConsumer ackReplyConsumer;

		PushedAcknowledgeablePubsubMessage(ProjectSubscriptionName projectSubscriptionName, PubsubMessage message,
				AckReplyConsumer ackReplyConsumer) {
			super(projectSubscriptionName, message);
			this.ackReplyConsumer = ackReplyConsumer;
		}

		@Override
		public ListenableFuture<Void> ack() {
			SettableListenableFuture<Void> settableListenableFuture = new SettableListenableFuture<>();

			try {
				this.ackReplyConsumer.ack();
				settableListenableFuture.set(null);
			}
			catch (Throwable throwable) {
				settableListenableFuture.setException(throwable);
			}

			return settableListenableFuture;
		}

		@Override
		public ListenableFuture<Void> nack() {
			SettableListenableFuture<Void> settableListenableFuture = new SettableListenableFuture<>();

			try {
				this.ackReplyConsumer.nack();
				settableListenableFuture.set(null);
			}
			catch (Throwable throwable) {
				settableListenableFuture.setException(throwable);
			}

			return settableListenableFuture;
		}

		@Override
		public String toString() {
			return "PushedAcknowledgeablePubsubMessage{" +
					"projectId='" + getProjectSubscriptionName().getProject() + '\'' +
					", subscriptionName='" + getProjectSubscriptionName().getSubscription() + '\'' +
					", message=" + getPubsubMessage() +
					'}';
		}
	}

	private class ConvertedPulledAcknowledgeablePubsubMessage<T> extends PulledAcknowledgeablePubsubMessage
			implements ConvertedAcknowledgeablePubsubMessage<T> {

		private final T payload;

		ConvertedPulledAcknowledgeablePubsubMessage(AcknowledgeablePubsubMessage message, T payload) {
			super(message.getProjectSubscriptionName(), message.getPubsubMessage(), message.getAckId());

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

		ConvertedPushedAcknowledgeablePubsubMessage(ProjectSubscriptionName projectSubscriptionName,
				PubsubMessage message, T payload, AckReplyConsumer ackReplyConsumer) {
			super(projectSubscriptionName, message, ackReplyConsumer);
			this.payload = payload;
		}

		@Override
		public T getPayload() {
			return this.payload;
		}
	}

}
