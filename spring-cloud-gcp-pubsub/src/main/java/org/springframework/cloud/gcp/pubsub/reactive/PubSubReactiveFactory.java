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
import java.util.function.Function;

import com.google.api.gax.rpc.DeadlineExceededException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private static final Log LOGGER = LogFactory.getLog(PubSubReactiveFactory.class);

	private PubSubReactiveFactory() { }

	public static <T> Publisher<AcknowledgeablePubsubMessage> createPolledPublisher(
			String subscriptionName, PubSubSubscriberOperations subscriberOperations) {

		return Flux.<List<AcknowledgeablePubsubMessage>>create(sink -> {

			sink.onRequest((numRequested) -> {
				int demand = numRequested > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) numRequested;
				produceMessages(subscriptionName, demand, sink, subscriberOperations);
			});

		}).flatMapIterable(Function.identity());
	}

	private static void produceMessages(String subscriptionName, int demand, FluxSink<List<AcknowledgeablePubsubMessage>> sink, PubSubSubscriberOperations subscriberOperations) {

		subscriberOperations.pullAsync(subscriptionName, demand, false).addCallback(
				acknowledgeablePubsubMessages -> {

					sink.next(acknowledgeablePubsubMessages);

					int remainingDemand = demand - acknowledgeablePubsubMessages.size();

					if (remainingDemand > 0) {
						// Ensure requested messages get fulfilled eventually.
						produceMessages(subscriptionName, remainingDemand, sink, subscriberOperations);
					}

				}, throwable -> {
					if (throwable instanceof DeadlineExceededException)  {
						LOGGER.warn("Pub/Sub deadline exceeded; retrying.");
						produceMessages(subscriptionName, demand, sink, subscriberOperations);
					}
					else {
						sink.error(throwable);
					}
				}
		);
	}

}
