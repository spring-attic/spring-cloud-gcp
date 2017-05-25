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

package org.springframework.integration.gcp.outbound;

import java.io.IOException;

import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;

import org.springframework.cloud.gcp.pubsub.converters.SimpleMessageConverter;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Converts from internal Spring message to GCP Pub/Sub message and publishes the Pub/Sub
 * message to a Pub/Sub topic.
 *
 * @author João André Martins
 */
public class PubSubMessageHandler extends AbstractMessageHandler {

	private Publisher publisher;
	private MessageConverter messageConverter;

	public PubSubMessageHandler(String projectId, String topicName) throws IOException {
		this(projectId, topicName, new SimpleMessageConverter());
	}

	public PubSubMessageHandler(String projectId, String topicName,
			MessageConverter messageConverter) throws IOException {
		publisher = Publisher.defaultBuilder(TopicName.create(projectId, topicName))
				.build();
		this.messageConverter = messageConverter;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Object pubsubMessageObject = messageConverter.fromMessage(message,
				PubsubMessage.class);

		if (!(pubsubMessageObject instanceof PubsubMessage)) {
			throw new MessageConversionException("The specified converter must produce"
					+ "PubsubMessages to send to Google Cloud Pub/Sub.");
		}

		publisher.publish((PubsubMessage) pubsubMessageObject);
	}
}
