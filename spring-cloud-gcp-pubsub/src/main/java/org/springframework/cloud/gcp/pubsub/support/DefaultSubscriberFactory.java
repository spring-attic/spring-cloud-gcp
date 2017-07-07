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

package org.springframework.cloud.gcp.pubsub.support;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.ChannelProvider;
import com.google.api.gax.grpc.ExecutorProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.SubscriptionName;

/**
 *
 * The default {@link SubscriberFactory} implementation.
 *
 * @author João André Martins
 */
public class DefaultSubscriberFactory implements SubscriberFactory {

	private String projectId;

	private final ExecutorProvider executorProvider;

	private final ChannelProvider channelProvider;

	private final CredentialsProvider credentialsProvider;

	public DefaultSubscriberFactory(String projectId, ExecutorProvider executorProvider,
			ChannelProvider channelProvider, CredentialsProvider credentialsProvider) {
		this.projectId = projectId;
		this.executorProvider = executorProvider;
		this.channelProvider = channelProvider;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public Subscriber getSubscriber(String subscriptionName, MessageReceiver receiver) {
		return Subscriber.defaultBuilder(
				SubscriptionName.create(this.projectId, subscriptionName), receiver)
				.setChannelProvider(this.channelProvider)
				.setExecutorProvider(this.executorProvider)
				.setCredentialsProvider(this.credentialsProvider)
				.build();
	}
}
