/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Firebase Authentication application properties.
 *
 * @author Vinicius Carvalho
 * @since 1.2.2
 */
@ConfigurationProperties("spring.cloud.gcp.security.firebase")
public class FirebaseAuthenticationProperties {

	/**
	 * Link to Google's public endpoint containing Firebase public keys.
	 */
	private String publicKeysEndpoint = "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

	/**
	 * Overrides the GCP project ID specified in the Core module.
	 */
	private String projectId;

	public String getPublicKeysEndpoint() {
		return publicKeysEndpoint;
	}

	public void setPublicKeysEndpoint(String publicKeysEndpoint) {
		this.publicKeysEndpoint = publicKeysEndpoint;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
}
