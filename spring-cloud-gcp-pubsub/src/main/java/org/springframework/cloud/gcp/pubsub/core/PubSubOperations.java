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
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * @author Vinicius Carvalho
 */
public interface PubSubOperations {

	Publisher getPublisher(String topic);

	String send(String topic, Message message);

	String send(String topic, Object payload);

	String send(String topic, Object payload, MessageHeaders headers);

	ListenableFuture<String> sendAsync(String topic, Message message);

	ListenableFuture<String> sendAsync(String topic, Object payload);

	ListenableFuture<String> sendAsync(String topic, Object payload, MessageHeaders headers);

	Message<String> pull(String subscription, boolean returnImmediately);

	<T> Message<T> pull(String subscrption, Class<T> clazz, boolean returnImmediately);

	Subscriber subscribe(String subscription, MessageReceiver messageReceiver);

}
