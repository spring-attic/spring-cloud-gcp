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

import com.google.api.gax.grpc.ExecutorProvider;
import com.google.api.gax.grpc.InstantiatingExecutorProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.converters.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;

/**
 * @author Vinicius Carvalho
 */
public class PubSubTemplate implements PubSubOperations, InitializingBean {

	private final String projectId;

	private final GoogleCredentials credentials;

	private ConcurrentHashMap<String, Publisher> publishers = new ConcurrentHashMap<>();

	private ExecutorProvider executorProvider;

	private MessageConverter converter;

	private int concurrentProducers = 1;

	public PubSubTemplate(GoogleCredentials credentials, String projectId) {
		this.projectId = projectId;
		this.credentials = credentials;
		this.executorProvider = InstantiatingExecutorProvider.newBuilder()
				.setExecutorThreadCount(concurrentProducers).build();
		this.converter = new SimpleMessageConverter();
	}

	@Override
	public String send(final String topic, Message message) throws RuntimeException {

		Publisher publisher = publishers.computeIfAbsent(topic, s -> {
			try {
				return Publisher.defaultBuilder(TopicName.create(projectId, topic))
						.setExecutorProvider(executorProvider).build();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		});

		try {
			return publisher.publish(PubsubMessage.newBuilder()
					.setData(ByteString
							.copyFrom(message.getPayload().toString().getBytes()))
					.build()).get();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}
}
