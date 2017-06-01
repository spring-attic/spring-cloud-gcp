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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.InstantiatingExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.cloud.pubsub.spi.v1.TopicAdminSettings;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.converters.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * @author Vinicius Carvalho
 * @author João André Martins
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {

	private static final String DEFAULT_SOURCE_NAME = "spring";

	private final String projectId;

	private final GoogleCredentials credentials;

	private ConcurrentHashMap<String, Publisher> publishers = new ConcurrentHashMap<>();

	/**
	 * Aimed at reusing the same executor across every {@link Publisher}, so the number of allocated
	 * threads doesn't blow out of proportion.
	 */
	private ExecutorProvider executorProvider;

	private MessageConverter messageConverter = new SimpleMessageConverter();

	/**
	 * The Spring component a Google Cloud Pub/Sub call is originated from.
	 *
	 * <p>Spring Integration callers should set this to "spring-integration", Spring Cloud to
	 * "spring-cloud", etc.
	 */
	private String sourceName = DEFAULT_SOURCE_NAME;

	/**
	 * The version of the Spring component a Google Cloud Pub/Sub call is originated from.
	 *
	 * <p>By default, the version is obtained from this package's manifest file, which is mirrored
	 * from this package's pom.xml file. Ideally, this would be the version of the library invoking
	 * this call (e.g., Spring Integration, Spring Cloud, etc.).
	 */
	private String sourceVersion;

	private int concurrentProducers = 1;

	public PubSubTemplate(GoogleCredentials credentials, String projectId) {
		this.projectId = projectId;
		this.credentials = credentials;
		this.sourceVersion = this.getClass().getPackage().getImplementationVersion();
		this.executorProvider = InstantiatingExecutorProvider.newBuilder()
				.setExecutorThreadCount(this.concurrentProducers).build();
	}

	@Override
	public ListenableFuture<String> send(final String topic, Message message) {
		// Reuse existing publisher if one already exists.
		Publisher publisher = this.publishers.computeIfAbsent(topic, key -> {
			try {
				return Publisher.defaultBuilder(TopicName.create(this.projectId, key))
						.setExecutorProvider(this.executorProvider)
						.setChannelProvider(
								TopicAdminSettings
										.defaultChannelProviderBuilder()
										.setCredentialsProvider(() -> this.credentials)
										.setClientLibHeader(this.sourceName, this.sourceVersion)
										.build())
						.build();
			}
			catch (IOException ioe) {
				throw new PubSubException("An error creating the Google Cloud Pub/Sub publisher " +
						"occurred.", ioe);
			}
		});

		// Convert from payload into PubsubMessage.
		Object pubsubMessageObject =
				this.messageConverter.fromMessage(message, PubsubMessage.class);

		if (!(pubsubMessageObject instanceof PubsubMessage)) {
			throw new MessageConversionException("The specified converter must produce "
					+ "PubsubMessages to send to Google Cloud Pub/Sub.");
		}

		// Send message to Google Cloud Pub/Sub.
		ApiFuture<String> publishFuture = publisher.publish((PubsubMessage) pubsubMessageObject);

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
	public void afterPropertiesSet() throws Exception {
	}

	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}
}
