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

/**
 * Describes supported operations that one can perform on the Secret Manager API.
 *
 * <p>For some methods you may specify the secret from GCP Secret Manager by URI string.
 * The following secret URI syntax is supported:
 *
 * 1. Long form - specify the project ID, secret ID, and version
 * sm://projects/{project-id}/secrets/{secret-id}/versions/{version-id}
 *
 * 2.  Long form - specify project ID, secret ID, and use latest version
 * sm://projects/{project-id}/secrets/{secret-id}
 *
 * 3. Short form - specify project ID, secret ID, and version
 * sm://{project-id}/{secret-id}/{version-id}
 *
 * 4. Short form - specify secret and version, use default GCP project configured
 * sm://{secret-id}/{version}
 *
 * 5. Shortest form - specify secret ID, use default project and latest version.
 * sm://{secret-id}
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
	 * Creates a new secret or a new version of existing secret with the provided
	 * {@code payload} for a specific {@code projectId}.
	 *
	 * <p>
	 * If there is already a secret saved in SecretManager with the specified
	 * {@code secretId}, then it simply creates a new version under the secret with the secret
	 * {@code payload}.
	 *
	 * @param secretId the secret ID of the secret to create.
	 * @param payload the secret payload as a byte array.
	 * @param projectId unique identifier of your project.
	 */
	void createSecret(String secretId, byte[] payload, String projectId);

	/**
	 * Enables the specified secret version under the default-configured project.
	 *
	 * @param secretId the secret ID of the secret to enable.
	 * @param version the version to enable
	 */
	void enableSecretVersion(String secretId, String version);

	/**
	 * Enables the secret version under the specified project.
	 *
	 * @param secretId the secret ID of the secret to enable.
	 * @param version the version to enable
	 * @param projectId unique identifier of your project.
	 */
	void enableSecretVersion(String secretId, String version, String projectId);

	/**
	 * Disables the specified secret version under the default-configured project.
	 *
	 * @param secretId the secret ID of the secret to disable.
	 * @param version the version to disable
	 */
	void disableSecretVersion(String secretId, String version);

	/**
	 * Disables the secret version under the specified project.
	 *
	 * @param secretId the secret ID of the secret to disable.
	 * @param version the version to disable
	 * @param projectId unique identifier of your project.
	 */
	void disableSecretVersion(String secretId, String version, String projectId);

	/**
	 * Deletes the specified secret under the default-configured project.
	 *
	 * @param secretId the secret ID of the secret to delete.
	 */
	void deleteSecret(String secretId);

	/**
	 * Deletes the specified secret.
	 *
	 * @param secretId the secret ID of the secret to delete.
	 * @param projectId the GCP project containing the secret to delete.
	 */
	void deleteSecret(String secretId, String projectId);

	/**
	 * Deletes the specified secret version.
	 *
	 * @param secretId the secret ID of the secret to delete.
	 * @param version the version to delete
	 * @param projectId the GCP project containing the secret to delete.
	 */
	void deleteSecretVersion(String secretId, String version, String projectId);

	/**
	 * Gets the secret payload of the specified {@code secretIdentifier} secret.
	 *
	 * <p>The {@code secretIdentifier} must either be a secret ID or a fully qualified
	 * `sm://` protocol string which specifies the secret (see javadocs of
	 * {@link SecretManagerOperations} for the protocol format).
	 *
	 * If the secret ID string is passed in, then this will return the payload of the secret for
	 * the default project at the latest version.
	 *
	 * @param secretIdentifier the GCP secret ID of the secret or an sm:// formatted
	 * 		string specifying the secret.
	 * @return The secret payload as String
	 */
	String getSecretString(String secretIdentifier);

	/**
	 * Gets the secret payload of the specified {@code secretIdentifier} secret.
	 *
	 * <p>The {@code secretIdentifier} must either be a secret ID or a fully qualified
	 * `sm://` protocol string which specifies the secret (see javadocs of
	 * {@link SecretManagerOperations} for the protocol format).
	 *
	 * If the secret ID string is passed in, then this will return the payload of the secret for
	 * the default project at the latest version.
	 *
	 * @param secretIdentifier the GCP secret ID of the secret or an sm:// formatted
	 * 		string specifying the secret.
	 * @return The secret payload as byte array
	 */
	byte[] getSecretBytes(String secretIdentifier);

	/**
	 * Returns true if there already exists a secret under the GCP project with the
	 * {@code secretId}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @return true if the secret exists in Secret Manager; false otherwise
	 */
	boolean secretExists(String secretId);

	/**
	 * Returns true if there already exists a secret under the GCP {@code projectId} with the
	 * {@code secretId}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @param projectId unique identifier of your project.
	 * @return true if the secret exists in Secret Manager; false otherwise
	 */
	boolean secretExists(String secretId, String projectId);
}
