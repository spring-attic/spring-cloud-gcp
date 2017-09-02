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
package org.springframework.cloud.gcp.trace;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stackdriver Trace Properties.
 *
 * @author Ray Tsang
 */
@ConfigurationProperties("spring.cloud.gcp.trace")
public class GcpTraceProperties {
	/**
	 * Number of threads to use by the underlying gRPC channel to send the trace request to
	 * Stackdriver.
	 */
	private int executorThreads = 4;

	/**
	 * Buffer size in bytes. Traces will be flushed to Stackdriver when buffered trace
	 * messages exceed this size.
	 */
	private int bufferSizeBytes = 4096;

	/**
	 * Scheduled delay in seconds. Buffered trace messages will be flushed to Stackdriver when
	 * buffered longer than scheduled delays.
	 */
	private int scheduledDelaySeconds = 5;

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
}
