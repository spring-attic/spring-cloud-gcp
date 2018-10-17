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

package org.springframework.cloud.gcp.pubsub.integration.inbound;

import java.util.Map;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Subscriber;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.pubsub.core.PubSubException;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.PubSubHeaderMapper;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Converts from GCP Pub/Sub message to Spring message and sends the Spring message to the
 * attached channels.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Doug Hoard
 */
public class PubSubInboundChannelAdapter extends MessageProducerSupport {

	private static final Log LOGGER = LogFactory.getLog(PubSubInboundChannelAdapter.class);

	private final String subscriptionName;

	private final PubSubSubscriberOperations pubSubSubscriberOperations;

	private Subscriber subscriber;

	private AckMode ackMode = AckMode.AUTO;

	private HeaderMapper<Map<String, String>> headerMapper = new PubSubHeaderMapper();

	private Class payloadType = byte[].class;

	public PubSubInboundChannelAdapter(PubSubSubscriberOperations pubSubSubscriberOperations, String subscriptionName) {
		Assert.notNull(pubSubSubscriberOperations, "Pub/Sub subscriber template can't be null.");
		Assert.notNull(subscriptionName, "Pub/Sub subscription name can't be null.");
		this.pubSubSubscriberOperations = pubSubSubscriberOperations;
		this.subscriptionName = subscriptionName;
	}

	public AckMode getAckMode() {
		return this.ackMode;
	}

	public void setAckMode(AckMode ackMode) {
		Assert.notNull(ackMode, "The acknowledgement mode can't be null.");
		this.ackMode = ackMode;
	}

	public Class getPayloadType() {
		return this.payloadType;
	}

	/**
	 * Set the desired type of the payload of the {@link org.springframework.messaging.Message} constructed by
	 * converting the incoming Pub/Sub message. The channel adapter will use the
	 * {@link org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter} configured for
	 * {@link PubSubSubscriberOperations#subscribeAndConvert(String, java.util.function.Consumer, Class)}. The default
	 * payload type is {@code byte[].class}.
	 * @param payloadType the type of the payload of the {@link org.springframework.messaging.Message} produced by the
	 * 				adapter. Cannot be set to null.
	 */
	public void setPayloadType(Class payloadType) {
		Assert.notNull(payloadType, "The payload type cannot be null.");
		this.payloadType = payloadType;
	}

	/**
	 * Set the header mapper to map headers from incoming {@link com.google.pubsub.v1.PubsubMessage} into
	 * {@link org.springframework.messaging.Message}.
	 * @param headerMapper the header mapper
	 */
	public void setHeaderMapper(HeaderMapper<Map<String, String>> headerMapper) {
		Assert.notNull(headerMapper, "The header mapper can't be null.");
		this.headerMapper = headerMapper;
	}

	@Override
	protected void doStart() {
		super.doStart();

		this.subscriber = this.pubSubSubscriberOperations.subscribeAndConvert(
				this.subscriptionName, this::consumeMessage, this.payloadType);
	}

	@Override
	protected void doStop() {
		if (this.subscriber != null) {
			this.subscriber.stopAsync();
		}

		super.doStop();
	}

	@SuppressWarnings("deprecation")
	private void consumeMessage(ConvertedBasicAcknowledgeablePubsubMessage message) {
		Map<String, Object> messageHeaders =
				this.headerMapper.toHeaders(message.getPubsubMessage().getAttributesMap());

		if (this.ackMode == AckMode.MANUAL) {
			// Send the original message downstream so user decides on when to ack/nack.
			messageHeaders.put(GcpPubSubHeaders.ORIGINAL_MESSAGE, message);

			// Deprecated mechanism for manual (n)acking.
			messageHeaders.put(GcpPubSubHeaders.ACKNOWLEDGEMENT, new AckReplyConsumer() {
				@Override
				public void ack() {
					LOGGER.warn("ACKNOWLEDGEMENT header is deprecated. Please use ORIGINAL_MESSAGE header to ack.");
					message.ack();
				}

				@Override
				public void nack() {
					LOGGER.warn("ACKNOWLEDGEMENT header is deprecated. Please use ORIGINAL_MESSAGE header to nack.");
					message.nack();
				}
			});
		}

		try {
			sendMessage(MessageBuilder.withPayload(message.getPayload()).copyHeaders(messageHeaders).build());
		}
		catch (RuntimeException re) {
			if (this.ackMode == AckMode.AUTO) {
				message.nack();
			}
			throw new PubSubException("Sending Spring message failed.", re);
		}

		if ((this.ackMode == AckMode.AUTO) || (this.ackMode == AckMode.AUTO_ACK)) {
			message.ack();
		}
	}

}
