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

package com.google.cloud.spring.kms;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.spring.core.GcpProjectIdProvider;

import org.springframework.util.Assert;

/**
 * Utilities for parsing KMS properties.
 *
 * @author Emmanouil Gkatziouras
 */
final class KmsPropertyUtils {

	public static final String LOCATION_GLOBAL = "global";

	private KmsPropertyUtils() { }

	static CryptoKeyName getCryptoKeyName(String input, GcpProjectIdProvider projectIdProvider) {

		String[] tokens = input.split("/");

		String projectId = projectIdProvider.getProjectId();
		String locationId;
		String keyRingId;
		String keyId;

		if (tokens.length == 2) {
			// property is form "<key-ring-id>/<key-id>"
			locationId = LOCATION_GLOBAL;
			keyRingId = tokens[0];
			keyId = tokens[1];
		}
		else if (tokens.length == 3) {
			// property is form "<location-id>/<key-ring-id>/<key-id>"
			locationId = tokens[0];
			keyRingId = tokens[1];
			keyId = tokens[2];
		}
		else if (tokens.length == 4) {
			// property is form "<project-id>/<location-id>/<key-ring-id>/<key-id>"
			projectId = tokens[0];
			locationId = tokens[1];
			keyRingId = tokens[2];
			keyId = tokens[3];
		}
		else if (tokens.length == 8
					&& tokens[0].equals("projects")
					&& tokens[2].equals("locations")
					&& tokens[4].equals("keyRings")
					&& tokens[6].equals("cryptoKeys")) {
			// property is form "projects/<project-id>/locations/<location-id>/keyRings/<key-ring-id>/cryptoKeys/<key-id>"
			projectId = tokens[1];
			locationId = tokens[3];
			keyRingId = tokens[5];
			keyId = tokens[7];
		}
		else {
			throw new IllegalArgumentException(
					"Unrecognized format for specifying a GCP KMS : " + input);
		}

		Assert.hasText(
				projectId, "The GCP KMS project id must not be empty: " + input);

		Assert.hasText(
				locationId, "The GCP KMS location id must not be empty: " + input);

		Assert.hasText(
				keyRingId, "The GCP KMS keyRing id must not be empty: " + input);

		Assert.hasText(
				keyId, "The GCP KMS key id must not be empty: " + input);

		return CryptoKeyName.newBuilder()
				.setProject(projectId)
				.setLocation(locationId)
				.setKeyRing(keyRingId)
				.setCryptoKey(keyId)
				.build();
	}
}
