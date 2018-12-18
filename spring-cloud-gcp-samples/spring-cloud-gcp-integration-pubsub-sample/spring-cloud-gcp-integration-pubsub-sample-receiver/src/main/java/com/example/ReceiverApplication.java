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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubMessageSource;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;

/**
 *
 * Spring Boot Application demonstrating two ways to receive PubSub messages.
 *
 * <p>Accepts the following arguments:
 * <p>--syncPull: Enables synchronous, one-message-at-a-time message retrieval. Default behavior is asynchronous
 * <p>--delay: Slows down acknowledgement of each message by a second, allowing better demonstration of load
 *     balancing behavior of Pull vs. Asynchronous Pull strategies.
 * <p>Example: mvn spring-boot:run -Dspring-boot.run.arguments=--delay,--syncPull
 *
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Dmitry Solomakha
 * @author Elena Felder
 *
 * @since 1.1
 */
@SpringBootApplication
public class ReceiverApplication {

	private static final Log LOGGER = LogFactory.getLog(ReceiverApplication.class);

	private static boolean delayAcknoledgement;

	public static void main(String[] args) {
		Set<String> argSet = Arrays.stream(args).map(String::toLowerCase).collect(Collectors.toSet());
		delayAcknoledgement = argSet.contains("--delay");

		LOGGER.info("Starting receiver with " + (delayAcknoledgement ? "delay" : "no delay"));
		SpringApplication.run(ReceiverApplication.class, args);
	}

	@ConditionalOnProperty(value = "syncpull", havingValue = "false", matchIfMissing = true)
	static class StreamingPull {
		@Bean
		public MessageChannel pubsubInputChannel() {
			return new DirectChannel();
		}

		@Bean
		public PubSubInboundChannelAdapter messageChannelAdapter(
				@Qualifier("pubsubInputChannel") MessageChannel inputChannel,
				PubSubTemplate pubSubTemplate) {
			PubSubInboundChannelAdapter adapter =
					new PubSubInboundChannelAdapter(pubSubTemplate, "exampleSubscription");
			adapter.setOutputChannel(inputChannel);
			adapter.setAckMode(AckMode.MANUAL);
			adapter.setPayloadType(String.class);
			return adapter;
		}

		@ServiceActivator(inputChannel = "pubsubInputChannel")
		public void messageReceiver(String payload,
				@Header(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message)
					throws InterruptedException {
			LOGGER.info("Message arrived! Payload: " + payload);
			if (delayAcknoledgement) {
				Thread.sleep(1000);
			}
			message.ack();
		}
	}

	@ConditionalOnProperty("syncpull")
	static class SyncPull {

		@Bean
		@InboundChannelAdapter(channel = "pubsubInputChannel", poller = @Poller(fixedDelay = "100"))
		public MessageSource<Object> pubsubAdapter(PubSubTemplate pubSubTemplate) {
			PubSubMessageSource messageSource = new PubSubMessageSource(pubSubTemplate,  "exampleSubscription");
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

}
