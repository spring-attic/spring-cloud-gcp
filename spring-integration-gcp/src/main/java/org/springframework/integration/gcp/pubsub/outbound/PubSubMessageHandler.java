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

package org.springframework.integration.gcp.pubsub.outbound;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.Assert;

/**
 * Sends messages to Google Cloud Pub/Sub by delegating to {@link PubSubOperations}.
 *
 * @author João André Martins
 */
public class PubSubMessageHandler extends AbstractMessageHandler {

	private final PubSubOperations pubSubTemplate;

	private MessageConverter messageConverter = new StringMessageConverter();

	private String topic;

	public PubSubMessageHandler(PubSubOperations pubSubTemplate, String topic) {
		this.pubSubTemplate = pubSubTemplate;
		this.topic = topic;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		Object payload = message.getPayload();

		if (payload instanceof PubsubMessage) {
			this.pubSubTemplate.publish(this.topic, (PubsubMessage) payload);
			return;
		}

		ByteString pubsubPayload;

		if (payload instanceof byte[]) {
			pubsubPayload = ByteString.copyFrom((byte[]) payload);
		}
		else if (payload instanceof ByteString) {
			pubsubPayload = (ByteString) payload;
		}
		else {
			pubsubPayload =	ByteString.copyFrom(
					(String) this.messageConverter.fromMessage(message, String.class),
					Charset.defaultCharset());
		}

		Map<String, String> headers = new HashMap<>();
		message.getHeaders().forEach(
				(key, value) -> headers.put(key, value.toString()));

		this.pubSubTemplate.publish(this.topic, pubsubPayload, headers);
	}

	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter,
				"The specified message converter can't be null.");
		this.messageConverter = messageConverter;
	}
}
