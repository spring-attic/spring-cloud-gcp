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

package org.springframework.cloud.gcp.pubsub.core;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;

import org.springframework.messaging.Message;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An abstraction for Google Cloud Pub/Sub.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 */
public interface PubSubOperations {

	/**
	 * Sends a Spring {@link Message} to Pub/Sub.
	 *
	 * @param topic the name of an existing topic
	 * @param message the Spring message
	 * @return the listenable future of the call
	 */
	ListenableFuture<String> publish(String topic, Message message);

	/**
	 * Adds a callback method to an existing subscription.
	 *
	 * <p>
	 * The created {@link Subscriber} is returned so it can be stopped.
	 *
	 * @param subscription the name of an existing subscription
	 * @param messageHandler the callback method triggered when new messages arrive
	 * @return subscriber listening to new messages
	 */
	Subscriber subscribe(String subscription, MessageReceiver messageHandler);

}
