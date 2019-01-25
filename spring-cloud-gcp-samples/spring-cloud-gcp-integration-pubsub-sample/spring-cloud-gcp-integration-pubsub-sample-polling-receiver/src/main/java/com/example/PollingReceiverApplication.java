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

package com.example;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubMessageSource;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.handler.annotation.Header;

/**
 * Spring Boot Application demonstrating receiving PubSub messages via synchronous pull.
 *
 * Accepts an argument {@code --delay} to slow down acknowledgement of each message by a
 * second, allowing better demonstration of load balancing behavior.
 * <p>Example: mvn spring-boot:run -Dspring-boot.run.arguments=--delay
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
@SpringBootApplication
public class PollingReceiverApplication {

	private static final Log LOGGER = LogFactory.getLog(PollingReceiverApplication.class);

	private static boolean delayAcknoledgement;

	public static void main(String[] args) {
		Set<String> argSet = Arrays.stream(args).map(String::toLowerCase).collect(Collectors.toSet());
		delayAcknoledgement = argSet.contains("--delay");

		LOGGER.info("Starting receiver with " + (delayAcknoledgement ? "delay" : "no delay"));
		SpringApplication.run(PollingReceiverApplication.class, args);
	}

	@Bean
	@InboundChannelAdapter(channel = "pubsubInputChannel", poller = @Poller(fixedDelay = "100"))
	public MessageSource<Object> pubsubAdapter(PubSubTemplate pubSubTemplate) {
		PubSubMessageSource messageSource = new PubSubMessageSource(pubSubTemplate,  "exampleSubscription");
		messageSource.setMaxFetchSize(5);
		messageSource.setAckMode(AckMode.MANUAL);
		messageSource.setPayloadType(String.class);
		return messageSource;
	}


	@ServiceActivator(inputChannel = "pubsubInputChannel")
	public void messageReceiver(String payload,
			@Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message)
				throws InterruptedException {
		LOGGER.info("Message arrived by Synchronous Pull! Payload: " + payload);
		if (delayAcknoledgement) {
			Thread.sleep(1000);
		}
		message.ack();
	}

}
