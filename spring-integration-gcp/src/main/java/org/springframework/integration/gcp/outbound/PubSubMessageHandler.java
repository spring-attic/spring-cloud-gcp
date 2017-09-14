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

package org.springframework.integration.gcp.outbound;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.Assert;

/**
 * Sends messages to Google Cloud Pub/Sub by delegating to {@link PubSubTemplate}.
 *
 * @author João André Martins
 */
public class PubSubMessageHandler extends AbstractMessageHandler {

	private final PubSubOperations pubSubTemplate;

	private MessageConverter messageConverter;

	private String topic;

	public PubSubMessageHandler(PubSubTemplate pubSubTemplate) {
		this.pubSubTemplate = pubSubTemplate;

		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		this.messageConverter =
				new CompositeMessageConverter(ImmutableList.of(stringMessageConverter));
	}

	@Override
	protected void handleMessageInternal(Message<?> message) {
		String payload = (String) this.messageConverter.fromMessage(message, String.class);

		Map<String, String> headers = new HashMap<>();
		message.getHeaders().forEach(
				(key, value) -> headers.put(key, value.toString()));

		this.pubSubTemplate.publish(this.topic, payload, headers);
	}

	public String getTopic() {
		return this.topic;
	}

	public void setTopic(String topic) {
		Assert.notNull(topic, "The topic can't be null.");
		this.topic = topic;
	}
}
