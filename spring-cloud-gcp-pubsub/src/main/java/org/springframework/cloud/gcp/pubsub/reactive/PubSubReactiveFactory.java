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

import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * @since 1.2
 */
public final class PubSubReactiveFactory {

	private static final Log LOGGER = LogFactory.getLog(PubSubReactiveFactory.class);

	private PubSubReactiveFactory() { }

	public static <T> Publisher<AcknowledgeablePubsubMessage> createPolledPublisher(
			String subscriptionName, PubSubSubscriberOperations subscriberOperations, Class<T> targetType) {
		ExecutorService executorService = Executors.newSingleThreadExecutor();

		return Flux.create(sink -> {
			sink.onRequest((numRequested) -> {
				// request must be non-blocking.
				executorService.submit(new PubSubPullTask(subscriptionName, numRequested, sink, subscriberOperations));
			});

			sink.onDispose(() -> {
				executorService.shutdown();
			});
		});
	}

	private static class PubSubPullTask implements Runnable {
		private int demand;
		private FluxSink sink;
		private PubSubSubscriberOperations subscriberOperations;
		private String subscriptionName;

		PubSubPullTask(String subscriptionName, long demand, FluxSink<AcknowledgeablePubsubMessage> sink, PubSubSubscriberOperations subscriberOperations) {
			this.subscriptionName = subscriptionName;
			this.demand = demand > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) demand;
			this.sink = sink;
			this.subscriberOperations = subscriberOperations;
		}

		@Override
		public void run() {
			long remainingDemand = demand;

			while (remainingDemand > 0) {
				int numToRequest = remainingDemand > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) remainingDemand;

				LOGGER.info("  Current remaining demand: " + remainingDemand);
				List<AcknowledgeablePubsubMessage> messages
						= this.subscriberOperations.pull(this.subscriptionName, numToRequest, false);

				int numReceived = messages.size();
				messages.forEach(m -> {
					LOGGER.info("Sending message to subscriber: "
							+ new String(m.getPubsubMessage().getData().toByteArray(), Charset.defaultCharset()));
					sink.next(m);
				});
				remainingDemand -= numReceived;
				LOGGER.info("Got " + numReceived + " messages; remaining demand = " + remainingDemand);

			}
		}
	}

}
