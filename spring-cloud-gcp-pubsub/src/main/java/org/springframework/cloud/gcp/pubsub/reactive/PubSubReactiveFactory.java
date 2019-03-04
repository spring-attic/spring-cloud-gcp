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
import java.util.concurrent.TimeUnit;

import com.google.api.gax.rpc.DeadlineExceededException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;

/**
 * Creates a Flux respecting backpressure by way of Pub/Sub Synchronous Pull.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
public final class PubSubReactiveFactory {

	private static final Log LOGGER = LogFactory.getLog(PubSubReactiveFactory.class);

	private PubSubSubscriberOperations subscriberOperations;

	private long pollingPeriod;

	/**
	 * Instantiates `PubSubReactiveFactory` with a `PubSubSubscriberOperations` template for interacting with GCP Pub/Sub,
	 * and a default polling period to use in case of infinite demand.
	 *
	 * @param subscriberOperations template for interacting with GCP Pub/Sub subscriber operations.
	 * @param pollingPeriod amount of time to wait between polling attempts, in milliseconds.
	 */
	public PubSubReactiveFactory(PubSubSubscriberOperations subscriberOperations, long pollingPeriod) {
		this.subscriberOperations = subscriberOperations;
		this.pollingPeriod = pollingPeriod;
	}

	/**
	 * Creates an infinite stream {@link Publisher} of {@link AcknowledgeablePubsubMessage} objects.
	 * <p>For unlimited demand, the underlying subscription will be polled at a regular interval, specified at
	 * `PubSubReactiveFactory` creation time.
	 * <p>For specific demand, as many messages as are available will be returned immediately, with remaining demand being
	 * fulfilled in the future. Pub/Sub timeout will cause a retry with the same demand.
	 * @param subscriptionName subscription from which to retrieve messages.
	 * @return unlimited stream of {@link AcknowledgeablePubsubMessage} objects.
	 */
	public Publisher<AcknowledgeablePubsubMessage> createPolledPublisher(String subscriptionName) {

		return Flux.<AcknowledgeablePubsubMessage>create(sink -> {
			sink.onRequest((numRequested) -> {
				LOGGER.info("****  " + this.toString() + " " + numRequested + " Requested ");
				if (numRequested == Long.MAX_VALUE) {
					// unlimited demand
					Disposable task = Schedulers.single().schedulePeriodically(
							new PubSubNonBlockingUnlimitedDemandPullTask(subscriptionName, sink), 0, pollingPeriod, TimeUnit.MILLISECONDS);
					sink.onCancel(() -> {
						task.dispose();
					});
				}
				else {
					Schedulers.single().schedule(new PubSubBlockingLimitedDemandPullTask(subscriptionName, numRequested, sink));
				}
			});

		});
	}

	private abstract class PubSubPullTask implements Runnable {

		protected final String subscriptionName;

		protected final FluxSink<AcknowledgeablePubsubMessage> sink;

		PubSubPullTask(String subscriptionName, FluxSink<AcknowledgeablePubsubMessage> sink) {
			this.subscriptionName = subscriptionName;
			this.sink = sink;
		}

		protected List<AcknowledgeablePubsubMessage> pullToSink(long demand, boolean block) {
			int numMessagesToPull = demand > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) demand;

			List<AcknowledgeablePubsubMessage> messages =  PubSubReactiveFactory.this.subscriberOperations.pull(
					this.subscriptionName, numMessagesToPull, !block);

			if (messages.size() > 0) {
				LOGGER.info("**** unbounded: " + this.toString() + " Retrieved " + messages.size() + " messages. ");
				messages.forEach(sink::next);
			}

			return messages;
		}

	}

	private class PubSubBlockingLimitedDemandPullTask extends PubSubPullTask {

		private final long initialDemand;

		PubSubBlockingLimitedDemandPullTask(String subscriptionName, long initialDemand, FluxSink sink) {
			super(subscriptionName, sink);
			this.initialDemand = initialDemand;
		}

		@Override
		public void run() {
			long demand = this.initialDemand;
			List<AcknowledgeablePubsubMessage> messages = null;

			while (demand > 0 && !this.sink.isCancelled()) {
				try {
					messages = pullToSink(demand, true);
					LOGGER.info("**** " + this.toString() + " decreasing demand from  " + demand + " to " + (demand - messages.size()));
					demand -= messages.size();
				}
				catch (DeadlineExceededException e) {
					LOGGER.trace("Blocking pull timed out due to empty subscription " + this.subscriptionName + "; retrying.");
				}
			}

			LOGGER.info("**** " + this.toString() + " Completed fulfilling demand of initial size " + this.initialDemand);
		}

	}

	private class PubSubNonBlockingUnlimitedDemandPullTask extends PubSubPullTask {

		PubSubNonBlockingUnlimitedDemandPullTask(String subscriptionName, FluxSink sink) {
			super(subscriptionName, sink);
		}

		@Override
		public void run() {
			pullToSink(Integer.MAX_VALUE, false);
		}

	}


}
