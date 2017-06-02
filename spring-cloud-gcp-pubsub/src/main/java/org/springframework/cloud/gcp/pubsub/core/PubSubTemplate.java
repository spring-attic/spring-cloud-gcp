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
 *
 */

package org.springframework.cloud.gcp.pubsub.core;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.InstantiatingExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.cloud.pubsub.spi.v1.TopicAdminSettings;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.converters.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * @author Vinicius Carvalho
 * @author João André Martins
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {

	private final String projectId;

	private final GoogleCredentials credentials;

	private ConcurrentHashMap<String, Publisher> publishers = new ConcurrentHashMap<>();

	private ExecutorProvider executorProvider;

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private int concurrentProducers = 1;

	public PubSubTemplate(GoogleCredentials credentials, String projectId) {
		this.projectId = projectId;
		this.credentials = credentials;
		this.executorProvider = InstantiatingExecutorProvider.newBuilder()
				.setExecutorThreadCount(this.concurrentProducers).build();
	}

	@Override
	public String send(final String topic, Message message) {

		Publisher publisher = this.publishers.computeIfAbsent(topic, s -> {
			try {
				return Publisher.defaultBuilder(TopicName.create(this.projectId, topic))
						.setExecutorProvider(this.executorProvider)
						.setChannelProvider(
								TopicAdminSettings
										.defaultChannelProviderBuilder()
										.setCredentialsProvider(() -> this.credentials)
										.build())
						.build();
			} catch (IOException ioe) {
				throw new PubSubException("An error creating the Google Cloud Pub/Sub publisher " +
						"occurred.", ioe);
			}
		});

		Object pubsubMessageObject =
				this.messageConverter.fromMessage(message, PubsubMessage.class);

		if (!(pubsubMessageObject instanceof PubsubMessage)) {
			throw new MessageConversionException("The specified converter must produce "
					+ "PubsubMessages to send to Google Cloud Pub/Sub.");
		}

		try {
			return publisher.publish((PubsubMessage) pubsubMessageObject).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new PubSubException("An error publishing the message to the Google Cloud " +
					"Pub/Sub topic occurred.", e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}
}
