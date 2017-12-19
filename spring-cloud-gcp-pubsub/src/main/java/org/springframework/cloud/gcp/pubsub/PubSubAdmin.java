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

package org.springframework.cloud.gcp.pubsub;

import java.io.IOException;
import java.util.List;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.pubsub.v1.PagedResponseWrappers;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.common.collect.Lists;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.Topic;
import com.google.pubsub.v1.TopicName;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.Assert;

/**
 * Pub/Sub admin utility that creates new topics and subscriptions on Google Cloud Pub/Sub.
 *
 * @author João André Martins
 */
public class PubSubAdmin {

	private final String projectId;

	private final TopicAdminClient topicAdminClient;

	private final SubscriptionAdminClient subscriptionAdminClient;

	/** Default inspired in the subscription creation web UI. */
	private int defaultAckDeadline = 10;

	/**
	 * This constructor instantiates TopicAdminClient and SubscriptionAdminClient with all their
	 * defaults and the provided credentials provider.
	 */
	public PubSubAdmin(GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {
		this(projectIdProvider,
				TopicAdminClient.create(
						TopicAdminSettings.newBuilder()
								.setCredentialsProvider(credentialsProvider)
								.build()),
				SubscriptionAdminClient.create(
						SubscriptionAdminSettings.newBuilder()
						.setCredentialsProvider(credentialsProvider)
						.build()));
	}

	public PubSubAdmin(GcpProjectIdProvider projectIdProvider, TopicAdminClient topicAdminClient,
			SubscriptionAdminClient subscriptionAdminClient) {
		Assert.notNull(projectIdProvider, "The project ID provider can't be null.");
		Assert.notNull(topicAdminClient, "The topic administration client can't be null");
		Assert.notNull(subscriptionAdminClient,
				"The subscription administration client can't be null");

		this.projectId = projectIdProvider.getProjectId();
		Assert.hasText(this.projectId, "The project ID can't be null or empty.");
		this.topicAdminClient = topicAdminClient;
		this.subscriptionAdminClient = subscriptionAdminClient;
	}

	/**
	 * Create a new topic on Google Cloud Pub/Sub.
	 *
	 * @param topicName the name for the new topic
	 * @return the created topic
	 */
	public Topic createTopic(String topicName) {
		Assert.hasText(topicName, "No topic name was specified.");

		return this.topicAdminClient.createTopic(TopicName.create(this.projectId, topicName));
	}

	/**
	 * Get the configuration of a Google Cloud Pub/Sub topic.
	 *
	 * @param topicName canonical topic name, e.g., "topicName"
	 * @return topic configuration or {@code null} if topic doesn't exist
	 */
	public Topic getTopic(String topicName) {
		Assert.hasText(topicName, "No topic name was specified.");

		try {
			return this.topicAdminClient.getTopic(TopicName.create(this.projectId, topicName));
		}
		catch (ApiException aex) {
			if (aex.getStatusCode().getCode() == StatusCode.Code.NOT_FOUND) {
				return null;
			}

			throw aex;
		}
	}

	/**
	 * Delete a topic from Google Cloud Pub/Sub.
	 *
	 * @param topicName the name of the topic to be deleted
	 */
	public void deleteTopic(String topicName) {
		Assert.hasText(topicName, "No topic name was specified.");

		this.topicAdminClient.deleteTopic(TopicName.create(this.projectId, topicName));
	}

	/**
	 * Return every topic in a project.
	 *
	 * <p>If there are multiple pages, they will all be merged into the same result.
	 */
	public List<Topic> listTopics() {
		PagedResponseWrappers.ListTopicsPagedResponse topicListPage =
				this.topicAdminClient.listTopics(ProjectName.create(this.projectId));

		return Lists.newArrayList(topicListPage.iterateAll());
	}

	/**
	 * Create a new subscription on Google Cloud Pub/Sub.
	 *
	 * @param subscriptionName the name of the new subscription
	 * @param topicName the name of the topic being subscribed to
	 * @return the created subscription
	 */
	public Subscription createSubscription(String subscriptionName, String topicName) {
		return createSubscription(subscriptionName, topicName, null, null);
	}

	/**
	 * Create a new subscription on Google Cloud Pub/Sub.
	 *
	 * @param subscriptionName the name of the new subscription
	 * @param topicName the name of the topic being subscribed to
	 * @param ackDeadline deadline in seconds before a message is resent. If not provided, set to
	 *                    default of 10 seconds
	 * @return the created subscription
	 */
	public Subscription createSubscription(String subscriptionName, String topicName,
			Integer ackDeadline) {
		return createSubscription(subscriptionName, topicName, ackDeadline, null);
	}

	/**
	 * Create a new subscription on Google Cloud Pub/Sub.
	 *
	 * @param subscriptionName the name of the new subscription
	 * @param topicName the name of the topic being subscribed to
	 * @param pushEndpoint URL of the service receiving the push messages. If not provided, uses
	 *                     message pulling by default
	 * @return the created subscription
	 */
	public Subscription createSubscription(String subscriptionName, String topicName,
			String pushEndpoint) {
		return createSubscription(subscriptionName, topicName, null, pushEndpoint);
	}

	/**
	 * Create a new subscription on Google Cloud Pub/Sub.
	 *
	 * @param subscriptionName the name of the new subscription
	 * @param topicName the name of the topic being subscribed to
	 * @param ackDeadline deadline in seconds before a message is resent. If not provided, set to
	 *                    default of 10 seconds
	 * @param pushEndpoint URL of the service receiving the push messages. If not provided, uses
	 *                     message pulling by default
	 * @return the created subscription
	 */
	public Subscription createSubscription(String subscriptionName, String topicName,
			Integer ackDeadline, String pushEndpoint) {
		Assert.hasText(subscriptionName, "No subscription name was specified.");
		Assert.hasText(topicName, "No topic name was specified.");

		int finalAckDeadline = this.defaultAckDeadline;
		if (ackDeadline != null) {
			Assert.isTrue(ackDeadline >= 0,
					"The acknowledgement deadline value can't be negative.");
			finalAckDeadline = ackDeadline;
		}

		PushConfig.Builder pushConfigBuilder = PushConfig.newBuilder();
		if (pushEndpoint != null) {
			pushConfigBuilder.setPushEndpoint(pushEndpoint);
		}

		return this.subscriptionAdminClient.createSubscription(
				SubscriptionName.of(this.projectId, subscriptionName),
				TopicName.of(this.projectId, topicName),
				pushConfigBuilder.build(),
				finalAckDeadline);
	}

	/**
	 * Get the configuration of a Google Cloud Pub/Sub subscription.
	 *
	 * @param subscriptionName canonical subscription name, e.g., "subscriptionName"
	 * @return subscription configuration or {@code null} if subscription doesn't exist
	 */
	public Subscription getSubscription(String subscriptionName) {
		Assert.hasText(subscriptionName, "No subscription name was specified");

		try {
			return this.subscriptionAdminClient.getSubscription(
					SubscriptionName.create(this.projectId, subscriptionName));
		}
		catch (ApiException aex) {
			if (aex.getStatusCode().getCode() == StatusCode.Code.NOT_FOUND) {
				return null;
			}

			throw aex;
		}
	}

	/**
	 * Delete a subscription from Google Cloud Pub/Sub.
	 *
	 * @param subscriptionName
	 */
	public void deleteSubscription(String subscriptionName) {
		Assert.hasText(subscriptionName, "No subscription name was specified");

		this.subscriptionAdminClient.deleteSubscription(
				SubscriptionName.of(this.projectId, subscriptionName));
	}

	/**
	 * Return every subscription in a project.
	 *
	 * <p>If there are multiple pages, they will all be merged into the same result.
	 */
	public List<Subscription> listSubscriptions() {
		PagedResponseWrappers.ListSubscriptionsPagedResponse subscriptionsPage =
				this.subscriptionAdminClient.listSubscriptions(ProjectName.of(this.projectId));

		return Lists.newArrayList(subscriptionsPage.iterateAll());
	}

	/**
	 * @return the default acknowledgement deadline value in seconds
	 */
	public int getDefaultAckDeadline() {
		return this.defaultAckDeadline;
	}

	/**
	 * Set the default acknowledgement deadline value.
	 *
	 * @param defaultAckDeadline default acknowledgement deadline value in seconds
	 */
	public void setDefaultAckDeadline(int defaultAckDeadline) {
		Assert.isTrue(defaultAckDeadline >= 0,
				"The acknowledgement deadline value can't be negative.");

		this.defaultAckDeadline = defaultAckDeadline;
	}
}
