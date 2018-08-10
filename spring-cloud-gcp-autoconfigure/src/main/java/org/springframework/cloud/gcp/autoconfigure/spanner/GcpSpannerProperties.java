/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.spanner;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.CredentialsSupplier;
import org.springframework.cloud.gcp.core.GcpScope;

/**
 * @author Chengyuan Zhao
 * @author Ray Tsang
 */
@ConfigurationProperties("spring.cloud.gcp.spanner")
public class GcpSpannerProperties implements CredentialsSupplier {

	/** Overrides the GCP OAuth2 credentials specified in the Core module. */
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(
			GcpScope.SPANNER_DATA.getUrl(), GcpScope.SPANNER_ADMIN.getUrl());

	private String projectId;

	private String instanceId;

	private String database;

	// If {@code true} then create-table statements generated will cascade on delete.
	// No-action on delete if {@code false}.
	private boolean createInterleavedTableDdlOnDeleteCascade = true;

	// Default value is negative to indicate to use Cloud Spanner default number.
	private int numRpcChannels = -1;

	// Default value is negative to indicate to use Cloud Spanner default number.
	private int prefetchChunks = -1;

	// Default value is negative to indicate to use Cloud Spanner default number.
	private int minSessions = -1;

	// Default value is negative to indicate to use Cloud Spanner default number.
	private int maxSessions = -1;

	// Default value is negative to indicate to use Cloud Spanner default number.
	private int maxIdleSessions = -1;

	// Default value is negative to indicate to use Cloud Spanner default number.
	private float writeSessionsFraction = -1;

	// Default value is negative to indicate to use Cloud Spanner default number.
	private int keepAliveIntervalMinutes = -1;

	public Credentials getCredentials() {
		return this.credentials;
	}

	public String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getInstanceId() {
		return this.instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getDatabase() {
		return this.database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public int getNumRpcChannels() {
		return this.numRpcChannels;
	}

	public void setNumRpcChannels(int numRpcChannels) {
		this.numRpcChannels = numRpcChannels;
	}

	public int getPrefetchChunks() {
		return this.prefetchChunks;
	}

	public void setPrefetchChunks(int prefetchChunks) {
		this.prefetchChunks = prefetchChunks;
	}

	public int getMinSessions() {
		return this.minSessions;
	}

	public void setMinSessions(int minSessions) {
		this.minSessions = minSessions;
	}

	public int getMaxSessions() {
		return this.maxSessions;
	}

	public void setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
	}

	public int getMaxIdleSessions() {
		return this.maxIdleSessions;
	}

	public void setMaxIdleSessions(int maxIdleSessions) {
		this.maxIdleSessions = maxIdleSessions;
	}

	public float getWriteSessionsFraction() {
		return this.writeSessionsFraction;
	}

	public void setWriteSessionsFraction(float writeSessionsFraction) {
		this.writeSessionsFraction = writeSessionsFraction;
	}

	public int getKeepAliveIntervalMinutes() {
		return this.keepAliveIntervalMinutes;
	}

	public void setKeepAliveIntervalMinutes(int keepAliveIntervalMinutes) {
		this.keepAliveIntervalMinutes = keepAliveIntervalMinutes;
	}

	public boolean isCreateInterleavedTableDdlOnDeleteCascade() {
		return this.createInterleavedTableDdlOnDeleteCascade;
	}

	public void setCreateInterleavedTableDdlOnDeleteCascade(
			boolean createInterleavedTableDdlOnDeleteCascade) {
		this.createInterleavedTableDdlOnDeleteCascade =
				createInterleavedTableDdlOnDeleteCascade;
	}
}
