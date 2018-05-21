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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.PubSubAcknowledger;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Default implementation of {@link PubSubOperations}.
 *
 * <p>The main Google Cloud Pub/Sub integration component for publishing to topics and consuming
 * messages from subscriptions asynchronously or by pulling.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(PubSubTemplate.class);

	private PubSubMessageConverter messageConverter = new JacksonPubSubMessageConverter();

	private final PublisherFactory publisherFactory;

	private final SubscriberFactory subscriberFactory;

	private final SubscriberStub subscriberStub;

	private final PubSubAcknowledger acknowledger;

	private Charset charset = Charset.defaultCharset();

	/**
	 * Default {@link PubSubTemplate} constructor that uses a {@link JacksonPubSubMessageConverter}
	 * to serialize and deserialize payloads.
	 * @param publisherFactory the {@link com.google.cloud.pubsub.v1.Publisher} factory to
	 * publish to topics
	 * @param subscriberFactory the {@link com.google.cloud.pubsub.v1.Subscriber} factory
	 * to subscribe to subscriptions
	 */
	public PubSubTemplate(PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory) {
		this.publisherFactory = publisherFactory;
		this.subscriberFactory = subscriberFactory;
		this.subscriberStub = this.subscriberFactory.createSubscriberStub();
		this.acknowledger = this.subscriberFactory.createAcknowledger();
	}

	public PubSubMessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public PubSubTemplate setMessageConverter(PubSubMessageConverter messageConverter) {
		Assert.notNull(messageConverter, "A valid PubSub message converter is required.");
		this.messageConverter = messageConverter;
		return this;
	}

	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * Set the charset to decode the string in {@link #publish(String, Object, Map)}.
	 * {@code Charset.defaultCharset()} is used by default.
	 * @param charset character set to decode the string in a byte array
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public <T> ListenableFuture<String> publish(String topic, T payload,
			Map<String, String> headers) {
		PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();

		if (headers != null) {
			pubsubMessageBuilder.putAllAttributes(headers);
		}

		if (payload instanceof String) {
			pubsubMessageBuilder.setData(
					ByteString.copyFrom(((String) payload).getBytes(this.charset)));
		}
		else if (payload instanceof ByteString) {
			pubsubMessageBuilder.setData((ByteString) payload);
		}
		else if (payload instanceof byte[]) {
			pubsubMessageBuilder.setData(ByteString.copyFrom((byte[]) payload));
		}
		else {
			try {
				pubsubMessageBuilder.setData(
						ByteString.copyFrom(this.messageConverter.toPayload(payload)));
			}
			catch (IOException ioe) {
				throw new UncheckedIOException("Error serializing payload. ", ioe);
			}
		}

		return publish(topic, pubsubMessageBuilder.build());
	}

	@Override
	public <T> ListenableFuture<String> publish(String topic, T payload) {
		return publish(topic, payload, null);
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

	public PublisherFactory getPublisherFactory() {
		return this.publisherFactory;
	}

	public SubscriberFactory getSubscriberFactory() {
		return this.subscriberFactory;
	}

	public PubSubAcknowledger getAcknowledger() {
		return this.acknowledger;
	}
}
