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

import java.util.Collection;

/**
 * Acknowledges Pub/Sub messages based on their subscription name and ack ID.
 *
 * @author João André Martins
 */
public interface PubSubAcknowledger {

	/**
	 * Acknowledge a bulk of messages based on their ack IDs which pertain to a subscription name.
	 * @param ackIds ack IDs to be acknowledged
	 * @param subscriptionName subscription names to which the ack IDs belong. Must be in the form
	 * "projects/[GCP_PROJECT_ID]/subscriptions/[PUBSUB_SUBSCRIPTION_ID]"
	 */
	void ack(Collection<String> ackIds, String subscriptionName);

	/**
	 * Negatively acknowledge a bulk of messages based on their ack IDs which pertain to a
	 * subscription name.
	 * @param ackIds ack IDs to be acknowledged
	 * @param subscriptionName subscription names to which the ack IDs belong. Must be in the form
	 * "projects/[GCP_PROJECT_ID]/subscriptions/[PUBSUB_SUBSCRIPTION_ID]"
	 */
	void nack(Collection<String> ackIds, String subscriptionName);
}
