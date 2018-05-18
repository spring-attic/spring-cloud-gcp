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
 * @author João André Martins
 */
@ConfigurationProperties("spring.cloud.gcp.trace")
public class GcpTraceProperties implements CredentialsSupplier {

	/**
	 * Overrides the GCP project ID specified in the Core module.
	 */
	private String projectId;

	/**
	 * Overrides the GCP OAuth2 credentials specified in the Core module.
	 */
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.TRACE_APPEND.getUrl());

	/**
	 * Number of threads to be used by the Trace executor.
	 */
	private int numExecutorThreads = 4;

	/**
	 * HTTP/2 authority the channel claims to be connecting to.
	 */
	private String authority;

	/**
	 * Compression to use for the call.
	 */
	private String compression;

	/**
	 * Call deadline.
	 */
	private Long deadlineMs;

	/**
	 * Maximum size for an inbound message.
	 */
	private Integer maxInboundSize;

	/**
	 * Maximum size for an outbound message.
	 */
	private Integer maxOutboundSize;

	/**
	 * Waits for the channel to be ready in case of a transient failure. Defaults to failing fast
	 * in that case.
	 */
	private Boolean waitForReady;

	public String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public Credentials getCredentials() {
		return this.credentials;
	}

	public int getNumExecutorThreads() {
		return this.numExecutorThreads;
	}

	public void setNumExecutorThreads(int numExecutorThreads) {
		this.numExecutorThreads = numExecutorThreads;
	}

	public String getAuthority() {
		return this.authority;
	}

	public void setAuthority(String authority) {
		this.authority = authority;
	}

	public String getCompression() {
		return this.compression;
	}

	public void setCompression(String compression) {
		this.compression = compression;
	}

	public Long getDeadlineMs() {
		return this.deadlineMs;
	}

	public void setDeadlineMs(long deadlineMs) {
		this.deadlineMs = deadlineMs;
	}

	public Integer getMaxInboundSize() {
		return this.maxInboundSize;
	}

	public void setMaxInboundSize(int maxInboundSize) {
		this.maxInboundSize = maxInboundSize;
	}

	public Integer getMaxOutboundSize() {
		return this.maxOutboundSize;
	}

	public void setMaxOutboundSize(int maxOutboundSize) {
		this.maxOutboundSize = maxOutboundSize;
	}

	public Boolean isWaitForReady() {
		return this.waitForReady;
	}

	public void setWaitForReady(boolean waitForReady) {
		this.waitForReady = waitForReady;
	}
}
