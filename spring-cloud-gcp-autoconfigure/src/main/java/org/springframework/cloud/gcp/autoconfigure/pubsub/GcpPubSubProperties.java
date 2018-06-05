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
		private Long maxAckDurationSeconds;

		/**
		 * The optional parallel pull count setting for the subscriber factory.
		 */
		private Integer parallelPullCount;

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

		public Long getMaxAckDurationSeconds() {
			return this.maxAckDurationSeconds;
		}

		public void setMaxAckDurationSeconds(Long maxAckDurationSeconds) {
			this.maxAckDurationSeconds = maxAckDurationSeconds;
		}

		public Integer getParallelPullCount() {
			return this.parallelPullCount;
		}

		public void setParallelPullCount(Integer parallelPullCount) {
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
		private Long totalTimeoutSeconds;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Long initialRetryDelaySeconds;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Double retryDelayMultiplier;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Long maxRetryDelaySeconds;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Integer maxAttempts;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Boolean jittered;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Long initialRpcTimeoutSeconds;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Double rpcTimeoutMultiplier;

		/**
		 * Property for setting of the same name in {@link com.google.api.gax.retrying.RetrySettings}
		 */
		private Long maxRpcTimeoutSeconds;

		public Long getTotalTimeoutSeconds() {
			return this.totalTimeoutSeconds;
		}

		public void setTotalTimeoutSeconds(Long totalTimeoutSeconds) {
			this.totalTimeoutSeconds = totalTimeoutSeconds;
		}

		public Long getInitialRetryDelaySeconds() {
			return this.initialRetryDelaySeconds;
		}

		public void setInitialRetryDelaySeconds(Long initialRetryDelaySeconds) {
			this.initialRetryDelaySeconds = initialRetryDelaySeconds;
		}

		public Double getRetryDelayMultiplier() {
			return this.retryDelayMultiplier;
		}

		public void setRetryDelayMultiplier(Double retryDelayMultiplier) {
			this.retryDelayMultiplier = retryDelayMultiplier;
		}

		public Long getMaxRetryDelaySeconds() {
			return this.maxRetryDelaySeconds;
		}

		public void setMaxRetryDelaySeconds(Long maxRetryDelaySeconds) {
			this.maxRetryDelaySeconds = maxRetryDelaySeconds;
		}

		public Integer getMaxAttempts() {
			return this.maxAttempts;
		}

		public void setMaxAttempts(Integer maxAttempts) {
			this.maxAttempts = maxAttempts;
		}

		public Boolean getJittered() {
			return this.jittered;
		}

		public void setJittered(Boolean jittered) {
			this.jittered = jittered;
		}

		public Long getInitialRpcTimeoutSeconds() {
			return this.initialRpcTimeoutSeconds;
		}

		public void setInitialRpcTimeoutSeconds(Long initialRpcTimeoutSeconds) {
			this.initialRpcTimeoutSeconds = initialRpcTimeoutSeconds;
		}

		public Double getRpcTimeoutMultiplier() {
			return this.rpcTimeoutMultiplier;
		}

		public void setRpcTimeoutMultiplier(Double rpcTimeoutMultiplier) {
			this.rpcTimeoutMultiplier = rpcTimeoutMultiplier;
		}

		public Long getMaxRpcTimeoutSeconds() {
			return this.maxRpcTimeoutSeconds;
		}

		public void setMaxRpcTimeoutSeconds(Long maxRpcTimeoutSeconds) {
			this.maxRpcTimeoutSeconds = maxRpcTimeoutSeconds;
		}
	}

	public static class FlowControl {

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.FlowControlSettings}
		 */
		private Long maxOutstandingElementCount;

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.FlowControlSettings}
		 */
		private Long maxOutstandingRequestBytes;

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.FlowControlSettings}
		 */
		private LimitExceededBehavior limitExceededBehavior;

		public Long getMaxOutstandingElementCount() {
			return this.maxOutstandingElementCount;
		}

		public void setMaxOutstandingElementCount(
				Long maxOutstandingElementCount) {
			this.maxOutstandingElementCount = maxOutstandingElementCount;
		}

		public Long getMaxOutstandingRequestBytes() {
			return this.maxOutstandingRequestBytes;
		}

		public void setMaxOutstandingRequestBytes(
				Long maxOutstandingRequestBytes) {
			this.maxOutstandingRequestBytes = maxOutstandingRequestBytes;
		}

		public LimitExceededBehavior getLimitExceededBehavior() {
			return this.limitExceededBehavior;
		}

		public void setLimitExceededBehavior(
				LimitExceededBehavior limitExceededBehavior) {
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
		private Long elementCountThreshold;

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private Long requestByteThreshold;

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private Long delayThresholdSeconds;

		/**
		 * Property for setting of the same name in
		 * {@link com.google.api.gax.batching.BatchingSettings}
		 */
		private Boolean enabled;

		public Long getElementCountThreshold() {
			return this.elementCountThreshold;
		}

		public void setElementCountThreshold(Long elementCountThreshold) {
			this.elementCountThreshold = elementCountThreshold;
		}

		public Long getRequestByteThreshold() {
			return this.requestByteThreshold;
		}

		public void setRequestByteThreshold(Long requestByteThreshold) {
			this.requestByteThreshold = requestByteThreshold;
		}

		public Long getDelayThresholdSeconds() {
			return this.delayThresholdSeconds;
		}

		public void setDelayThresholdSeconds(Long delayThresholdSeconds) {
			this.delayThresholdSeconds = delayThresholdSeconds;
		}

		public Boolean getEnabled() {
			return this.enabled;
		}

		public void setEnabled(Boolean enabled) {
			this.enabled = enabled;
		}

		public FlowControl getFlowControl() {
			return this.flowControl;
		}
	}
}
