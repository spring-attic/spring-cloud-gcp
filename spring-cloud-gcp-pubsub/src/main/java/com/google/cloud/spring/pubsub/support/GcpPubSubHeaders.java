/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spring.pubsub.support;

import java.util.Optional;

import org.springframework.messaging.Message;

/**
 * Google Cloud Platform internal headers for Spring Messaging messages.
 *
 * @author João André Martins
 * @author Elena Felder
 * @author Chengyuan Zhao
 */
public abstract class GcpPubSubHeaders {

	private GcpPubSubHeaders() {
	}

	private static final String PREFIX = "gcp_pubsub_";

	/**
	 * The topic header text.
	 */
	public static final String TOPIC = PREFIX + "topic";

	/**
	 * The original message header text.
	 */
	public static final String ORIGINAL_MESSAGE = PREFIX + "original_message";

	/**
	 * The Pub/Sub message ordering key.
	 */
	public static final String ORDERING_KEY = PREFIX + "ordering_key";

	/**
	 * A simple utility method for pulling the {@link #ORIGINAL_MESSAGE} header out of a {@link Message}.
	 *
	 * @param message The Spring Message that was converted by a
	 * {@link com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter}.
	 * @return An Optional possibly containing a BasicAcknowledgeablePubsubMessage for acking and nacking.
	 */
	public static Optional<BasicAcknowledgeablePubsubMessage> getOriginalMessage(Message<?> message) {
		Object originalMessage = message.getHeaders().get(ORIGINAL_MESSAGE);
		if (originalMessage instanceof BasicAcknowledgeablePubsubMessage) {
			return Optional.of((BasicAcknowledgeablePubsubMessage) originalMessage);
		}
		return Optional.empty();
	}
}
