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

import java.nio.charset.Charset;
import java.util.Map;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Default implementation of {@link PubSubOperations}.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(PubSubTemplate.class);

	private final PublisherFactory publisherFactory;

	private final SubscriberFactory subscriberFactory;

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
				this.publisherFactory.getPublisher(topic).publish(pubsubMessage);

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
		Subscriber subscriber = this.subscriberFactory.getSubscriber(subscription, messageHandler);
		subscriber.startAsync();
		return subscriber;
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
