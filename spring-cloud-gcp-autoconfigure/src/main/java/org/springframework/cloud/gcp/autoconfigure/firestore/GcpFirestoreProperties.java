/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.firestore;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.CredentialsSupplier;
import org.springframework.cloud.gcp.core.GcpScope;

/**
 * Properties for configuring Cloud Datastore.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
@ConfigurationProperties("spring.cloud.gcp.firestore")
public class GcpFirestoreProperties implements CredentialsSupplier {

	/** Overrides the GCP OAuth2 credentials specified in the Core module.
	 * Uses same URL as Datastore*/
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.DATASTORE.getUrl());

	private String projectId;

	/**
	 * The host and port of the Firestore emulator service; can be overridden to specify an emulator.
	 */
	private String hostPort = "firestore.googleapis.com:443";

	@Override
	public Credentials getCredentials() {
		return this.credentials;
	}

	public String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getHostPort() {
		return hostPort;
	}

	public void setHostPort(String hostPort) {
		this.hostPort = hostPort;
	}
}
