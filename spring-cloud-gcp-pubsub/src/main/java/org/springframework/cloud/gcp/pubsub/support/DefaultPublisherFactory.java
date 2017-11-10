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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.ExecutorProvider;
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
 * Creates {@link Publisher}s for topics once and reuses them.
 *
 * <p>
 * Every {@link Publisher} produced by this factory reuses the same {@link ExecutorProvider},
 * so the number of created threads doesn't spin out of control.
 *
 * @author João André Martins
 */
public class DefaultPublisherFactory implements PublisherFactory {

	private final String projectId;

	/**
	 * {@link Publisher} cache, enforces only one {@link Publisher} per PubSub topic exists.
	 */
	private final ConcurrentHashMap<String, Publisher> publishers = new ConcurrentHashMap<>();

	private final ExecutorProvider executorProvider;

	private final TransportChannelProvider channelProvider;

	private final CredentialsProvider credentialsProvider;

	public DefaultPublisherFactory(GcpProjectIdProvider projectIdProvider,
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
	}

	@Override
	public Publisher getPublisher(String topic) {
		return this.publishers.computeIfAbsent(topic, key -> {
			try {
				return Publisher.newBuilder(TopicName.of(this.projectId, key))
						.setExecutorProvider(this.executorProvider)
						.setChannelProvider(this.channelProvider)
						.setCredentialsProvider(this.credentialsProvider)
						.build();
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
