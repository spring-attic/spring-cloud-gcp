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
import java.util.stream.Collectors;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherOperations;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.PubSubAcknowledger;
import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.SimplePubSubMessageConverter;
import org.springframework.util.Assert;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SettableListenableFuture;

/**
 * Default implementation of {@link PubSubOperations}.
 *
 * <p>The main Google Cloud Pub/Sub integration component for publishing to topics and consuming
 * messages from subscriptions asynchronously or by pulling.
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class PubSubTemplate implements PubSubOperations {

	private PubSubPublisherOperations pubSubPublisherOperations;
	private PubSubSubscriberOperations pubSubSubscriberOperations;

	/**
	 * Default {@link PubSubTemplate} constructor that uses {@link SimplePubSubMessageConverter}
	 * to serialize and deserialize payloads.
	 * @param publisherFactory the {@link com.google.cloud.pubsub.v1.Publisher} factory to
	 * publish to topics
	 * @param subscriberFactory the {@link com.google.cloud.pubsub.v1.Subscriber} factory
	 * to subscribe to subscriptions
	 */
	public PubSubTemplate(PublisherFactory publisherFactory,
			SubscriberFactory subscriberFactory) {
		this.publisherFactory = publisherFactory;
		this.subscriberFactory = subscriberFactory;
		this.subscriberStub = this.subscriberFactory.createSubscriberStub();
		this.acknowledger = this.subscriberFactory.createAcknowledger();
	}



}
