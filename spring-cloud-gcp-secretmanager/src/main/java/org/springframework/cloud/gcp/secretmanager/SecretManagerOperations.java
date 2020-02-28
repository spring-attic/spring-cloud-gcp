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

import com.google.protobuf.ByteString;

/**
 * Describes supported operations that one can perform on the Secret Manager API.
 *
 * @author Daniel Zou
 * @since 1.2.2
 */
public interface SecretManagerOperations {

	/**
	 * Creates a new secret or a new version of existing secret with the provided
	 * {@code payload}.
	 *
	 * <p>
	 * If there is already a secret saved in SecretManager with the specified
	 * {@code secretId}, then it simply creates a new version under the secret with the secret
	 * {@code payload}.
	 *
	 * @param secretId the secret ID of the secret to create.
	 * @param payload the secret payload string.
	 */
	void createSecret(String secretId, String payload);

	/**
	 * Creates a new secret or a new version of existing secret with the provided
	 * {@code payload}.
	 *
	 * <p>
	 * If there is already a secret saved in SecretManager with the specified
	 * {@code secretId}, then it simply creates a new version under the secret with the secret
	 * {@code payload}.
	 *
	 * @param secretId the secret ID of the secret to create.
	 * @param payload the secret payload as a byte array.
	 */
	void createSecret(String secretId, byte[] payload);

	/**
	 * Gets the secret payload of the specified {@code secretId} at the latest version.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @return The secret payload as String
	 */
	String getSecretString(String secretId);

	/**
	 * Gets the secret payload of the specified {@code secretId} at version
	 * {@code versionName}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @param versionName which version of the secret to load. The version can be a version
	 *     number as a string (e.g. "5") or an alias (e.g. "latest").
	 * @return The secret payload as String
	 */
	String getSecretString(String secretId, String versionName);

	/**
	 * Gets the secret payload of the specified {@code secretId} at the latest version.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @return The secret payload as byte[]
	 */
	byte[] getSecretBytes(String secretId);

	/**
	 * Gets the secret payload of the specified {@code secretId} at version
	 * {@code versionName}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @param versionName which version of the secret to load. The version can be a version
	 *     number as a string (e.g. "5") or an alias (e.g. "latest").
	 * @return The secret payload as byte[]
	 */
	byte[] getSecretBytes(String secretId, String versionName);

	/**
	 * Gets the secret payload of the specified {@code secretId} at version
	 * {@code versionName}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @param versionName which version of the secret to load. The version can be a version
	 *     number as a string (e.g. "5") or an alias (e.g. "latest").
	 * @return The secret payload as {@link ByteString}
	 */
	ByteString getSecretByteString(String secretId, String versionName);

	/**
	 * Returns true if there already exists a secret under the GCP project with the
	 * {@code secretId}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @return true if the secret exists in Secret Manager; false otherwise
	 */
	boolean secretExists(String secretId);
}
