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
		return subscriptionAdminClient;
	}

	public PubSubSupport setSubscriptionAdminClient(SubscriptionAdminClient subscriptionAdminClient) {
		this.subscriptionAdminClient = subscriptionAdminClient;
		return this;
	}

	public TopicAdminClient getTopicAdminClient() {
		return topicAdminClient;
	}

	public PubSubSupport setTopicAdminClient(TopicAdminClient topicAdminClient) {
		this.topicAdminClient = topicAdminClient;
		return this;
	}

	public CredentialsProvider getCredentialsProvider() {
		return credentialsProvider;
	}

	public PubSubSupport setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
		return this;
	}

	public ChannelProvider getChannelProvider() {
		return channelProvider;
	}

	public PubSubSupport setChannelProvider(ChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
		return this;
	}
}
