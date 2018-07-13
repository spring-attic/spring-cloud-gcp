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

package org.springframework.cloud.gcp.pubsub.integration;

/**
 * Determines acknowledgement policy for incoming messages from Google Cloud Pub/Sub.
 *
 * @author João André Martins
 * @author Doug Hoard
 */
public enum AckMode {

	/**
	 * The framework does not ack / nack the message. The
	 * {@link com.google.cloud.pubsub.v1.AckReplyConsumer} is sent back to the user
	 * for acking or nacking the {@link com.google.pubsub.v1.PubsubMessage}.
	 */
	MANUAL,

	/**
	 * The framework acks the {@link com.google.pubsub.v1.PubsubMessage} after it is
	 * sent to the channel and processed successfully.
	 * <p>The framework nacks the {@link com.google.pubsub.v1.PubsubMessage} when an exception
	 * occurs while processing the message.
	 */
	AUTO,

	/**
	 * The framework acks the {@link com.google.pubsub.v1.PubsubMessage} after it is
	 * sent to the channel and processed successfully.
	 * <p>The framework does not immediately nack the message when the exception occurs,
	 * and allows the eventual redelivery to take effect.
	 * @since 1.1
	 */
	AUTO_ACK

}
