/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.util.Assert;

/**
 * A factory for procuring {@link Flux} instances backed by GCP Pub/Sub Subscriptions.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
public final class PubSubReactiveFactory {

	private static final Log LOGGER = LogFactory.getLog(PubSubReactiveFactory.class);

	private final PubSubSubscriberOperations subscriberOperations;

	private final Scheduler scheduler;

	/**
	 * Instantiate `PubSubReactiveFactory` capable of generating subscription-based streams.
	 * @param subscriberOperations template for interacting with GCP Pub/Sub subscriber operations.
	 * @param scheduler scheduler to use for asynchronously retrieving Pub/Sub messages.
	 */
	public PubSubReactiveFactory(PubSubSubscriberOperations subscriberOperations, Scheduler scheduler) {
		Assert.notNull(subscriberOperations, "subscriberOperations cannot be null.");
		Assert.notNull(scheduler, "scheduler cannot be null.");
		this.subscriberOperations = subscriberOperations;
		this.scheduler = scheduler;
	}

	/**
	 * Create an infinite stream {@link Flux} of {@link AcknowledgeablePubsubMessage} objects.
	 * <p>The {@link Flux} respects backpressure by using of Pub/Sub Synchronous Pull to retrieve
	 * batches of up to the requested number of messages until the full demand is fulfilled
	 * or subscription terminated.
	 * <p>For unlimited demand, the underlying subscription will be polled at a regular interval,
	 * requesting up to {@code Integer.MAX_VALUE} messages at each poll.
	 * <p>For specific demand, as many messages as are available will be returned immediately,
	 * with remaining demand being fulfilled in the future.
	 * Pub/Sub timeout will cause a retry with the same demand.
	 * <p>Any exceptions that are thrown by the Pub/Sub client will be passed as an error to the stream.
	 * The error handling operators, like {@link Flux#retry()},
	 * can be used to recover and continue streaming messages.
	 * @param subscriptionName subscription from which to retrieve messages.
	 * @param pollingPeriodMs how frequently to poll the source subscription in case of unlimited demand, in milliseconds.
	 * @return infinite stream of {@link AcknowledgeablePubsubMessage} objects.
	 */
	public Flux<AcknowledgeablePubsubMessage> poll(String subscriptionName, long pollingPeriodMs) {

		return Flux.create(sink -> {

			Scheduler.Worker subscriptionWorker = this.scheduler.createWorker();

			sink.onRequest((numRequested) -> {
				if (numRequested == Long.MAX_VALUE) {
					// unlimited demand
					subscriptionWorker.schedulePeriodically(
							new NonBlockingUnlimitedDemandPullTask(subscriptionName, sink), 0, pollingPeriodMs, TimeUnit.MILLISECONDS);
				}
				else {
					subscriptionWorker.schedule(new BlockingLimitedDemandPullTask(subscriptionName, numRequested, sink));
				}
			});

			sink.onDispose(subscriptionWorker);

		});
	}

	private abstract class PubSubPullTask implements Runnable {

		protected final String subscriptionName;

		protected final FluxSink<AcknowledgeablePubsubMessage> sink;

		PubSubPullTask(String subscriptionName, FluxSink<AcknowledgeablePubsubMessage> sink) {
			this.subscriptionName = subscriptionName;
			this.sink = sink;
		}

		/**
		 * Retrieve up to a specified number of messages, sending them to the subscription.
		 * @param demand maximum number of messages to retrieve
		 * @param block whether to wait for the first message to become available
		 * @return number of messages retrieved
		 */
		protected int pullToSink(int demand, boolean block) {

			List<AcknowledgeablePubsubMessage> messages =
					PubSubReactiveFactory.this.subscriberOperations.pull(this.subscriptionName, demand, !block);

			if (!this.sink.isCancelled()) {
				messages.forEach(sink::next);
			}

			return messages.size();
		}

	}

	/**
	 * Runnable task issuing blocking Pub/Sub Pull requests until the specified number of
	 * messages has been retrieved.
	 */
	private class BlockingLimitedDemandPullTask extends PubSubPullTask {

		private final long initialDemand;

		BlockingLimitedDemandPullTask(String subscriptionName, long initialDemand, FluxSink<AcknowledgeablePubsubMessage> sink) {
			super(subscriptionName, sink);
			this.initialDemand = initialDemand;
		}

		@Override
		public void run() {
			try {
				long demand = this.initialDemand;
				List<AcknowledgeablePubsubMessage> messages;

				while (demand > 0 && !this.sink.isCancelled()) {
					try {
						int intDemand = demand > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) demand;
						demand -= pullToSink(intDemand, true);
					}
					catch (DeadlineExceededException e) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Blocking pull timed out due to empty subscription "
									+ this.subscriptionName
									+ "; retrying.");
						}
					}
				}
			}
			catch (RuntimeException e) {
				sink.error(e);
			}
		}

	}

	/**
	 * Runnable task issuing a single Pub/Sub Pull request for all available messages.
	 * Terminates immediately if no messages are available.
	 */
	private class NonBlockingUnlimitedDemandPullTask extends PubSubPullTask {

		NonBlockingUnlimitedDemandPullTask(String subscriptionName, FluxSink<AcknowledgeablePubsubMessage> sink) {
			super(subscriptionName, sink);
		}

		@Override
		public void run() {
			try {
				pullToSink(Integer.MAX_VALUE, false);
			}
			catch (RuntimeException e) {
				sink.error(e);
			}
		}

	}

}
