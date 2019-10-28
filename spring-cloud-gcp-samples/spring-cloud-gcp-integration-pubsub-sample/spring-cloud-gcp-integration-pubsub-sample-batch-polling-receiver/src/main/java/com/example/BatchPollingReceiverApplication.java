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

package com.example;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubBatchMessageSource;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;

/**
 * Spring Boot Application demonstrating receiving batch PubSub messages via synchronous pull.
 *
 * @author Lauren Huang
 *
 * @since 1.2
 */
@SpringBootApplication
public class BatchPollingReceiverApplication {

	private static final Log LOGGER = LogFactory.getLog(BatchPollingReceiverApplication.class);


	public static void main(String[] args) {
		SpringApplication.run(BatchPollingReceiverApplication.class, args);
	}

	@Bean
	@InboundChannelAdapter(channel = "pubsubInputChannel", poller = @Poller(fixedDelay = "100"))
	public MessageSource<Object> pubsubAdapter(PubSubTemplate pubSubTemplate) {
		PubSubBatchMessageSource messageSource = new PubSubBatchMessageSource(pubSubTemplate, "exampleSubscription");
		messageSource.setMaxFetchSize(5);
		messageSource.setAckMode(AckMode.MANUAL);
		return messageSource;
	}

	@ServiceActivator(inputChannel = "pubsubInputChannel")
	public void messageReceiver(List<AcknowledgeablePubsubMessage> payload) {
		LOGGER.info("Batch of " + payload.size() + " message(s) arrived by Synchronous Pull!");
		for (AcknowledgeablePubsubMessage message : payload) {
			LOGGER.info("Payload: " + message.getPubsubMessage().getData().toStringUtf8());
			message.ack();
		}
	}

}
