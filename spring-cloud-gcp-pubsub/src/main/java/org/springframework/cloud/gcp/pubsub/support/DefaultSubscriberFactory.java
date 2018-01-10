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

import com.google.api.gax.batching.FlowControlSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.rpc.HeaderProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.pubsub.v1.SubscriptionName;
import org.threeten.bp.Duration;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.Assert;

/**
 *
 * The default {@link SubscriberFactory} implementation.
 *
 * @author João André Martins
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
	 * Sets the provider for the subscribers' executor.
	 *
	 * <p>
	 * Useful to specify the number of threads to be used by each executor.
	 */
	public void setExecutorProvider(ExecutorProvider executorProvider) {
		this.executorProvider = executorProvider;
	}

	/**
	 * Sets the provider for the subscribers' transport channel.
	 */
	public void setChannelProvider(TransportChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
	}

	/**
	 * Sets the provider for the GCP credentials to be used by the subscribers' API calls.
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Sets the provider for the HTTP headers to be added to the subscribers' REST API calls.
	 */
	public void setHeaderProvider(HeaderProvider headerProvider) {
		this.headerProvider = headerProvider;
	}

	/**
	 * Sets the provider for the system executor, to poll and manage lease extensions.
	 */
	public void setSystemExecutorProvider(ExecutorProvider systemExecutorProvider) {
		this.systemExecutorProvider = systemExecutorProvider;
	}

	/**
	 * Sets the flow control for the subscribers, including the behaviour for when the flow limits are hit.
	 */
	public void setFlowControlSettings(FlowControlSettings flowControlSettings) {
		this.flowControlSettings = flowControlSettings;
	}

	/**
	 * Sets the maximum period the ack timeout is extended by.
	 */
	public void setMaxAckDurationPeriod(Duration maxAckDurationPeriod) {
		this.maxAckDurationPeriod = maxAckDurationPeriod;
	}

	/**
	 * Sets the number of pull workers.
	 */
	public void setParallelPullCount(Integer parallelPullCount) {
		this.parallelPullCount = parallelPullCount;
	}

	@Override
	public Subscriber getSubscriber(String subscriptionName, MessageReceiver receiver) {
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
}
