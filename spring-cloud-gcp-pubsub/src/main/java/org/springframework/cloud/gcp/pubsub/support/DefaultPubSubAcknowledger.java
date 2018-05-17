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

import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;

/**
 * Uses the Google Cloud Pub/Sub client library to (negatively) acknowledge messages based on their
 * ack IDs and subscription name.
 *
 * @author João André Martins
 */
class DefaultPubSubAcknowledger implements PubSubAcknowledger {

	private SubscriberStub subscriberStub;

	DefaultPubSubAcknowledger(SubscriberStub subscriberStub) {
		this.subscriberStub = subscriberStub;
	}

	@Override
	public void ack(Collection<String> ackIds, String subscriptionName) {
		AcknowledgeRequest acknowledgeRequest = AcknowledgeRequest.newBuilder()
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		this.subscriberStub.acknowledgeCallable().call(acknowledgeRequest);
	}

	@Override
	public void nack(Collection<String> ackIds, String subscriptionName) {
		ModifyAckDeadlineRequest modifyAckDeadlineRequest = ModifyAckDeadlineRequest.newBuilder()
				.setAckDeadlineSeconds(0)
				.addAllAckIds(ackIds)
				.setSubscription(subscriptionName)
				.build();

		this.subscriberStub.modifyAckDeadlineCallable().call(modifyAckDeadlineRequest);
	}
}
