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

package org.springframework.cloud.gcp.core;

/**
 * OAuth2 scopes for Google Cloud Platform services integrated by Spring Cloud GCP.
 *
 * @author João André Martins
 */
public enum GcpScope {
	PUBSUB("https://www.googleapis.com/auth/pubsub"),
	SPANNER("https://www.googleapis.com/auth/spanner.data"),
	SQLADMIN("https://www.googleapis.com/auth/sqlservice.admin"),
	STORAGE_READ_ONLY("https://www.googleapis.com/auth/devstorage.read_only"),
	STORAGE_READ_WRITE("https://www.googleapis.com/auth/devstorage.read_write"),
	RUNTIME_CONFIG_SCOPE("https://www.googleapis.com/auth/cloudruntimeconfig"),
	TRACE_APPEND("https://www.googleapis.com/auth/trace.append"),
	CLOUD_PLATFORM("https://www.googleapis.com/auth/cloud-platform");

	private String url;

	GcpScope(String url) {
		this.url = url;
	}

	public String getUrl() {
		return this.url;
	}
}
