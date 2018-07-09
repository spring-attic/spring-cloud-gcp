/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.pubsub.core.publisher;

import java.util.Map;

import com.google.pubsub.v1.PubsubMessage;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * An abstraction for Google Cloud Pub/Sub publisher operations.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Doug Hoard
 *
 * @since 1.1
 */
public interface PubSubPublisherOperations {

	/**
	 * Send a message to Pub/Sub.
	 * @param topic   the name of an existing topic
	 * @param payload an object that will be serialized and sent
	 * @return the listenable future of the call
	 */
	<T> ListenableFuture<String> publish(String topic, T payload, Map<String, String> headers);

	/**
	 * Send a message to Pub/Sub.
	 * @param topic   the name of an existing topic
	 * @param payload an object that will be serialized and sent
	 * @return the listenable future of the call
	 */
	<T> ListenableFuture<String> publish(String topic, T payload);

	/**
	 * Send a message to Pub/Sub.
	 * @param topic         the name of an existing topic
	 * @param pubsubMessage a Google Cloud Pub/Sub API message
	 * @return the listenable future of the call
	 */
	ListenableFuture<String> publish(String topic, PubsubMessage pubsubMessage);

}
