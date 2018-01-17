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

package org.springframework.cloud.gcp.pubsub.support;

import java.io.IOException;

import com.google.api.core.ApiClock;
import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.HeaderProvider;
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

	private final String projectId;

	private ExecutorProvider executorProvider;

	private TransportChannelProvider channelProvider;

	private CredentialsProvider credentialsProvider;

	private HeaderProvider headerProvider;

	private ExecutorProvider systemExecutorProvider;

	private FlowControlSettings flowControlSettings;

	private Duration maxAckDurationPeriod;

	private Integer parallelPullCount;

	private String pullEndpoint;

	private ApiClock apiClock;

	/**
	 * Default {@link DefaultSubscriberFactory} constructor.
	 *
	 * @param projectIdProvider provides the GCP project ID
	 */
	public DefaultSubscriberFactory(GcpProjectIdProvider projectIdProvider) {
		Assert.notNull(projectIdProvider, "The project ID provider can't be null.");

		this.projectId = projectIdProvider.getProjectId();
		Assert.hasText(this.projectId, "The project ID can't be null or empty.");
	}

	/**
	 * Set the provider for the subscribers' executor. Useful to specify the number of threads to be used by each
	 * executor.
	 */
	public void setExecutorProvider(ExecutorProvider executorProvider) {
		this.executorProvider = executorProvider;
	}

	/**
	 * Set the provider for the subscribers' transport channel.
	 */
	public void setChannelProvider(TransportChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
	}

	/**
	 * Set the provider for the GCP credentials to be used by the subscribers' API calls.
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Set the provider for the HTTP headers to be added to the subscribers' REST API calls.
	 */
	public void setHeaderProvider(HeaderProvider headerProvider) {
		this.headerProvider = headerProvider;
	}

	/**
	 * Set the provider for the system executor, to poll and manage lease extensions.
	 */
	public void setSystemExecutorProvider(ExecutorProvider systemExecutorProvider) {
		this.systemExecutorProvider = systemExecutorProvider;
	}

	/**
	 * Set the flow control for the subscribers, including the behaviour for when the flow limits are hit.
	 */
	public void setFlowControlSettings(FlowControlSettings flowControlSettings) {
		this.flowControlSettings = flowControlSettings;
	}

	/**
	 * Set the maximum period the ack timeout is extended by.
	 */
	public void setMaxAckDurationPeriod(Duration maxAckDurationPeriod) {
		this.maxAckDurationPeriod = maxAckDurationPeriod;
	}

	/**
	 * Set the number of pull workers.
	 */
	public void setParallelPullCount(Integer parallelPullCount) {
		this.parallelPullCount = parallelPullCount;
	}

	/**
	 * Sets the endpoint for synchronous pulling messages.
	 */
	public void setPullEndpoint(String pullEndpoint) {
		this.pullEndpoint = pullEndpoint;
	}

	/**
	 * Sets the clock to use for the retry logic in synchronous pulling.
	 */
	public void setApiClock(ApiClock apiClock) {
		this.apiClock = apiClock;
	}

	@Override
	public Subscriber createSubscriber(String subscriptionName, MessageReceiver receiver) {
		Subscriber.Builder subscriberBuilder = Subscriber.newBuilder(
				SubscriptionName.of(this.projectId, subscriptionName), receiver);

		if (this.channelProvider != null) {
			subscriberBuilder.setChannelProvider(this.channelProvider);
		}

		if (this.executorProvider != null) {
			subscriberBuilder.setExecutorProvider(this.executorProvider);
		}

		if (this.credentialsProvider != null) {
			subscriberBuilder.setCredentialsProvider(this.credentialsProvider);
		}

		if (this.headerProvider != null) {
			subscriberBuilder.setHeaderProvider(this.headerProvider);
		}

		if (this.systemExecutorProvider != null) {
			subscriberBuilder.setSystemExecutorProvider(this.systemExecutorProvider);
		}

		if (this.flowControlSettings != null) {
			subscriberBuilder.setFlowControlSettings(this.flowControlSettings);
		}

		if (this.maxAckDurationPeriod != null) {
			subscriberBuilder.setMaxAckExtensionPeriod(this.maxAckDurationPeriod);
		}

		if (this.parallelPullCount != null) {
			subscriberBuilder.setParallelPullCount(this.parallelPullCount);
		}

		return subscriberBuilder.build();
	}

	@Override
	public PullRequest createPullRequest(String subscriptionName, Integer maxMessages,
			Boolean returnImmediately) {
		Assert.hasLength(subscriptionName, "The subscription name must be provided.");

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
	public SubscriberStub createSubscriberStub(RetrySettings retrySettings) {
		SubscriptionAdminSettings.Builder subscriptionAdminSettingsBuilder =
				SubscriptionAdminSettings.newBuilder();

		if (this.credentialsProvider != null) {
			subscriptionAdminSettingsBuilder.setCredentialsProvider(this.credentialsProvider);
		}

		if (this.pullEndpoint != null) {
			subscriptionAdminSettingsBuilder.setEndpoint(this.pullEndpoint);
		}

		if (this.executorProvider != null) {
			subscriptionAdminSettingsBuilder.setExecutorProvider(this.executorProvider);
		}

		if (this.headerProvider != null) {
			subscriptionAdminSettingsBuilder.setHeaderProvider(this.headerProvider);
		}

		if (this.channelProvider != null) {
			subscriptionAdminSettingsBuilder.setTransportChannelProvider(this.channelProvider);
		}

		if (this.apiClock != null) {
			subscriptionAdminSettingsBuilder.setClock(this.apiClock);
		}

		if (retrySettings != null) {
			subscriptionAdminSettingsBuilder.pullSettings().setRetrySettings(retrySettings);
		}

		try {
			return GrpcSubscriberStub.create(subscriptionAdminSettingsBuilder.build());
		}
		catch (IOException e) {
			throw new RuntimeException("Error creating the SubscriberStub", e);
		}
	}
}
