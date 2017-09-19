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

package org.springframework.integration.gcp.outbound;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

/**
 * Sends messages to Google Cloud Pub/Sub by delegating to {@link PubSubOperations}.
 *
 * @author João André Martins
 */
public class PubSubMessageHandler extends AbstractMessageHandler {

	private final PubSubOperations pubSubTemplate;

	private String topic;

	public PubSubMessageHandler(PubSubOperations pubSubTemplate, String topic) {
		this.pubSubTemplate = pubSubTemplate;
		this.topic = topic;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		this.pubSubTemplate.publish(this.topic, message);
	}
}
