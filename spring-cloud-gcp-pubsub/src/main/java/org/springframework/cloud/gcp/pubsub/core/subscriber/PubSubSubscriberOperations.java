/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.pubsub.core.subscriber;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An abstraction for Google Cloud Pub/Sub subscription / pulling operations.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 * @author Doug Hoard
 * @author Maurice Zeijen
 *
 * @since 1.1
 */
public interface PubSubSubscriberOperations {

	/**
	 * Subscribe to a subscription with a given message receiver.
	 *
	 * @param messageReceiver the message receiver with which to subscribe
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @return the subscriber
	 * @deprecated as of 1.1, use {@link #subscribe(String, Consumer)} instead.
	 */
	@Deprecated
	Subscriber subscribe(String subscription, MessageReceiver messageReceiver);

	/**
	 * Add a callback method to an existing subscription.
	 * <p>The created {@link Subscriber} is returned so it can be stopped.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param messageConsumer the callback method triggered when new messages arrive
	 * @return subscriber listening to new messages
	 * @since 1.1
	 */
	Subscriber subscribe(String subscription, Consumer<BasicAcknowledgeablePubsubMessage> messageConsumer);

	/**
	 * Add a callback method to an existing subscription that receives Pub/Sub messages converted to the requested
	 * payload type.
	 * <p>The created {@link Subscriber} is returned so it can be stopped.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param messageConsumer the callback method triggered when new messages arrive
	 * @param payloadType the type to which the payload of the Pub/Sub message should be converted
	 * @param <T> the type of the payload
	 * @return subscriber listening to new messages
	 * @since 1.1
	 */
	<T> Subscriber subscribeAndConvert(String subscription,
			Consumer<ConvertedBasicAcknowledgeablePubsubMessage<T>> messageConsumer, Class<T> payloadType);

	/**
	 * Pull and auto-acknowledge a number of messages from a Google Cloud Pub/Sub subscription.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param maxMessages the maximum number of pulled messages. If this value is null then
	 * up to Integer.MAX_VALUE messages will be requested.
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @return the list of received messages
	 */
	List<PubsubMessage> pullAndAck(String subscription, Integer maxMessages, Boolean returnImmediately);

	/**
	 * Asynchronously pull and auto-acknowledge a number of messages from a Google Cloud Pub/Sub subscription.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param maxMessages the maximum number of pulled messages. If this value is null then
	 * up to Integer.MAX_VALUE messages will be requested.
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @return the ListenableFuture for the asynchronous execution, returning the list of
	 * received acknowledgeable messages
	 * @since 1.2.3
	 */
	ListenableFuture<List<PubsubMessage>> pullAndAckAsync(String subscription, Integer maxMessages, Boolean returnImmediately);

	/**
	 * Pull a number of messages from a Google Cloud Pub/Sub subscription.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param maxMessages the maximum number of pulled messages. If this value is null then
	 * up to Integer.MAX_VALUE messages will be requested.
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @return the list of received acknowledgeable messages
	 */
	List<AcknowledgeablePubsubMessage> pull(String subscription, Integer maxMessages, Boolean returnImmediately);

	/**
	 * Asynchronously pull a number of messages from a Google Cloud Pub/Sub subscription.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param maxMessages the maximum number of pulled messages. If this value is null then
	 * up to Integer.MAX_VALUE messages will be requested.
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @return the ListenableFuture for the asynchronous execution, returning the list of
	 * received acknowledgeable messages
	 * @since 1.2.3
	 */
	ListenableFuture<List<AcknowledgeablePubsubMessage>> pullAsync(String subscription, Integer maxMessages, Boolean returnImmediately);

	/**
	 * Pull a number of messages from a Google Cloud Pub/Sub subscription and convert them to Spring messages with
	 * the desired payload type.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param maxMessages the maximum number of pulled messages. If this value is null then
	 * up to Integer.MAX_VALUE messages will be requested.
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @param payloadType the type to which the payload of the Pub/Sub messages should be converted
	 * @param <T> the type of the payload
	 * @return the list of received acknowledgeable messages
	 * @since 1.1
	 */
	<T> List<ConvertedAcknowledgeablePubsubMessage<T>> pullAndConvert(String subscription, Integer maxMessages,
			Boolean returnImmediately, Class<T> payloadType);

	/**
	 * Asynchronously pull a number of messages from a Google Cloud Pub/Sub subscription and convert them to Spring messages with
	 * the desired payload type.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @param maxMessages the maximum number of pulled messages. If this value is null then
	 * up to Integer.MAX_VALUE messages will be requested.
	 * @param returnImmediately returns immediately even if subscription doesn't contain enough
	 * messages to satisfy {@code maxMessages}
	 * @param payloadType the type to which the payload of the Pub/Sub messages should be converted
	 * @param <T> the type of the payload
	 * @return the ListenableFuture for the asynchronous execution, returning the list of
	 *  received acknowledgeable messages
	 * @since 1.2.3
	 */
	<T> ListenableFuture<List<ConvertedAcknowledgeablePubsubMessage<T>>> pullAndConvertAsync(String subscription,
			Integer maxMessages, Boolean returnImmediately, Class<T> payloadType);

	/**
	 * Pull and auto-acknowledge a message from a Google Cloud Pub/Sub subscription.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @return a received message, or {@code null} if none exists in the subscription
	 */
	PubsubMessage pullNext(String subscription);

	/**
	 * Asynchronously pull and auto-acknowledge a message from a Google Cloud Pub/Sub subscription.
	 * @param subscription canonical subscription name, e.g., "subscriptionName", or the fully-qualified
	 * subscription name in the {@code projects/<project_name>/subscriptions/<subscription_name>} format
	 * @return the ListenableFuture for the asynchronous execution, returning a received message,
	 * or {@code null} if none exists in the subscription
	 * @since 1.2.3
	 */
	ListenableFuture<PubsubMessage> pullNextAsync(String subscription);

	/**
	 * Acknowledge a batch of messages. The messages must have the same project id.
	 * @param acknowledgeablePubsubMessages messages to be acknowledged
	 * @return {@code ListenableFuture<Void>} the ListenableFuture for the asynchronous execution
	 */
	ListenableFuture<Void> ack(Collection<? extends AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages);

	/**
	 * Negatively acknowledge a batch of messages. The messages must have the same project id.
	 * @param acknowledgeablePubsubMessages messages to be negatively acknowledged
	 * @return {@code ListenableFuture<Void>} the ListenableFuture for the asynchronous execution
	 */
	ListenableFuture<Void> nack(Collection<? extends AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages);

	/**
	 * Modify the ack deadline of a batch of messages. The messages must have the same project id.
	 * @param acknowledgeablePubsubMessages messages to be modified
	 * @param ackDeadlineSeconds the new ack deadline in seconds. A deadline of 0 effectively nacks the messages.
	 * @return {@code ListenableFuture<Void>} the ListenableFuture for the asynchronous execution
	 * @since 1.1
	 */
	ListenableFuture<Void> modifyAckDeadline(
			Collection<? extends AcknowledgeablePubsubMessage> acknowledgeablePubsubMessages,
			int ackDeadlineSeconds);

}
