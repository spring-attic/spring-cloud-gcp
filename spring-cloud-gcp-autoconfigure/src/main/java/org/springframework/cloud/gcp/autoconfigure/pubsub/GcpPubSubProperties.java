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

import com.google.api.gax.batching.FlowController.LimitExceededBehavior;

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

	public Subscriber getSubscriber() {
		return this.subscriber;
	}

	public Publisher getPublisher() {
		return this.publisher;
	}

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

		/**
		 * Properties for {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private final Retry retry = new Retry();

		/**
		 * Properties for {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private final Batching batching = new Batching();

		public Batching getBatching() {
			return this.batching;
		}

		public Retry getRetry() {
			return this.retry;
		}

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

		/**
		 * Properties for {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private final Retry retry = new Retry();

		/**
		 * Properties for {@link com.google.api.gax.batching.FlowControlSettings}
		 */
		private final FlowControl flowControl = new FlowControl();

		public Retry getRetry() {
			return this.retry;
		}

		public FlowControl getFlowControl() {
			return this.flowControl;
		}

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

	public static class Retry {

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Long> totalTimeoutSeconds = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Long> initialRetryDelaySeconds = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Double> retryDelayMultiplier = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Long> maxRetryDelaySeconds = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Integer> maxAttempts = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Boolean> jittered = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Long> initialRpcTimeoutSeconds = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Double> rpcTimeoutMultiplier = Optional.empty();

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Optional<Long> maxRpcTimeoutSeconds = Optional.empty();

		public Optional<Long> getTotalTimeoutSeconds() {
			return this.totalTimeoutSeconds;
		}

		public void setTotalTimeoutSeconds(Optional<Long> totalTimeoutSeconds) {
			this.totalTimeoutSeconds = totalTimeoutSeconds;
		}

		public Optional<Long> getInitialRetryDelaySeconds() {
			return this.initialRetryDelaySeconds;
		}

		public void setInitialRetryDelaySeconds(Optional<Long> initialRetryDelaySeconds) {
			this.initialRetryDelaySeconds = initialRetryDelaySeconds;
		}

		public Optional<Double> getRetryDelayMultiplier() {
			return this.retryDelayMultiplier;
		}

		public void setRetryDelayMultiplier(Optional<Double> retryDelayMultiplier) {
			this.retryDelayMultiplier = retryDelayMultiplier;
		}

		public Optional<Long> getMaxRetryDelaySeconds() {
			return this.maxRetryDelaySeconds;
		}

		public void setMaxRetryDelaySeconds(Optional<Long> maxRetryDelaySeconds) {
			this.maxRetryDelaySeconds = maxRetryDelaySeconds;
		}

		public Optional<Integer> getMaxAttempts() {
			return this.maxAttempts;
		}

		public void setMaxAttempts(Optional<Integer> maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public Optional<Boolean> getJittered() {
			return this.jittered;
		}

		public void setJittered(Optional<Boolean> jittered) {
			this.jittered = jittered;
		}

		public Optional<Long> getInitialRpcTimeoutSeconds() {
			return this.initialRpcTimeoutSeconds;
		}

		public void setInitialRpcTimeoutSeconds(Optional<Long> initialRpcTimeoutSeconds) {
			this.initialRpcTimeoutSeconds = initialRpcTimeoutSeconds;
		}

		public Optional<Double> getRpcTimeoutMultiplier() {
			return this.rpcTimeoutMultiplier;
		}

		public void setRpcTimeoutMultiplier(Optional<Double> rpcTimeoutMultiplier) {
			this.rpcTimeoutMultiplier = rpcTimeoutMultiplier;
		}

		public Optional<Long> getMaxRpcTimeoutSeconds() {
			return this.maxRpcTimeoutSeconds;
		}

		public void setMaxRpcTimeoutSeconds(Optional<Long> maxRpcTimeoutSeconds) {
			this.maxRpcTimeoutSeconds = maxRpcTimeoutSeconds;
		}
	}

	public static class FlowControl {

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.FlowControlSettings}
		 */
		private Optional<Long> maxOutstandingElementCount = Optional.empty();

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.FlowControlSettings}
		 */
		private Optional<Long> maxOutstandingRequestBytes = Optional.empty();

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.FlowControlSettings}
		 */
		private Optional<LimitExceededBehavior> limitExceededBehavior = Optional.empty();

		public Optional<Long> getMaxOutstandingElementCount() {
			return this.maxOutstandingElementCount;
		}

		public void setMaxOutstandingElementCount(
				Optional<Long> maxOutstandingElementCount) {
			this.maxOutstandingElementCount = maxOutstandingElementCount;
		}

		public Optional<Long> getMaxOutstandingRequestBytes() {
			return this.maxOutstandingRequestBytes;
		}

		public void setMaxOutstandingRequestBytes(
				Optional<Long> maxOutstandingRequestBytes) {
			this.maxOutstandingRequestBytes = maxOutstandingRequestBytes;
		}

		public Optional<LimitExceededBehavior> getLimitExceededBehavior() {
			return this.limitExceededBehavior;
		}

		public void setLimitExceededBehavior(
				Optional<LimitExceededBehavior> limitExceededBehavior) {
			this.limitExceededBehavior = limitExceededBehavior;
		}
	}

	public static class Batching {

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private final FlowControl flowControl = new FlowControl();

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private Optional<Long> elementCountThreshold = Optional.empty();

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private Optional<Long> requestByteThreshold = Optional.empty();

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private Optional<Long> delayThresholdSeconds = Optional.empty();

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private Optional<Boolean> enabled = Optional.empty();

		public Optional<Long> getElementCountThreshold() {
			return this.elementCountThreshold;
		}

		public void setElementCountThreshold(Optional<Long> elementCountThreshold) {
			this.elementCountThreshold = elementCountThreshold;
		}

		public Optional<Long> getRequestByteThreshold() {
			return this.requestByteThreshold;
		}

		public void setRequestByteThreshold(Optional<Long> requestByteThreshold) {
			this.requestByteThreshold = requestByteThreshold;
		}

		public Optional<Long> getDelayThresholdSeconds() {
			return this.delayThresholdSeconds;
		}

		public void setDelayThresholdSeconds(Optional<Long> delayThresholdSeconds) {
			this.delayThresholdSeconds = delayThresholdSeconds;
		}

		public Optional<Boolean> getEnabled() {
			return this.enabled;
		}

		public void setEnabled(Optional<Boolean> enabled) {
			this.enabled = enabled;
		}

		public FlowControl getFlowControl() {
			return this.flowControl;
		}
	}
}
