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

package org.springframework.cloud.gcp.pubsub.reactive;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;

/**
 * Creates a Flux populated by using Pub/Sub Synchronous Pull.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
public final class PubSubReactiveFactory {

	private PubSubSubscriberOperations subscriberOperations;

	public PubSubReactiveFactory(PubSubSubscriberOperations subscriberOperations) {
		this.subscriberOperations = subscriberOperations;

	}

	public Publisher<AcknowledgeablePubsubMessage> createPolledPublisher(
			String subscriptionName, int delayInMilliseconds) {

		PubSubReactiveSubscription reactiveSubscription = new PubSubReactiveSubscription(subscriptionName);

		return Flux.interval(Duration.ofMillis(delayInMilliseconds))
				.doOnRequest(reactiveSubscription::addDemand)
				.map(t -> reactiveSubscription.pullMessages())
				.flatMapIterable(Function.identity());
	}


	private class PubSubReactiveSubscription {

		private String subscriptionName;

		private AtomicLong totalDemand;

		PubSubReactiveSubscription(String subscriptionName) {
			this.subscriptionName = subscriptionName;
			this.totalDemand = new AtomicLong(0);
		}

		void addDemand(long demand) {
			this.totalDemand.addAndGet(demand);
		}

		private List<AcknowledgeablePubsubMessage> pullMessages() {

			// does not really matter if there is another request coming in simultaneously, increasing totalDemand
			// while the ternary is in progress. The next poll will request the newly added demand.
			int numMessagesToPull = this.totalDemand.longValue() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalDemand.longValue();

			// positive demand present
			if (numMessagesToPull > 0) {
				// pull messages if available; immediately return empty list if no messages.
				List<AcknowledgeablePubsubMessage> messages = PubSubReactiveFactory.this.subscriberOperations.pull(this.subscriptionName, numMessagesToPull, true);
				this.totalDemand.addAndGet(-1L * messages.size());
				return messages;
			}

			return Collections.emptyList();
		}

	}

}
