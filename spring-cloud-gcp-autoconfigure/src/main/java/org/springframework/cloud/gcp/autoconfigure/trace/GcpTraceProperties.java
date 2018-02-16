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

package org.springframework.cloud.gcp.autoconfigure.trace;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.CredentialsSupplier;
import org.springframework.cloud.gcp.core.GcpScope;

/**
 * Stackdriver Trace Properties.
 *
 * @author Ray Tsang
 * @author Mike Eltsufin
 */
@ConfigurationProperties("spring.cloud.gcp.trace")
public class GcpTraceProperties implements CredentialsSupplier {
	/**
	 * Number of threads to use by the underlying gRPC channel to send the trace request to
	 * Stackdriver.
	 */
	private int executorThreads = 4;

	/**
	 * Buffer size in bytes. Traces will be flushed to Stackdriver when buffered trace
	 * messages exceed this size.
	 *
	 * <p>This value is defaulted to 1% of the {@link Runtime#totalMemory()} in bytes.
	 * However, be careful when running inside of a containerized environment. You should either set JVM's max heap
	 * or set this value explicitly.
	 */
	private int bufferSizeBytes = percentOfRuntimeTotalMemory(0.01f);

	/**
	 * Scheduled delay in seconds. Buffered trace messages will be flushed to Stackdriver when
	 * buffered longer than scheduled delays.
	 */
	private int scheduledDelaySeconds = 10;

	/** Overrides the GCP project ID specified in the Core module. */
	private String projectId;

	/** Overrides the GCP OAuth2 credentials specified in the Core module. */
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.TRACE_APPEND.getUrl());

	/**
	 * A utility method to determine x% of total memory based on Zipkin's AsyncReporter.
	 * However, need to be careful about running this inside of a container environment since only certain versions
	 * of the JDK with additional flags can accurately detect container memory limit.
	 *
	 * @param percentage 1.0 means 100%, 0.01 means 1%
	 * @return Percentage of @{link Runtime{@link Runtime#totalMemory()}}
	 */
	static int percentOfRuntimeTotalMemory(float percentage) {
		long result = (long) (Runtime.getRuntime().totalMemory() * percentage);
		// don't overflow in the rare case 1% of memory is larger than 2 GiB!
		return (int) Math.max(Math.min(Integer.MAX_VALUE, result), Integer.MIN_VALUE);
	}

	public int getExecutorThreads() {
		return this.executorThreads;
	}

	public void setExecutorThreads(int executorThreads) {
		this.executorThreads = executorThreads;
	}

	public int getBufferSizeBytes() {
		return this.bufferSizeBytes;
	}

	public void setBufferSizeBytes(int bufferSizeBytes) {
		this.bufferSizeBytes = bufferSizeBytes;
	}

	public int getScheduledDelaySeconds() {
		return this.scheduledDelaySeconds;
	}

	public void setScheduledDelaySeconds(int scheduledDelaySeconds) {
		this.scheduledDelaySeconds = scheduledDelaySeconds;
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

}
