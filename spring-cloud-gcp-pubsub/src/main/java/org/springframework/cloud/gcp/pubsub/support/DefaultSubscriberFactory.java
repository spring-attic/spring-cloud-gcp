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

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.stub.GrpcSubscriberStub;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.SubscriptionName;

import org.threeten.bp.Duration;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.Assert;

/**
 *
 * The default {@link SubscriberFactory} implementation.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class DefaultSubscriberFactory implements SubscriberFactory {

	private String projectId;

	private final ExecutorProvider executorProvider;

	private final TransportChannelProvider channelProvider;

	private final CredentialsProvider credentialsProvider;

	private Long pullTimeoutMillis;

	public DefaultSubscriberFactory(GcpProjectIdProvider projectIdProvider,
			ExecutorProvider executorProvider,
			TransportChannelProvider channelProvider,
			CredentialsProvider credentialsProvider) {
		Assert.notNull(projectIdProvider, "The project ID provider can't be null.");
		Assert.notNull(executorProvider, "The executor provider can't be null.");
		Assert.notNull(channelProvider, "The channel provider can't be null.");
		Assert.notNull(credentialsProvider, "The credentials provider can't be null.");

		this.projectId = projectIdProvider.getProjectId();
		Assert.hasText(this.projectId, "The project ID can't be null or empty.");
		this.executorProvider = executorProvider;
		this.channelProvider = channelProvider;
		this.credentialsProvider = credentialsProvider;
		this.pullTimeoutMillis = pullTimeoutMillis;
	}

	/**
	 * Enables simple a timeout for subscription pull requests.
	 *
	 * @param pullTimeoutMillis Timeout in milliseconds for subscription pull requests. If
	 * set, disables pull retry logic and uses the simple timeout. If set to null, default
	 * pull behavior with retries will be used.
	 */
	public void setPullTimeoutMillis(Long pullTimeoutMillis) {
		this.pullTimeoutMillis = pullTimeoutMillis;
	}

	@Override
	public Subscriber createSubscriber(String subscriptionName, MessageReceiver receiver) {
		return Subscriber.newBuilder(
				SubscriptionName.of(this.projectId, subscriptionName), receiver)
				.setChannelProvider(this.channelProvider)
				.setExecutorProvider(this.executorProvider)
				.setCredentialsProvider(this.credentialsProvider)
				.build();
	}

	@Override
	public PullRequest createPullRequest(String subscriptionName, Integer maxMessages,
			Boolean returnImmediately) {
		PullRequest.Builder pullRequestBuilder =
				PullRequest.newBuilder().setSubscriptionWithSubscriptionName(
						SubscriptionName.of(this.projectId, subscriptionName));

		if (maxMessages != null) {
			pullRequestBuilder.setMaxMessages(maxMessages);
		}

		if (returnImmediately != null) {
			pullRequestBuilder.setReturnImmediately(returnImmediately);
		}

		return pullRequestBuilder.build();
	}

	@Override
	public SubscriberStub createSubscriberStub() {
		try {
			SubscriptionAdminSettings.Builder subscriptionAdminSettingsBuilder = SubscriptionAdminSettings.newBuilder();
			if (this.pullTimeoutMillis != null) {
				subscriptionAdminSettingsBuilder.pullSettings()
						.setSimpleTimeoutNoRetries(Duration.ofSeconds(this.pullTimeoutMillis));
			}

			return GrpcSubscriberStub.create(subscriptionAdminSettingsBuilder.build());
		}
		catch (IOException e) {
			throw new RuntimeException("Error creating default SubscriptionAdminSettings", e);
		}
	}
}
