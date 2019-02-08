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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a Flux populated by using Pub/Sub Synchronous Pull.
 *
 * @author Elena Felder
 * @since 1.2
 */
public class FluxSubscriber {

	private static final Log LOGGER = LogFactory.getLog(FluxSubscriber.class);

	public static <T> Flux<ConvertedAcknowledgeablePubsubMessage<T>> createPolledFlux(
			PubSubSubscriberOperations subscriberOperations, Class<T> targetType) {
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		return Flux.create(sink -> {
			sink.onRequest((numRequested) -> {
				// request must be non-blocking.
				executorService.submit(new PubSubPullTask<T>(numRequested, sink, subscriberOperations, targetType));
			});

			sink.onDispose(() -> {
				executorService.shutdown();
			});
		});
	}

	private static class PubSubPullTask<T> implements Runnable {
		private int demand;
		private FluxSink sink;
		private PubSubSubscriberOperations subscriberOperations;
		private Class<T> targetType;

		public PubSubPullTask(long demand, FluxSink<ConvertedAcknowledgeablePubsubMessage<T>> sink, PubSubSubscriberOperations subscriberOperations, Class<T> targetType) {
			this.demand = demand > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) demand;
			this.sink = sink;
			this.subscriberOperations = subscriberOperations;
			this.targetType = targetType;
		}

		@Override
		public void run() {
			long remainingDemand = demand;

			while (remainingDemand > 0) {
				int numToRequest = remainingDemand > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remainingDemand;

				LOGGER.info("  Current remaining demand: " + remainingDemand);
				List<ConvertedAcknowledgeablePubsubMessage<T>> messages
						= this.subscriberOperations.pullAndConvert("reactiveSubscription",
						numToRequest,
						false,
						targetType);

				int numReceived = messages.size();
				messages.forEach(m -> {
					LOGGER.info("Sending message to subscriber: " + m.getPayload());
					sink.next(m);
				});
				remainingDemand -= numReceived;
				LOGGER.info("Got " + numReceived + " messages; remaining demand = " + remainingDemand);

			}
		}
	}

}
