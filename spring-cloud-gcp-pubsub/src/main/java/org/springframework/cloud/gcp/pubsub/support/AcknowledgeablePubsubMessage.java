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

import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;

/**
 * A {@link PubsubMessage} wrapper that allows it to be acknowledged.
 *
 * <p>To acknowledge {@link AcknowledgeablePubsubMessage} in bulk, using a
 * {@link org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations#ack(java.util.Collection)}
 * is recommended. To do that, a user must first extract the ack IDs and group them by subscription name.
 *
 * @author João André Martins
 */
public class AcknowledgeablePubsubMessage {

	private PubsubMessage message;

	private String ackId;

	private String subscriptionName;

	private SubscriberStub subscriberStub;

	public AcknowledgeablePubsubMessage(PubsubMessage message, String ackId,
			String subscriptionName, SubscriberStub subscriberStub) {
		this.message = message;
		this.ackId = ackId;
		this.subscriptionName = subscriptionName;
		this.subscriberStub = subscriberStub;
	}

	public PubsubMessage getMessage() {
		return this.message;
	}

	public String getAckId() {
		return this.ackId;
	}

	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	public void ack() {
		AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
				.addAckIds(this.ackId)
				.setSubscription(this.subscriptionName)
				.build();

		this.subscriberStub.acknowledgeCallable().call(acknowledgeRequest);
	}

	public void nack() {
		ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest.newBuilder()
				.setAckDeadlineSeconds(0)
				.addAckIds(this.ackId)
				.setSubscription(this.subscriptionName)
				.build();

		this.subscriberStub.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
	}

}
