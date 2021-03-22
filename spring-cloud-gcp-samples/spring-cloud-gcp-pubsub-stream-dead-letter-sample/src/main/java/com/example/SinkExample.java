/*
 * Copyright 2017-2018 the original author or authors.
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

package com.example;

import java.util.function.Consumer;

import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;

/**
 * Example of a sink for the sample app.
 *
 * @author Travis Tomsu
 */
@Configuration
public class SinkExample {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Bean
	public Consumer<Message<UserMessage>> logUserMessage() {
		return message -> {
			UserMessage userMessage = message.getPayload();
			BasicAcknowledgeablePubsubMessage nackable = GcpPubSubHeaders.getOriginalMessage(message)
					.orElseThrow(() -> new IllegalStateException("Could not find original PubSubMessage."));
			Integer deliveryAttempt = Subscriber.getDeliveryAttempt(nackable.getPubsubMessage());

			// Typically you wouldn't nack() every message, but this demonstrates the Pub/Sub system retrying delivery
			// some number of times before the message is routed to the dead letter queue.
			log.info("Nacking message (attempt {}) from {} at {}: {}", deliveryAttempt, userMessage.getUsername(),
					userMessage.getCreatedAt(), userMessage.getBody());
			nackable.nack();
		};
	}

	@Bean
	public Consumer<UserMessage> deadLetterMessages() {
		return userMessage -> log.info("Received message on dead letter topic from {}: {}", userMessage.getUsername(),
				userMessage.getBody());
	}
}
