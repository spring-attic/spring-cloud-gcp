/*
 *  Copyright 2017-2018 original author or authors.
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

import java.util.List;
import java.util.Map;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An abstraction for Google Cloud Pub/Sub.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public interface PubSubOperations {

	/**
	 * Send a message to Pub/Sub.
	 * @param topic the name of an existing topic
	 * @param payload an object that will be serialized and sent
	 * @return the listenable future of the call
	 */
	<T> ListenableFuture<String> publish(String topic, T payload,
			Map<String, String> headers);

	/**
	 * Send a message to Pub/Sub.
	 * @param topic the name of an existing topic
	 * @param payload an object that will be serialized and sent
	 * @return the listenable future of the call
	 */
	<T> ListenableFuture<String> publish(String topic, T payload);

	/**
	 * Send a message to Pub/Sub.
	 * @param topic the name of an existing topic
	 * @param pubsubMessage a Google Cloud Pub/Sub API message
	 * @return the listenable future of the call
	 */
	ListenableFuture<String> publish(String topic, PubsubMessage pubsubMessage);

	/**
	 * Add a callback method to an existing subscription.
	 *
	 * <p>The created {@link Subscriber} is returned so it can be stopped.
	 * @param subscription the name of an existing subscription
	 * @param messageHandler the callback method triggered when new messages arrive
	 * @return subscriber listening to new messages
	 */
	Subscriber subscribe(String subscription, MessageReceiver messageHandler);

	/**
	 * Pull and auto-acknowledge a number of messages from a Google Cloud Pub/Sub subscription.
	 * @param subscription the subscription name
	 * @param maxMessages the maximum number of pulled messages
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @return the list of received messages
	 */
	List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages,
			Boolean returnImmediately);

	/**
	 * Pull a number of messages from a Google Cloud Pub/Sub subscription.
	 * @param subscription the subscription name
	 * @param maxMessages the maximum number of pulled messages
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @return the list of received acknowledgeable messages
	 */
	List<AcknowledgeablePubsubMessage> pull(String subscription, Integer maxMessages,
			Boolean returnImmediately);

	/**
	 * Pull and auto-acknowledge a message from a Google Cloud Pub/Sub subscription.
	 * @param subscription the subscription name
	 * @return a received message, or {@code null} if none exists in the subscription
	 */
	PubsubMessage pullNext(String subscription);
}
