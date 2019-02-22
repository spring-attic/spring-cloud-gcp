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

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

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

	private ScheduledExecutorService executorService;

	private PubSubSubscriberOperations subscriberOperations;

	public PubSubReactiveFactory(ScheduledExecutorService executorService, PubSubSubscriberOperations subscriberOperations) {
		this.executorService = executorService;
		this.subscriberOperations = subscriberOperations;

	}

	public Publisher<AcknowledgeablePubsubMessage> createPolledPublisher(
			String subscriptionName, int delayInMilliseconds) {

		return Flux.<List<AcknowledgeablePubsubMessage>>create(sink -> {

			PubSubReactiveSubscription reactiveSubscription = new PubSubReactiveSubscription(sink, subscriptionName, delayInMilliseconds);
			sink.onRequest(reactiveSubscription::request);
			sink.onCancel(reactiveSubscription::cancel);

		}).flatMapIterable(Function.identity());
	}


	private class PubSubReactiveSubscription {

		private FluxSink<List<AcknowledgeablePubsubMessage>> sink;

		private String subscriptionName;

		private ScheduledFuture<?> scheduledFuture;

		private AtomicLong totalDemand;

		PubSubReactiveSubscription(FluxSink<List<AcknowledgeablePubsubMessage>> sink, String subscriptionName, int delayInMilliseconds) {

			this.sink = sink;
			this.subscriptionName = subscriptionName;
			this.totalDemand = new AtomicLong(0);

			this.scheduledFuture = executorService.scheduleWithFixedDelay(this::pullMessages, 0, delayInMilliseconds, TimeUnit.MILLISECONDS);
		}

		void request(long demand) {
			this.totalDemand.addAndGet(demand);
		}

		void cancel() {
			this.scheduledFuture.cancel(false);
		}

		private void pullMessages() {

			// does not really matter if there is another request coming in simultaneously, increasing totalDemand
			// while the ternary is in progress. The next poll will request the newly added demand.
			int numMessagesToPull = this.totalDemand.longValue() > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalDemand.longValue();

			// positive demand present
			if (numMessagesToPull > 0) {
				// pull messages if available; immediately return empty list if no messages.
				List<AcknowledgeablePubsubMessage> messages = PubSubReactiveFactory.this.subscriberOperations.pull(this.subscriptionName, numMessagesToPull, true);

				if (!messages.isEmpty()) {
					this.sink.next(messages);
					this.totalDemand.addAndGet(-1L * messages.size());
				}
			}
		}

	}

}
