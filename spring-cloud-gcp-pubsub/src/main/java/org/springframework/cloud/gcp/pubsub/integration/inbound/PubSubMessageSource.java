/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.pubsub.integration.inbound;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.PubSubHeaderMapper;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.integration.endpoint.AbstractFetchLimitingMessageSource;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A <a href="https://cloud.google.com/pubsub/docs/pull#pubsub-pull-messages-sync-java">PubSub Synchronous pull</a>
 * implementation of {@link AbstractMessageSource}.
 *
 * <p>Enables more even load balancing with polling adapters, at the cost of efficiency gained from streaming pull.
 *
 * <p>Messages are automatically acked right before being sent to the outbound channel.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public class PubSubMessageSource extends AbstractFetchLimitingMessageSource<Object> {

	private final String subscriptionName;

	private final PubSubSubscriberOperations pubSubSubscriberOperations;

	private AckMode ackMode = AckMode.MANUAL;

	private HeaderMapper<Map<String, String>> headerMapper = new PubSubHeaderMapper();

	private Class payloadType = byte[].class;

	private ArrayDeque<ConvertedAcknowledgeablePubsubMessage> cachedMessages = new ArrayDeque<>();

	public PubSubMessageSource(PubSubSubscriberOperations pubSubSubscriberOperations,
			String subscriptionName, int maxFetchSize) {
		Assert.notNull(pubSubSubscriberOperations, "Pub/Sub subscriber template can't be null.");
		Assert.notNull(subscriptionName, "Pub/Sub subscription name can't be null.");
		Assert.isTrue(maxFetchSize > 0, "maxFetchSize has to be a positive integer.");
		this.pubSubSubscriberOperations = pubSubSubscriberOperations;
		this.subscriptionName = subscriptionName;
		super.setMaxFetchSize(maxFetchSize);
	}

	public void setAckMode(AckMode ackMode) {
		Assert.notNull(ackMode, "The acknowledgement mode can't be null.");
		this.ackMode = ackMode;
	}

	public void setPayloadType(Class payloadType) {
		Assert.notNull(payloadType, "The payload type cannot be null.");
		this.payloadType = payloadType;
	}

	public void setHeaderMapper(HeaderMapper<Map<String, String>> headerMapper) {
		Assert.notNull(headerMapper, "The header mapper can't be null.");
		this.headerMapper = headerMapper;
	}

	/**
	 * Provides a single polled message.
	 * <p>Messages are received from Pub/Sub by synchronous pull, in batches determined
	 * by {@code fetchSize}.
	 * <p>Cached messages in {@link AckMode#AUTO_ACK} or {@link AckMode#AUTO} are not
	 * automatically acked until they are taken out of cache.
	 * @param fetchSize number of messages to fetch from Pub/Sub.
	 * @return {@link Message} wrapper containing the original message.
	 */
	@Override
	protected Object doReceive(int fetchSize) {
		if (this.cachedMessages.isEmpty()) {
			List<ConvertedAcknowledgeablePubsubMessage> messages
					= this.pubSubSubscriberOperations.pullAndConvert(this.subscriptionName, fetchSize, true, this.payloadType);
			if (messages.isEmpty()) {
				return null;
			}
			else if (messages.size() == 1) {
				// don't bother storing.
				return processMessage(messages.get(0));
			}
			else {
				this.cachedMessages.addAll(messages);
			}
		}

		return processMessage(this.cachedMessages.pollFirst());
	}

	@Override
	public String getComponentType() {
		return "gcp-pubsub:message-source";
	}

	/**
	 * Applies header customizations and acknowledges the message, if necessary.
	 * @param message source Pub/Sub message.
	 * @return {@link Message} wrapper containing the original message.
	 */
	private Message<Object> processMessage(ConvertedAcknowledgeablePubsubMessage message) {

		Map<String, Object> messageHeaders =
				this.headerMapper.toHeaders(message.getPubsubMessage().getAttributesMap());

		if (this.ackMode == AckMode.MANUAL) {
			// Send the original message downstream so user decides on when to ack/nack.
			messageHeaders.put(GcpPubSubHeaders.ORIGINAL_MESSAGE, message);
		}
		else {
			message.ack();
		}

		return getMessageBuilderFactory()
				.withPayload(message.getPayload())
				.copyHeaders(messageHeaders)
				.build();
	}

}
