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

	/**
	 * Contains settings specific to the subscriber factory.
	 */
	private final Subscriber subscriber = new Subscriber();

	/**
	 * Contains settings specific to the publisher factory.
	 */
	private final Publisher publisher = new Publisher();

	/**
	 * Overrides the GCP project ID specified in the Core module.
	 */
	private String projectId;

	/**
	 * The host and port of the local running emulator. If provided, this will setup the
	 * client to connect against a running pub/sub emulator.
	 */
	private String emulatorHost;

	/**
	 * Overrides the GCP OAuth2 credentials specified in the Core module.
	 */
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

	public static class Publisher {

		/**
		 * Number of threads used by every publisher.
		 */
		private int executorThreads = 4;

		/**
		 * Retry properties.
		 */
		private final Retry retry = new Retry();

		/**
		 * Batching properties.
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
		 * Number of threads used by every subscriber.
		 */
		private int executorThreads = 4;

		/**
		 * The optional pull endpoint setting for the subscriber factory.
		 */
		private String pullEndpoint;

		/**
		 * The optional max ack extension period in seconds for the subscriber factory.
		 */
		private Long maxAckExtensionPeriod = 0L;

		/**
		 * The optional parallel pull count setting for the subscriber factory.
		 */
		private Integer parallelPullCount;

		/**
		 * Retry settings for subscriber factory.
		 */
		private final Retry retry = new Retry();

		/**
		 * Flow control settings for subscriber factory.
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

		public Long getMaxAckExtensionPeriod() {
			return this.maxAckExtensionPeriod;
		}

		public void setMaxAckExtensionPeriod(Long maxAckExtensionPeriod) {
			this.maxAckExtensionPeriod = maxAckExtensionPeriod;
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
		 * TotalTimeout has ultimate control over how long the logic should keep trying the remote call
		 * until it gives up completely. The higher the total timeout, the more retries can be
		 * attempted.
		 */
		private Long totalTimeoutSeconds;

		/**
		 * InitialRetryDelay controls the delay before the first retry. Subsequent retries will use this
		 * value adjusted according to the RetryDelayMultiplier.
		 */
		private Long initialRetryDelaySeconds;

		/**
		 * RetryDelayMultiplier controls the change in retry delay. The retry delay of the previous call
		 * is multiplied by the RetryDelayMultiplier to calculate the retry delay for the next call.
		 */
		private Double retryDelayMultiplier;

		/**
		 * MaxRetryDelay puts a limit on the value of the retry delay, so that the RetryDelayMultiplier
		 * can't increase the retry delay higher than this amount.
		 */
		private Long maxRetryDelaySeconds;

		/**
		 * MaxAttempts defines the maximum number of attempts to perform.
		 * If this value is greater than 0, and the number of attempts reaches this limit,
		 * the logic will give up retrying even if the total retry time is still lower
		 * than TotalTimeout.
		 */
		private Integer maxAttempts;

		/**
		 * Jitter determines if the delay time should be randomized.
		 */
		private Boolean jittered;

		/**
		 * InitialRpcTimeout controls the timeout for the initial RPC. Subsequent calls will use this
		 * value adjusted according to the RpcTimeoutMultiplier.
		 */
		private Long initialRpcTimeoutSeconds;

		/**
		 * RpcTimeoutMultiplier controls the change in RPC timeout. The timeout of the previous call is
		 * multiplied by the RpcTimeoutMultiplier to calculate the timeout for the next call.
		 */
		private Double rpcTimeoutMultiplier;

		/**
		 * MaxRpcTimeout puts a limit on the value of the RPC timeout, so that the RpcTimeoutMultiplier
		 * can't increase the RPC timeout higher than this amount.
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
		 * Maximum number of outstanding elements to keep in memory before enforcing flow control.
		 */
		private Long maxOutstandingElementCount;

		/**
		 * Maximum number of outstanding bytes to keep in memory before enforcing flow control.
		 */
		private Long maxOutstandingRequestBytes;

		/**
		 * The behavior when the specified limits are exceeded.
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
		 * Flow control settings for batching.
		 */
		private final FlowControl flowControl = new FlowControl();

		/**
		 * The element count threshold to use for batching.
		 */
		private Long elementCountThreshold;

		/**
		 * The request byte threshold to use for batching.
		 */
		private Long requestByteThreshold;

		/**
		 * The delay threshold to use for batching. After this amount of time has elapsed (counting
		 * from the first element added), the elements will be wrapped up in a batch and sent.
		 */
		private Long delayThresholdSeconds;

		/**
		 * Enables batching if true.
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
