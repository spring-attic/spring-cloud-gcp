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

package org.springframework.integration.gcp.pubsub.inbound;

import java.util.HashMap;
import java.util.Map;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.gcp.pubsub.AckMode;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.Assert;

/**
 * Converts from GCP Pub/Sub message to Spring message and sends the Spring message to the
 * attached channels.
 *
 * @author João André Martins
 */
public class PubSubInboundChannelAdapter extends MessageProducerSupport {

	private final String subscriptionName;

	private final PubSubOperations pubSubTemplate;

	private Subscriber subscriber;

	private AckMode ackMode = AckMode.AUTO;

	private MessageConverter messageConverter;

	public PubSubInboundChannelAdapter(PubSubOperations pubSubTemplate, String subscriptionName) {
		this.pubSubTemplate = pubSubTemplate;
		this.subscriptionName = subscriptionName;

		StringMessageConverter stringMessageConverter = new StringMessageConverter();
		stringMessageConverter.setSerializedPayloadClass(String.class);
		this.messageConverter = stringMessageConverter;
	}

	@Override
	protected void doStart() {
		super.doStart();

		this.subscriber =
				this.pubSubTemplate.subscribe(this.subscriptionName, this::receiveMessage);
	}

	private void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
		Map<String, Object> messageHeaders = new HashMap<>();

		message.getAttributesMap().forEach(messageHeaders::put);

		if (this.ackMode == AckMode.MANUAL) {
			// Send the consumer downstream so user decides on when to ack/nack.
			messageHeaders.put(GcpHeaders.ACKNOWLEDGEMENT, consumer);
		}

		try {
			sendMessage(this.messageConverter.toMessage(
					message.getData().toStringUtf8(),
					new MessageHeaders(messageHeaders)));
		}
		catch (RuntimeException re) {
			if (this.ackMode == AckMode.AUTO) {
				consumer.nack();
			}
			throw re;
		}

		if (this.ackMode == AckMode.AUTO) {
			consumer.ack();
		}
	}

	@Override
	protected void doStop() {
		if (this.subscriber != null) {
			this.subscriber.stopAsync();
		}

		super.doStop();
	}

	public AckMode getAckMode() {
		return this.ackMode;
	}

	public void setAckMode(AckMode ackMode) {
		Assert.notNull(ackMode, "The acknowledgement mode can't be null.");
		this.ackMode = ackMode;
	}

	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter,
				"The specified message converter can't be null.");
		this.messageConverter = messageConverter;
	}

	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}
}
