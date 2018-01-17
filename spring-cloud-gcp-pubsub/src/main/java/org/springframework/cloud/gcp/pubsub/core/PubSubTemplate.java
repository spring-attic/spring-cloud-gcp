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

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.retrying.RetrySettings;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Default implementation of {@link PubSubOperations}.
 *
 * <p>
 * The main Google Cloud Pub/Sub integration component for publishing to topics and
 * consuming messages from subscriptions asynchronously or by pulling.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(PubSubTemplate.class);

	private final PublisherFactory publisherFactory;

	private final SubscriberFactory subscriberFactory;

	/**
	 * Default {@link PubSubTemplate} constructor.
	 *
	 * @param publisherFactory the {@link com.google.cloud.pubsub.v1.Publisher} factory to
	 *                         publish to topics
	 * @param subscriberFactory the {@link com.google.cloud.pubsub.v1.Subscriber} factory to
	 *                          subscribe to subscriptions
	 */
	public PubSubTemplate(PublisherFactory publisherFactory, SubscriberFactory subscriberFactory) {
		this.publisherFactory = publisherFactory;
		this.subscriberFactory = subscriberFactory;
	}

	@Override
	public ListenableFuture<String> publish(final String topic, String payload,
			Map<String, String> headers) {
		return publish(topic, payload, headers, Charset.defaultCharset());
	}

	@Override
	public ListenableFuture<String> publish(final String topic, String payload,
			Map<String, String> headers, Charset charset) {
		return publish(topic, payload.getBytes(charset), headers);
	}

	@Override
	public ListenableFuture<String> publish(final String topic, byte[] payload,
			Map<String, String> headers) {
		return publish(topic, ByteString.copyFrom(payload), headers);
	}

	@Override
	public ListenableFuture<String> publish(final String topic, ByteString payload,
			Map<String, String> headers) {
		PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder().setData(payload);

		if (headers != null) {
			pubsubMessageBuilder.putAllAttributes(headers);
		}

		return publish(topic, pubsubMessageBuilder.build());
	}

	@Override
	public ListenableFuture<String> publish(final String topic, PubsubMessage pubsubMessage) {
		ApiFuture<String> publishFuture =
				this.publisherFactory.createPublisher(topic).publish(pubsubMessage);

		final SettableListenableFuture<String> settableFuture = new SettableListenableFuture<>();
		ApiFutures.addCallback(publishFuture, new ApiFutureCallback<String>() {

			@Override
			public void onFailure(Throwable throwable) {
				LOGGER.warn("Publishing to " + topic + " topic failed.", throwable);
				settableFuture.setException(throwable);
			}

			@Override
			public void onSuccess(String result) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(
							"Publishing to " + topic + " was successful. Message ID: " + result);
				}
				settableFuture.set(result);
			}

		});

		return settableFuture;
	}

	@Override
	public Subscriber subscribe(String subscription, MessageReceiver messageHandler) {
		Subscriber subscriber = this.subscriberFactory.createSubscriber(subscription, messageHandler);
		subscriber.startAsync();
		return subscriber;
	}

	/**
	 * Pulls messages synchronously, on demand, using the pull request in argument.
	 *
	 * <p>
	 * This method acknowledges all received messages.
	 * @param pullRequest pull request containing the subscription name
	 * @return the list of {@link PubsubMessage} containing the headers and payload
	 */
	private List<PubsubMessage> pull(PullRequest pullRequest, RetrySettings retrySettings) {
		Assert.notNull(pullRequest, "The pull request cannot be null.");

		try {
			SubscriberStub subscriber = this.subscriberFactory.createSubscriberStub(retrySettings);
			Assert.notNull(subscriber, "A SubscriberStub is needed to execute the pull request.");

			PullResponse pullResponse =	subscriber.pullCallable().call(pullRequest);

			// Ack received messages.
			if (pullResponse.getReceivedMessagesCount() > 0) {
				List<String> ackIds = pullResponse.getReceivedMessagesList().stream()
						.map(ReceivedMessage::getAckId)
						.collect(Collectors.toList());

				AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
						.setSubscriptionWithSubscriptionName(
								pullRequest.getSubscriptionAsSubscriptionName())
						.addAllAckIds(ackIds)
						.build();

				subscriber.acknowledgeCallable().call(acknowledgeRequest);
			}

			return pullResponse.getReceivedMessagesList().stream()
					.map(ReceivedMessage::getMessage)
					.collect(Collectors.toList());
		}
		catch (Exception ioe) {
			throw new PubSubException("Error pulling messages from subscription "
					+ pullRequest.getSubscription() + ".", ioe);
		}
	}

	@Override
	public List<PubsubMessage> pull(String subscription, Integer maxMessages, Boolean returnImmediately,
			RetrySettings retrySettings) {
		return pull(this.subscriberFactory.createPullRequest(subscription, maxMessages, returnImmediately),
				retrySettings);
	}

	@Override
	public PubsubMessage pullNext(String subscription) {
		List<PubsubMessage> receivedMessageList = pull(subscription, 1, true, null);

		return receivedMessageList.size() > 0 ?	receivedMessageList.get(0) : null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	public PublisherFactory getPublisherFactory() {
		return this.publisherFactory;
	}

	public SubscriberFactory getSubscriberFactory() {
		return this.subscriberFactory;
	}
}
