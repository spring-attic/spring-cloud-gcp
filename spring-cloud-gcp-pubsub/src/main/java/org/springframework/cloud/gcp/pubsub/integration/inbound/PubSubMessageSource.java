/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.PubSubHeaderMapper;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.util.Assert;

/**
 * A <a href="https://cloud.google.com/pubsub/docs/pull#pubsub-pull-messages-sync-java">PubSub Synchronous pull</a>
 * implementation of {@link AbstractMessageSource}.
 *
 * Enables more even load balancing with polling adapters, at the cost of efficiency gained from batched pulls.
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public class PubSubMessageSource extends AbstractMessageSource<Object> {

	private final String subscriptionName;

	private final PubSubSubscriberOperations pubSubSubscriberOperations;

	private AckMode ackMode = AckMode.MANUAL;

	private HeaderMapper<Map<String, String>> headerMapper = new PubSubHeaderMapper();

	private Class payloadType = byte[].class;

	public PubSubMessageSource(PubSubSubscriberOperations pubSubSubscriberOperations, String subscriptionName) {
		Assert.notNull(pubSubSubscriberOperations, "Pub/Sub subscriber template can't be null.");
		Assert.notNull(subscriptionName, "Pub/Sub subscription name can't be null.");
		this.pubSubSubscriberOperations = pubSubSubscriberOperations;
		this.subscriptionName = subscriptionName;
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

	@Override
	protected Object doReceive() {
		List<ConvertedAcknowledgeablePubsubMessage> messages
				= this.pubSubSubscriberOperations.pullAndConvert(this.subscriptionName, 1, true, this.payloadType);

		if (messages.isEmpty()) {
			return null;
		}

		Assert.state(messages.size() == 1, "Only one Pub/Sub message expected.");
		ConvertedAcknowledgeablePubsubMessage message = messages.get(0);

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

	@Override
	public String getComponentType() {
		return "gcp-pubsub:message-source";
	}

}
