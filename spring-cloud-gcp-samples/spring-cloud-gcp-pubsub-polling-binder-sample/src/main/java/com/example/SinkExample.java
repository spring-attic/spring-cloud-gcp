/*
 * Copyright 2017-2018 the original author or authors.
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

package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.binder.PollableMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

/**
 * Example of a sink for the sample app.
 *
 * @author Elena Felder
 */
@EnableBinding(PollableSink.class)
public class SinkExample {

	private static final Log LOGGER = LogFactory.getLog(SinkExample.class);

	@Bean
	public ApplicationRunner poller(PollableMessageSource destIn) {

		PolledMessageHandler messageHandler = new PolledMessageHandler();

		// TODO: use https://github.com/spring-cloud/spring-cloud-stream/pull/1591
		return args -> {
			while (true) {
				try {
					if (!destIn.poll(messageHandler, ParameterizedTypeReference.forType(UserMessage.class))) {
						Thread.sleep(1000);
					}
				}
				catch (Exception e) {
					LOGGER.info("oops", e);
					break;
				}
			}
		};
	}

	static class PolledMessageHandler implements MessageHandler {
		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			AcknowledgeablePubsubMessage ackableMessage = (AcknowledgeablePubsubMessage)message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE);
			ackableMessage.ack();

			UserMessage userMessage = (UserMessage)message.getPayload();
			LOGGER.info("New message received from " + userMessage.getUsername() + ": " + userMessage.getBody() +
					" at " + userMessage.getCreatedAt());
		}
	}

}