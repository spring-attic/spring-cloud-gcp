/*
 * Copyright 2017-2020 the original author or authors.
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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.api.gax.rpc.DeadlineExceededException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.util.Assert;

/**
 * A factory for procuring {@link Flux} instances backed by GCP Pub/Sub Subscriptions.
 *
 * The {@link Scheduler}, that is given to the constructor,
 * is used for regularly polling the subscription,
 * when the demand is unlimited.
 * The scheduler is not used when there is a specific demand (a.k.a backpressure).
 *
 * @author Elena Felder
 * @author Maurice Zeijen
 *
 * @since 1.2
 */
public final class PubSubReactiveFactory {

	private static final Log LOGGER = LogFactory.getLog(PubSubReactiveFactory.class);

	private final PubSubSubscriberOperations subscriberOperations;

	private final Scheduler scheduler;

	private final int maxMessages;

	/**
	 * Instantiate `PubSubReactiveFactory` capable of generating subscription-based streams.
	 * <p>{@code maxMessages} is set to {@code Integer.MAX_VALUE}.</p>
	 * @param subscriberOperations template for interacting with GCP Pub/Sub subscriber operations.
	 * @param scheduler scheduler to use for asynchronously retrieving Pub/Sub messages.
	 */
	public PubSubReactiveFactory(PubSubSubscriberOperations subscriberOperations, Scheduler scheduler) {
		this(subscriberOperations, scheduler, Integer.MAX_VALUE);
	}

	/**
	 * Instantiate `PubSubReactiveFactory` capable of generating subscription-based streams.
	 * @param subscriberOperations template for interacting with GCP Pub/Sub subscriber operations.
	 * @param scheduler scheduler to use for asynchronously retrieving Pub/Sub messages.
	 * @param maxMessages max number of messages that may be pulled from the source
	 * subscription in case of unlimited demand.
	 */
	public PubSubReactiveFactory(PubSubSubscriberOperations subscriberOperations,
			Scheduler scheduler, int maxMessages) {
		Assert.notNull(subscriberOperations, "subscriberOperations cannot be null.");
		Assert.notNull(scheduler, "scheduler cannot be null.");
		if (maxMessages < 1) {
			throw new IllegalArgumentException("maxMessages cannot be less than 1.");
		}
		this.subscriberOperations = subscriberOperations;
		this.scheduler = scheduler;
		this.maxMessages = maxMessages;
	}

	/**
	 * Create an infinite stream {@link Flux} of {@link AcknowledgeablePubsubMessage} objects.
	 * <p>The {@link Flux} respects backpressure by using of Pub/Sub Synchronous Pull to retrieve
	 * batches of up to the requested number of messages until the full demand is fulfilled
	 * or subscription terminated.
	 * <p>For unlimited demand, the underlying subscription will be polled at a regular interval,
	 * requesting up to {@code maxMessages} messages at each poll.
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
			sink.onRequest((numRequested) -> {
				if (numRequested == Long.MAX_VALUE) {
					pollingPull(subscriptionName, pollingPeriodMs, sink);
				}
				else {
					backpressurePull(subscriptionName, numRequested, sink);
				}
			});
		});
	}

	private void pollingPull(String subscriptionName, long pollingPeriodMs,
			FluxSink<AcknowledgeablePubsubMessage> sink) {
		Disposable disposable = Flux
				.interval(Duration.ZERO, Duration.ofMillis(pollingPeriodMs), scheduler)
				.flatMap(ignore -> pullAll(subscriptionName))
				.subscribe(sink::next, sink::error);

		sink.onDispose(disposable);
	}

	private Flux<AcknowledgeablePubsubMessage> pullAll(String subscriptionName) {
		CompletableFuture<List<AcknowledgeablePubsubMessage>> pullResponseFuture = this.subscriberOperations
				.pullAsync(subscriptionName, maxMessages, true).completable();

		return Mono.fromFuture(pullResponseFuture).flatMapMany(Flux::fromIterable);
	}

	private void backpressurePull(String subscriptionName, long numRequested,
			FluxSink<AcknowledgeablePubsubMessage> sink) {
		int intDemand = numRequested > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) numRequested;
		this.subscriberOperations.pullAsync(subscriptionName, intDemand, false).addCallback(
				messages -> {
					if (!sink.isCancelled()) {
						messages.forEach(sink::next);
					}
					if (!sink.isCancelled()) {
						long numToPull = numRequested - messages.size();
						if (numToPull > 0) {
							backpressurePull(subscriptionName, numToPull, sink);
						}
					}
				},
				exception -> {
					if (exception instanceof DeadlineExceededException) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("Blocking pull timed out due to empty subscription "
									+ subscriptionName
									+ "; retrying.");
						}
						backpressurePull(subscriptionName, numRequested, sink);
					}
					else {
						sink.error(exception);
					}
				});

	}
}
