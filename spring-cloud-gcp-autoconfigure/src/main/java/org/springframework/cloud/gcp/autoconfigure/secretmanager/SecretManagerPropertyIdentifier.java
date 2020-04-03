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

package org.springframework.cloud.gcp.autoconfigure.secretmanager;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.Assert;

/**
 * Identifiers for specifying Secret Manager secrets using (project-id, secret-id, version).
 *
 * @author Daniel Zou
 * @since 1.2.3
 */
public class SecretManagerPropertyIdentifier {

	// This prefix string distinguishes whether a property should be queried from Secret Manager or not.
	private static final String GCP_SECRET_PREFIX = "gcp-secret/";

	private final String projectId;

	private final String secretId;

	private final String version;

	SecretManagerPropertyIdentifier(String projectId, String secretId, String version) {
		Assert.notNull(projectId, "Project Id of GCP Secret Manager secret must not be null");
		Assert.notNull(projectId, "Secret Id of GCP Secret Manager secret must not be null");
		Assert.notNull(projectId, "Version of GCP Secret Manager secret must not be null");

		this.projectId = projectId;
		this.secretId = secretId;
		this.version = version;
	}

	public String getProjectId() {
		return projectId;
	}

	public String getSecretId() {
		return secretId;
	}

	public String getVersion() {
		return version;
	}

	static SecretManagerPropertyIdentifier parseFromProperty(
			String property, GcpProjectIdProvider projectIdProvider) {

		if (!property.startsWith(GCP_SECRET_PREFIX)) {
			return null;
		}

		String resourcePath = property.substring(GCP_SECRET_PREFIX.length());
		String[] tokens = resourcePath.split("/");

		String projectId = projectIdProvider.getProjectId();
		String secretId = null;
		String version = "latest";

		if (tokens.length == 1) {
			// property is form "gcp-secret:<secret-id>"
			secretId = tokens[0];
		}
		else if (tokens.length == 2) {
			// property is form "gcp-secret:<project-id>/<secret-id>"
			projectId = tokens[0];
			secretId = tokens[1];
		}
		else if (tokens.length == 3) {
			// property is form "gcp-secret:<project-id>/<secret-id>/<version-id>"
			projectId = tokens[0];
			secretId = tokens[1];
			version = tokens[2];
		}
		else if (tokens.length == 4
				&& tokens[0].equals("projects")
				&& tokens[2].equals("secrets")) {
			// property is form "gcp-secret:projects/<project-id>/secrets/<secret-id>"
			projectId = tokens[1];
			secretId = tokens[3];
		}
		else if (tokens.length == 6
				&& tokens[0].equals("projects")
				&& tokens[2].equals("secrets")
				&& tokens[4].equals("versions")) {
			// property is form "gcp-secret:projects/<project-id>/secrets/<secret-id>/versions/<version>"
			projectId = tokens[1];
			secretId = tokens[3];
			version = tokens[5];
		}
		else {
			return null;
		}

		return new SecretManagerPropertyIdentifier(projectId, secretId, version);
	}
}
