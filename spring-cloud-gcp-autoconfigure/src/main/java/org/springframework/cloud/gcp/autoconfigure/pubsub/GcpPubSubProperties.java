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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.CredentialsSupplier;
import org.springframework.cloud.gcp.core.GcpScope;

/**
 * @author João André Martins
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
@ConfigurationProperties("spring.cloud.gcp.pubsub")
public class GcpPubSubProperties implements CredentialsSupplier {

	private final Subscriber subscriber = new Subscriber();

	private final Publisher publisher = new Publisher();

	public Subscriber getSubscriber() {
		return this.subscriber;
	}

	public Publisher getPublisher() {
		return this.publisher;
	}

	/** Overrides the GCP project ID specified in the Core module. */
	private String projectId;

	/**
	 * The host and port of the local running emulator. If provided, this will setup the
	 * client to connect against a running pub/sub emulator
	 */
	private String emulatorHost;

	/**
	 * Array of packages containing types that are whitelisted for deserialization from
	 * message payloads.
	 */
	private String[] trustedPackages;

	/** Overrides the GCP OAuth2 credentials specified in the Core module. */
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.PUBSUB.getUrl());

	public String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public Credentials getCredentials() {
		return this.credentials;
	}

	public String getEmulatorHost() {
		return this.emulatorHost;
	}

	public void setEmulatorHost(String emulatorHost) {
		this.emulatorHost = emulatorHost;
	}

	public String[] getTrustedPackages() {
		return this.trustedPackages;
	}

	public void setTrustedPackages(String[] trustedPackages) {
		this.trustedPackages = trustedPackages;
	}

	public static class Publisher {

		/**
		 * Number of threads used by every {@link com.google.cloud.pubsub.v1.Publisher}.
		 */
		private int executorThreads = 4;

		public int getExecutorThreads() {
			return this.executorThreads;
		}

		public void setExecutorThreads(int executorThreads) {
			this.executorThreads = executorThreads;
		}
	}

	public static class Subscriber {

		/**
		 * Number of threads used by every {@link com.google.cloud.pubsub.v1.Subscriber}.
		 */
		private int executorThreads = 4;

		/**
		 * The optional pull endpoint setting for the subscriber factory.
		 */
		private String pullEndpoint;

		/**
		 * The optional max ack duration in seconds for the subscriber factory.
		 */
		private Optional<Long> maxAckDurationSeconds = Optional.empty();

		/**
		 * The optional parallel pull count setting for the subscriber factory.
		 */
		private Optional<Integer> parallelPullCount = Optional.empty();

		public String getPullEndpoint() {
			return this.pullEndpoint;
		}

		public void setPullEndpoint(String pullEndpoint) {
			this.pullEndpoint = pullEndpoint;
		}

		public Optional<Long> getMaxAckDurationSeconds() {
			return this.maxAckDurationSeconds;
		}

		public void setMaxAckDurationSeconds(Optional<Long> maxAckDurationSeconds) {
			this.maxAckDurationSeconds = maxAckDurationSeconds;
		}

		public Optional<Integer> getParallelPullCount() {
			return this.parallelPullCount;
		}

		public void setParallelPullCount(Optional<Integer> parallelPullCount) {
			this.parallelPullCount = parallelPullCount;
		}

		public int getExecutorThreads() {
			return this.executorThreads;
		}

		public void setExecutorThreads(int executorThreads) {
			this.executorThreads = executorThreads;
		}
	}
}
