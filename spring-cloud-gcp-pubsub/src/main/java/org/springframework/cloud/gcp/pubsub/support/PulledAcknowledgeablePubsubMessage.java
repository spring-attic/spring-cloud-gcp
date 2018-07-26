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
 * <p>To acknowledge {@link PulledAcknowledgeablePubsubMessage} in bulk, using a
 * {@link org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations#ack(java.util.Collection)}
 * is recommended.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class PulledAcknowledgeablePubsubMessage implements AcknowledgeablePubsubMessage {

	private PubsubMessage message;

	private String ackId;

	private String subscriptionName;

	private SubscriberStub subscriberStub;

	public PulledAcknowledgeablePubsubMessage(PubsubMessage message, String ackId,
			String subscriptionName, SubscriberStub subscriberStub) {
		this.message = message;
		this.ackId = ackId;
		this.subscriptionName = subscriptionName;
		this.subscriberStub = subscriberStub;
	}

	@Override
	public PubsubMessage getPubsubMessage() {
		return this.message;
	}

	public String getAckId() {
		return this.ackId;
	}

	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	@Override
	public void ack() {
		AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
				.addAckIds(this.ackId)
				.setSubscription(this.subscriptionName)
				.build();

		this.subscriberStub.acknowledgeCallable().call(acknowledgeRequest);
	}

	@Override
	public void nack() {
		modifyAckDeadline(0);
	}

	public void modifyAckDeadline(int ackDeadlineSeconds) {
		ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest.newBuilder()
				.setAckDeadlineSeconds(ackDeadlineSeconds)
				.addAckIds(this.ackId)
				.setSubscription(this.subscriptionName)
				.build();

		this.subscriberStub.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
	}

}
