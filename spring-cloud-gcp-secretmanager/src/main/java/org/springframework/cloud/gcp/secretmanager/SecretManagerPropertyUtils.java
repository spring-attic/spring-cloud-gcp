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

package org.springframework.cloud.gcp.secretmanager;

import com.google.cloud.secretmanager.v1beta1.SecretVersionName;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utilities for parsing Secret Manager properties.
 *
 * @author Daniel Zou
 */
final class SecretManagerPropertyUtils {

	private static final String GCP_SECRET_PREFIX = "sm://";

	private SecretManagerPropertyUtils() { }

	static SecretVersionName getSecretVersionName(String input, GcpProjectIdProvider projectIdProvider) {
		if (!input.startsWith(GCP_SECRET_PREFIX)) {
			return null;
		}

		String resourcePath = input.substring(GCP_SECRET_PREFIX.length());
		String[] tokens = resourcePath.split("/");

		String projectId = projectIdProvider.getProjectId();
		String secretId = null;
		String version = "latest";

		if (tokens.length == 1) {
			// property is form "sm://<secret-id>"
			secretId = tokens[0];
		}
		else if (tokens.length == 2) {
			// property is form "sm://<secret-id>/<version>"
			secretId = tokens[0];
			version = tokens[1];
		}
		else if (tokens.length == 3) {
			// property is form "sm://<project-id>/<secret-id>/<version-id>"
			projectId = tokens[0];
			secretId = tokens[1];
			version = tokens[2];
		}
		else if (tokens.length == 4
				&& tokens[0].equals("projects")
				&& tokens[2].equals("secrets")) {
			// property is form "sm://projects/<project-id>/secrets/<secret-id>"
			projectId = tokens[1];
			secretId = tokens[3];
		}
		else if (tokens.length == 6
				&& tokens[0].equals("projects")
				&& tokens[2].equals("secrets")
				&& tokens[4].equals("versions")) {
			// property is form "sm://projects/<project-id>/secrets/<secret-id>/versions/<version>"
			projectId = tokens[1];
			secretId = tokens[3];
			version = tokens[5];
		}
		else {
			throw new IllegalArgumentException(
					"Unrecognized format for specifying a GCP Secret Manager secret: " + input);
		}

		Assert.isTrue(
				!StringUtils.isEmpty(secretId),
				"The GCP Secret Manager secret id must not be empty: " + input);

		Assert.isTrue(
				!StringUtils.isEmpty(projectId),
				"The GCP Secret Manager project id must not be empty: " + input);

		Assert.isTrue(
				!StringUtils.isEmpty(version),
				"The GCP Secret Manager secret version must not be empty: " + input);

		return SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();
	}
}
