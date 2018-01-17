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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.api.gax.batching.BatchingSettings;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
import com.google.api.gax.retrying.RetrySettings;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.annotations.VisibleForTesting;
import com.google.pubsub.v1.TopicName;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.pubsub.core.PubSubException;
import org.springframework.util.Assert;

/**
 * The default {@link PublisherFactory} implementation.
 *
 * <p>
 * Creates {@link Publisher}s for topics once, caches and reuses them.
 *
 * @author João André Martins
 */
public class DefaultPublisherFactory implements PublisherFactory {

	private final String projectId;

	/**
	 * {@link Publisher} cache, enforces only one {@link Publisher} per PubSub topic exists.
	 */
	private final ConcurrentHashMap<String, Publisher> publishers = new ConcurrentHashMap<>();

	private ExecutorProvider executorProvider;

	private TransportChannelProvider channelProvider;

	private CredentialsProvider credentialsProvider;

	private RetrySettings retrySettings;

	private BatchingSettings batchingSettings;

	/**
	 * Create {@link DefaultPublisherFactory} instance based on the provided {@link GcpProjectIdProvider}.
	 * <p>The {@link GcpProjectIdProvider} must not be null, neither provide an empty {@code projectId}.
	 * @param projectIdProvider provides the GCP project ID
	 */
	public DefaultPublisherFactory(GcpProjectIdProvider projectIdProvider) {
		Assert.notNull(projectIdProvider, "The project ID provider can't be null.");

		this.projectId = projectIdProvider.getProjectId();
		Assert.hasText(this.projectId, "The project ID can't be null or empty.");
	}

	/**
	 * Set the provider for the executor that will be used by the publisher. Useful to specify the number of threads to
	 * be used by each executor.
	 */
	public void setExecutorProvider(ExecutorProvider executorProvider) {
		this.executorProvider = executorProvider;
	}

	/**
	 * Set the provider for the channel to be used by the publisher. Useful to specify HTTP headers for the REST API
	 * calls.
	 */
	public void setChannelProvider(TransportChannelProvider channelProvider) {
		this.channelProvider = channelProvider;
	}

	/**
	 * Set the provider for the GCP credentials to be used by the publisher on every API calls it makes.
	 */
	public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
		this.credentialsProvider = credentialsProvider;
	}

	/**
	 * Set the API call retry configuration.
	 */
	public void setRetrySettings(RetrySettings retrySettings) {
		this.retrySettings = retrySettings;
	}

	/**
	 * Set the API call batching configuration.
	 */
	public void setBatchingSettings(BatchingSettings batchingSettings) {
		this.batchingSettings = batchingSettings;
	}

	@Override
	public Publisher createPublisher(String topic) {
		return this.publishers.computeIfAbsent(topic, key -> {
			try {
				Publisher.Builder publisherBuilder =
						Publisher.newBuilder(TopicName.of(this.projectId, key));

				if (this.executorProvider != null) {
					publisherBuilder.setExecutorProvider(this.executorProvider);
				}

				if (this.channelProvider != null) {
					publisherBuilder.setChannelProvider(this.channelProvider);
				}

				if (this.credentialsProvider != null) {
					publisherBuilder.setCredentialsProvider(this.credentialsProvider);
				}

				if (this.retrySettings != null) {
					publisherBuilder.setRetrySettings(this.retrySettings);
				}

				if (this.batchingSettings != null) {
					publisherBuilder.setBatchingSettings(this.batchingSettings);
				}

				return publisherBuilder.build();
			}
			catch (IOException ioe) {
				throw new PubSubException("An error creating the Google Cloud Pub/Sub publisher " +
						"occurred.", ioe);
			}
		});
	}

	@VisibleForTesting
	Map<String, Publisher> getCache() {
		return this.publishers;
	}
}
