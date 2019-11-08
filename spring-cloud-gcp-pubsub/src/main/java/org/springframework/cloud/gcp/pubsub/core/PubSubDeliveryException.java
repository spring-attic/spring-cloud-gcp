/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.pubsub.core;

import com.google.pubsub.v1.PubsubMessage;

import org.springframework.lang.Nullable;

/**
 * The Spring Google Cloud Pub/Sub specific {@link PubSubException}.
 * Handles failures while publishing events to Pub/Sub.
 *
 * @author SateeshKumar Kota
 *
 * @since 1.2
 */
public class PubSubDeliveryException extends PubSubException {

	@Nullable
	private final PubsubMessage failedMessage;

	public PubSubDeliveryException(PubsubMessage pubsubMessage) {
		super(null, null);
		this.failedMessage = pubsubMessage;
	}

	public PubSubDeliveryException(String description) {
		super(description);
		this.failedMessage = null;
	}

	public PubSubDeliveryException(@Nullable String description, @Nullable Throwable cause) {
		super(description, cause);
		this.failedMessage = null;
	}

	public PubSubDeliveryException(PubsubMessage pubsubMessage, String description) {
		super(description);
		this.failedMessage = pubsubMessage;
	}

	public PubSubDeliveryException(PubsubMessage pubsubMessage, Throwable cause) {
		super(null, cause);
		this.failedMessage = pubsubMessage;
	}

	public PubSubDeliveryException(PubsubMessage pubsubMessage, @Nullable String description, @Nullable Throwable cause) {
		super(description, cause);
		this.failedMessage = pubsubMessage;
	}

	public PubsubMessage getFailedMessage() {
		return this.failedMessage;
	}

	@Override
	public String toString() {

		return super.toString() + (this.failedMessage == null ? ""
				: (", failedMessage=" + this.failedMessage));
	}
}
