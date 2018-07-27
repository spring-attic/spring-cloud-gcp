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

/**
 * An extension of {@link AcknowledgeablePubsubMessage} that exposes ack ID and subscription name of the message.
 * It also allows modification of the ack deadline and acknowledgement of multiple messages at once using
 * {@link org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations#ack(java.util.Collection)}.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public interface BatchAcknowledgeablePubsubMessage extends AcknowledgeablePubsubMessage {

	String getAckId();

	String getSubscriptionName();

	void modifyAckDeadline(int ackDeadlineSeconds);
}
