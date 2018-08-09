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

package org.springframework.cloud.gcp.autoconfigure.datastore;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.CredentialsSupplier;
import org.springframework.cloud.gcp.core.GcpScope;

/**
 * Properties for configuring Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@ConfigurationProperties("spring.cloud.gcp.datastore")
public class GcpDatastoreProperties implements CredentialsSupplier {

	/** Overrides the GCP OAuth2 credentials specified in the Core module. */
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.DATASTORE.getUrl());

	private String projectId;

	private String namespace;

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

	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

}
