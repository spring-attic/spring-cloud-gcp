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

package org.springframework.cloud.gcp.pubsub.core;

import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import com.google.pubsub.v1.SubscriptionName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.converters.PubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * @author Vinicius Carvalho
 * @author João André Martins
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {
	private static final Log LOGGER = LogFactory.getLog(PubSubTemplate.class);

	private final GcpProjectIdProvider projectIdProvider;

	private final PublisherFactory publisherFactory;

	private final SubscriberFactory subscriberFactory;

	private final SubscriberStub subscriberStub;

	private final PubSubMessageConverter pubSubMessageConverter = new PubSubMessageConverter();

	private final MessageConverter messageConverter;

	public PubSubTemplate(GcpProjectIdProvider projectIdProvider, PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory, SubscriberStub subscriberStub, MessageConverter messageConverter) {
		this.projectIdProvider = projectIdProvider;
		this.publisherFactory = publisherFactory;
		this.subscriberFactory = subscriberFactory;
		this.subscriberStub = subscriberStub;
		this.messageConverter = messageConverter;
	}

	@Override
	public Publisher getPublisher(String topic) {
		return this.publisherFactory.getPublisher(topic);
	}

	public String send(String topic, String message) {
		return send(topic, message, null);
	}

	@Override
	public String send(String topic, Message message) {
		ListenableFuture<String> id = sendAsync(topic, message);
		try {
			return id.get();
		}
		catch (InterruptedException e) {
			LOGGER.debug("Caught interrupted exception when waiting for message ID. Topic: "
					+ topic + ". Returning null.");
			return null;
		}
		catch (ExecutionException e) {
			throw new PubSubException("Caught execution exception when waiting for message ID. Topic: " + topic, e);
		}
	}

	@Override
	public String send(String topic, Object message) {
		return send(topic, message, new MessageHeaders(new HashMap<>()));
	}

	@Override
	public String send(String topic, Object message, MessageHeaders headers) {
		return send(topic, this.messageConverter.toMessage(message, headers));
	}

	@Override
	public ListenableFuture<String> sendAsync(final String topic, Message message) {
		// Convert from payload into PubsubMessage.
		PubsubMessage pubsubMessageObject = (PubsubMessage) this.pubSubMessageConverter.fromMessage(message,
				PubsubMessage.class);

		// Send message to Google Cloud Pub/Sub.
		ApiFuture<String> publishFuture = this.publisherFactory.getPublisher(topic)
				.publish((PubsubMessage) pubsubMessageObject);

		final SettableListenableFuture<String> settableFuture = new SettableListenableFuture<>();
		ApiFutures.addCallback(publishFuture, new ApiFutureCallback<String>() {

			@Override
			public void onFailure(Throwable throwable) {
				settableFuture.setException(throwable);
			}

			@Override
			public void onSuccess(String result) {
				settableFuture.set(result);
			}

		});

		return settableFuture;
	}

	@Override
	public ListenableFuture<String> sendAsync(String topic, Object object) {
		return sendAsync(topic, object, new MessageHeaders(new HashMap<>()));
	}

	@Override
	public ListenableFuture<String> sendAsync(String topic, Object payload, MessageHeaders headers) {
		return sendAsync(topic, this.messageConverter.toMessage(payload, headers));
	}

	@Override
	public Message<String> pull(String subscription, boolean returnImmediately) {
		PullRequest request = PullRequest.newBuilder()
				.setSubscriptionWithSubscriptionName(
						SubscriptionName.create(this.projectIdProvider.getProjectId(), subscription))
				.setMaxMessages(1)
				.setReturnImmediately(returnImmediately)
				.build();
		PullResponse response = this.subscriberStub.pullCallable().call(request);
		if (response.getReceivedMessagesCount() == 0) {
			return null;
		}

		ReceivedMessage message = response.getReceivedMessages(0);
		return (Message<String>) this.pubSubMessageConverter.toMessage(message.getMessage(), null);
	}

	@Override
	public <T> Message<T> pull(String subscrption, Class<T> clazz, boolean returnImmediately) {
		return null;
	}

	@Override
	public Subscriber subscribe(String subscription, MessageReceiver messageReceiver) {
		return this.subscriberFactory.getSubscriber(subscription, messageReceiver);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}
}
