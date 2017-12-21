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

	private String projectId;

	private final ExecutorProvider executorProvider;

	private final TransportChannelProvider channelProvider;

	private final CredentialsProvider credentialsProvider;

	private final HeaderProvider headerProvider;

	private final ExecutorProvider systemExecutorProvider;

	private final FlowControlSettings flowControlSettings;

	private final Duration maxAckDurationPeriod;

	private final Integer parallelPullCount;

	/**
	 * Base {@link DefaultSubscriberFactory} constructor.
	 *
	 * @param projectIdProvider provides the GCP project ID
	 * @param executorProvider provides the executor to be used by the subscriber, useful to specify
	 *                         the number of threads to be used by each executor
	 * @param channelProvider provides the channel to be used by the subscriber
	 * @param credentialsProvider provides the GCP credentials to be used by the subscriber on the
	 *                            API calls it makes
	 * @param headerProvider provides headers for the API's HTTP calls
	 * @param systemExecutorProvider provides an executor for polling and managing lease extensions
	 * @param flowControlSettings configures the flow control of the subscriber, including the
	 *                            behaviour for when flow limits are hit
	 * @param maxAckDurationPeriod sets the maximum period the ack period is extended by
	 * @param parallelPullCount sets the number of pull workers
	 */
	public DefaultSubscriberFactory(GcpProjectIdProvider projectIdProvider,
			ExecutorProvider executorProvider,
			TransportChannelProvider channelProvider,
			CredentialsProvider credentialsProvider,
			HeaderProvider headerProvider,
			ExecutorProvider systemExecutorProvider,
			FlowControlSettings flowControlSettings,
			Duration maxAckDurationPeriod,
			Integer parallelPullCount) {
		Assert.notNull(projectIdProvider, "The project ID provider can't be null.");
		Assert.notNull(executorProvider, "The executor provider can't be null.");
		Assert.notNull(channelProvider, "The channel provider can't be null.");
		Assert.notNull(credentialsProvider, "The credentials provider can't be null.");

		this.projectId = projectIdProvider.getProjectId();
		Assert.hasText(this.projectId, "The project ID can't be null or empty.");
		this.executorProvider = executorProvider;
		this.channelProvider = channelProvider;
		this.credentialsProvider = credentialsProvider;
		this.headerProvider = headerProvider;
		this.systemExecutorProvider = systemExecutorProvider;
		this.flowControlSettings = flowControlSettings;
		this.maxAckDurationPeriod = maxAckDurationPeriod;
		this.parallelPullCount = parallelPullCount;
	}

	public DefaultSubscriberFactory(GcpProjectIdProvider projectIdProvider,
			ExecutorProvider executorProvider,
			TransportChannelProvider channelProvider,
			CredentialsProvider credentialsProvider) {
		this(projectIdProvider, executorProvider, channelProvider, credentialsProvider,
				null, null, null, null, null);
	}

	@Override
	public Subscriber getSubscriber(String subscriptionName, MessageReceiver receiver) {
		Subscriber.Builder subscriberBuilder = Subscriber.newBuilder(
				SubscriptionName.of(this.projectId, subscriptionName), receiver)
				.setChannelProvider(this.channelProvider)
				.setExecutorProvider(this.executorProvider)
				.setCredentialsProvider(this.credentialsProvider);

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
