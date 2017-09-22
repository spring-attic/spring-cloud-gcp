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

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.gcp.AckMode;
import org.springframework.integration.gcp.inbound.PubSubInboundChannelAdapter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

	private static final Log LOGGER = LogFactory.getLog(WebController.class);

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

	@Bean
	@ServiceActivator(inputChannel = "pubsubInputChannel")
	public MessageHandler messageReceiver() {
		return message -> {
			LOGGER.info("Message arrived! Payload: " + message.getPayload());
			AckReplyConsumer consumer =
					(AckReplyConsumer) message.getHeaders().get(GcpHeaders.ACKNOWLEDGEMENT);
			consumer.ack();
		};
	}
}
