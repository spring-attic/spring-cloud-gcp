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
 *
 */

package org.springframework.integration.gcp.inbound;

import java.util.HashMap;
import java.util.Map;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.spi.v1.AckReplyConsumer;
import com.google.cloud.pubsub.spi.v1.Subscriber;
import com.google.cloud.pubsub.spi.v1.SubscriptionAdminSettings;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.SubscriptionName;

import org.springframework.cloud.gcp.pubsub.support.GcpHeaders;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.MessageHeaders;

/**
 * Converts from GCP Pub/Sub message to Spring message and sends the Spring message to the
 * attached channels.
 *
 * @author João André Martins
 */
public class PubSubInboundChannelAdapter extends MessageProducerSupport {

	private String projectId;
	private String subscriptionName;
	private GoogleCredentials credentials;
	private Subscriber subscriber;

	public PubSubInboundChannelAdapter(String projectId, String subscriptionName,
									   GoogleCredentials credentials) {
		this.projectId = projectId;
		this.subscriptionName = subscriptionName;
		this.credentials = credentials;
	}

	@Override
	protected void doStart() {
		super.doStart();

		// TODO(joaomartins): Allow user to pass in receiveMessage.
		subscriber = Subscriber
				.defaultBuilder(SubscriptionName.create(projectId, this.subscriptionName),
						this::receiveMessage)
				.setChannelProvider(
						SubscriptionAdminSettings.defaultChannelProviderBuilder()
								.setCredentialsProvider(() -> credentials)
								.build())
				.build();
		subscriber.startAsync();
	}

	private void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
		Map<String, Object> messageHeaders = new HashMap<>();

		message.getAttributesMap().forEach(messageHeaders::put);
		// Send the consumer downstream so user decides on when to ack/nack.
		messageHeaders.put(GcpHeaders.ACKNOWLEDGEMENT, consumer);

		sendMessage(getMessagingTemplate().getMessageConverter()
				.toMessage(message.getData(), new MessageHeaders(messageHeaders)));
	}

	@Override
	protected void doStop() {
		if (subscriber != null) {
			subscriber.stopAsync();
		}

		super.doStop();
	}
}
