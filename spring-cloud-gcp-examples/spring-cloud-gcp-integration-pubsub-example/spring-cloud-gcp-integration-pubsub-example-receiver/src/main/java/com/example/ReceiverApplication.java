/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.example;

import java.io.IOException;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.gcp.pubsub.AckMode;
import org.springframework.integration.gcp.pubsub.inbound.PubSubInboundChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.handler.annotation.Header;

@SpringBootApplication
public class ReceiverApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(ReceiverApplication.class, args);
	}

	private static final Log LOGGER = LogFactory.getLog(ReceiverApplication.class);

	@Bean
	public MessageChannel pubsubInputChannel() {
		return new DirectChannel();
	}

	@Bean
	public PubSubInboundChannelAdapter messageChannelAdapter(
			@Qualifier("pubsubInputChannel") MessageChannel inputChannel,
			PubSubOperations pubSubTemplate) {
		PubSubInboundChannelAdapter adapter =
				new PubSubInboundChannelAdapter(pubSubTemplate, "exampleSubscription");
		adapter.setOutputChannel(inputChannel);
		adapter.setAckMode(AckMode.MANUAL);

		return adapter;
	}

	@ServiceActivator(inputChannel = "pubsubInputChannel")
	public void messageReceiver(String payload,
			@Header(GcpHeaders.ACKNOWLEDGEMENT) AckReplyConsumer ackReplyConsumer) {
		LOGGER.info("Message arrived! Payload: " + payload);
		ackReplyConsumer.ack();
	}
}
