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

import com.google.pubsub.v1.PubsubMessage;

/**
 * A {@link PubsubMessage} wrapper that allows it to be acknowledged.
 *
 * <p>To acknowledge {@link PulledAcknowledgeablePubsubMessage} in bulk, using a
 * {@link org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations#ack(java.util.Collection)}
 * is recommended.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Doug Hoard
 */
public interface PulledAcknowledgeablePubsubMessage extends AcknowledgeablePubsubMessage {

	@Override
	PubsubMessage getPubsubMessage();

	String getAckId();

	String getSubscriptionName();

	@Override
	void ack();

	void ack(boolean async);

	@Override
	void nack();

	void nack(boolean async);

	void modifyAckDeadline(int ackDeadlineSeconds);

	void modifyAckDeadline(int ackDeadlineSeconds, boolean async);

}
