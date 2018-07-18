/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.pubsub.support;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
public class ConvertedAcknowledgeableMessage<T> implements Message<T>, AckReplyConsumer {

	private PubsubMessage pubsubMessage;

	private AckReplyConsumer ackReplyConsumer;

	private Message<T> message;

	public ConvertedAcknowledgeableMessage(PubsubMessage pubsubMessage,
			AckReplyConsumer ackReplyConsumer, T payload) {
		this.pubsubMessage = pubsubMessage;
		this.ackReplyConsumer = ackReplyConsumer;
		this.message = MessageBuilder.withPayload(payload)
				.copyHeaders(pubsubMessage.getAttributesMap())
				.build();
	}

	public ConvertedAcknowledgeableMessage(AcknowledgeablePubsubMessage acknowledgeablePubsubMessage, T payload) {
		this(acknowledgeablePubsubMessage.getMessage(), acknowledgeablePubsubMessage, payload);
	}

	@Override
	public void ack() {
		this.ackReplyConsumer.ack();
	}

	@Override
	public void nack() {
		this.ackReplyConsumer.nack();
	}

	@Override
	public T getPayload() {
		return this.message.getPayload();
	}

	@Override
	public MessageHeaders getHeaders() {
		return this.message.getHeaders();
	}

	/**
	 * Accessor for the original unconverted Pub/Sub message.
	 * @return The original Pub/Sub message.
	 */
	public PubsubMessage getPubsubMessage() {
		return this.pubsubMessage;
	}

}
