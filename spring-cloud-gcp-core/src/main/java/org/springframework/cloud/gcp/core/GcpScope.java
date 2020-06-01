/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.core;

/**
 * OAuth2 scopes for Google Cloud Platform services integrated by Spring Cloud GCP.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
public enum GcpScope {
	/** scope for Pub/Sub. **/
	PUBSUB("https://www.googleapis.com/auth/pubsub"),

	/** scope for Spanner database admin functions. **/
	SPANNER_ADMIN("https://www.googleapis.com/auth/spanner.admin"),

	/** scope for Spanner data read and write. **/
	SPANNER_DATA("https://www.googleapis.com/auth/spanner.data"),

	/** scope for Datastore. **/
	DATASTORE("https://www.googleapis.com/auth/datastore"),

	/** scope for SQL Admin. **/
	SQLADMIN("https://www.googleapis.com/auth/sqlservice.admin"),

	/** scope for Storage read-only. **/
	STORAGE_READ_ONLY("https://www.googleapis.com/auth/devstorage.read_only"),

	/** scope for Storage read-write. **/
	STORAGE_READ_WRITE("https://www.googleapis.com/auth/devstorage.read_write"),

	/** scope for Runtime Configurator. **/
	RUNTIME_CONFIG_SCOPE("https://www.googleapis.com/auth/cloudruntimeconfig"),

	/** scope for Trace. **/
	TRACE_APPEND("https://www.googleapis.com/auth/trace.append"),

	/** scope for GCP general operations. **/
	CLOUD_PLATFORM("https://www.googleapis.com/auth/cloud-platform"),

	/** scope for Vision. **/
	CLOUD_VISION("https://www.googleapis.com/auth/cloud-vision"),

	/** scope for BigQuery. */
	BIG_QUERY("https://www.googleapis.com/auth/bigquery"),

	/** scope for Cloud Monitoring.*/
	CLOUD_MONITORING_WRITE("https://www.googleapis.com/auth/monitoring.write");

	private String url;

	GcpScope(String url) {
		this.url = url;
	}

	public String getUrl() {
		return this.url;
	}
}
