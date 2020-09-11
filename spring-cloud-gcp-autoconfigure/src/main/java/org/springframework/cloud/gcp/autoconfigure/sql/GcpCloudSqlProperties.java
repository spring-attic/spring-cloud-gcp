/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.sql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gcp.core.Credentials;

/**
 * Google Cloud SQL properties.
 *
 * @author João André Martins
 * @author Øystein Urdahl Hardeng
 */
@ConfigurationProperties("spring.cloud.gcp.sql")
public class GcpCloudSqlProperties {
	/** Name of the database in the Cloud SQL instance. */
	private String databaseName;

	/** Cloud SQL instance connection name. [GCP_PROJECT_ID]:[INSTANCE_REGION]:[INSTANCE_NAME]. */
	private String instanceConnectionName;

	/** A comma delimited list of preferred IP types for connecting to the Cloud SQL instance. */
	private String ipTypes;

	/** Overrides the GCP OAuth2 credentials specified in the Core module. */
	private Credentials credentials = new Credentials();

	public String getDatabaseName() {
		return this.databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getInstanceConnectionName() {
		return this.instanceConnectionName;
	}

	public void setInstanceConnectionName(String instanceConnectionName) {
		this.instanceConnectionName = instanceConnectionName;
	}

	public String getIpTypes() {
		return this.ipTypes;
	}

	public void setIpTypes(String ipTypes) {
		this.ipTypes = ipTypes;
	}

	public Credentials getCredentials() {
		return this.credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}
}
