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

import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.util.Assert;

/**
 * Pub/Sub specific implementation of {@link AcknowledgmentCallback}.
 *
 * <p>{@link AcknowledgmentCallback#isAcknowledged()} is implemented for semantic accuracy
 * only, since in Pub/Sub acknowledging a message more than once is not an error.
 * <p>{@link AcknowledgmentCallback#noAutoAck()} is not implemented; the correct way to
 * enable manual acking is through configuring {@link PubSubMessageSource}.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
public class PubSubAcknowledgmentCallback implements AcknowledgmentCallback {

	private final AcknowledgeablePubsubMessage message;

	private final AckMode ackMode;

	private boolean acknowledged;

	public PubSubAcknowledgmentCallback(AcknowledgeablePubsubMessage message, AckMode ackMode) {
		Assert.notNull(message, "message to be acknowledged cannot be null");
		Assert.notNull(ackMode, "ackMode cannot be null");
		this.message = message;
		this.ackMode = ackMode;
	}

	/**
	 * In {@link AckMode#AUTO_ACK} mode, nacking cannot be done through this callback.
	 * <p>Use {@link org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders#ORIGINAL_MESSAGE}
	 * to nack instead.
	 */
	@Override
	public void acknowledge(Status status) {
		if (status == AcknowledgmentCallback.Status.ACCEPT) {
			this.message.ack();
		}
		else if (this.ackMode == AckMode.MANUAL || this.ackMode == AckMode.AUTO) {
			this.message.nack();
		}
		this.acknowledged = true;
	}

	@Override
	public boolean isAutoAck() {
		return this.ackMode != AckMode.MANUAL;
	}

	@Override
	public boolean isAcknowledged() {
		return this.acknowledged;
	}
}
