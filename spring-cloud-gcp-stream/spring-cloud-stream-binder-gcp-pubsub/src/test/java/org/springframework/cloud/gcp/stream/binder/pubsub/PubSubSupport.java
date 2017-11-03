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

package org.springframework.cloud.gcp.stream.binder.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.ChannelProvider;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient;

/**
 * @author Andreas Berger
 */
public class PubSubSupport {

	private SubscriptionAdminClient subscriptionAdminClient;
	private TopicAdminClient topicAdminClient;
	private CredentialsProvider credentialsProvider;
	private ChannelProvider channelProvider;

	public SubscriptionAdminClient getSubscriptionAdminClient() {
		return this.subscriptionAdminClient;
	}

	public PubSubSupport setSubscriptionAdminClient(SubscriptionAdminClient subscriptionAdminClient) {
		this.subscriptionAdminClient = subscriptionAdminClient;
		return this;
	}

	public TopicAdminClient getTopicAdminClient() {
		return this.topicAdminClient;
	}

	public PubSubSupport setTopicAdminClient(TopicAdminClient topicAdminClient) {
		this.topicAdminClient = topicAdminClient;
		return this;
	}

	public CredentialsProvider getCredentialsProvider() {
		return this.credentialsProvider;
	}

	public PubSubSupport setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
		return this;
	}

	public ChannelProvider getChannelProvider() {
		return this.channelProvider;
	}

	public PubSubSupport setChannelProvider(ChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
		return this;
	}
}
