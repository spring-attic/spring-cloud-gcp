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

import java.util.stream.StreamSupport;

import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1beta1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1beta1.ProjectName;
import com.google.cloud.secretmanager.v1beta1.Replication;
import com.google.cloud.secretmanager.v1beta1.Secret;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient.ListSecretsPagedResponse;
import com.google.cloud.secretmanager.v1beta1.SecretName;
import com.google.cloud.secretmanager.v1beta1.SecretPayload;
import com.google.cloud.secretmanager.v1beta1.SecretVersionName;
import com.google.protobuf.ByteString;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;

/**
 * Offers convenience methods for performing common operations on Secret Manager including
 * creating and reading secrets.
 *
 * @author Daniel Zou
 * @since 1.3
 */
public class SecretManagerTemplate {

	private final SecretManagerServiceClient secretManagerServiceClient;

	private final GcpProjectIdProvider projectIdProvider;

	public SecretManagerTemplate(
			SecretManagerServiceClient secretManagerServiceClient,
			GcpProjectIdProvider projectIdProvider) {
		this.secretManagerServiceClient = secretManagerServiceClient;
		this.projectIdProvider = projectIdProvider;
	}

	/**
	 * Creates a new secret using the provided {@code secretId} and creates a new version of
	 * the secret with the provided {@code payload}.
	 *
	 * <p>
	 * If there is already a secret saved in SecretManager with the specified
	 * {@code secretId}, then it simply creates a new version under the secret with the secret
	 * {@code payload}.
	 *
	 * @param secretId the secret ID of the secret to create.
	 * @param payload the secret payload string.
	 */
	public void createSecret(String secretId, String payload) {
		if (!secretExists(secretId)) {
			createSecret(secretId);
		}

		createNewSecretVersion(secretId, ByteString.copyFromUtf8(payload));
	}

	/**
	 * Creates a new secret using the provided {@code secretId} and creates a new version of
	 * the secret with the provided {@code payload}.
	 *
	 * <p>
	 * If there is already a secret saved in SecretManager with the specified
	 * {@code secretId}, then it simply creates a new version under the secret with the secret
	 * {@code payload}.
	 *
	 * @param secretId the secret ID of the secret to create.
	 * @param payload the secret payload as a byte array.
	 */
	public void createSecret(String secretId, byte[] payload) {
		if (!secretExists(secretId)) {
			createSecret(secretId);
		}

		createNewSecretVersion(secretId, ByteString.copyFrom(payload));
	}

	/**
	 * Gets the secret payload of the specified {@code secretId} at version
	 * {@code versionName}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @param versionName which version of the secret to load. The version can be a version
	 *     number as a string (e.g. "5") or an alias (e.g. "latest").
	 * @return The secret payload as String
	 */
	public String getSecretString(String secretId, String versionName) {
		return getSecretByteString(secretId, versionName).toStringUtf8();
	}

	/**
	 * Gets the secret payload of the specified {@code secretId} at version
	 * {@code versionName}.
	 *
	 * @param secretId unique identifier of your secret in Secret Manager.
	 * @param versionName which version of the secret to load. The version can be a version
	 *     number as a string (e.g. "5") or an alias (e.g. "latest").
	 * @return The secret payload as byte[]
	 */
	public byte[] getSecretBytes(String secretId, String versionName) {
		return getSecretByteString(secretId, versionName).toByteArray();
	}

	public ByteString getSecretByteString(String secretId, String versionName) {
		SecretVersionName secretVersionName = SecretVersionName.of(
				this.projectIdProvider.getProjectId(),
				secretId,
				versionName);

		AccessSecretVersionResponse response = secretManagerServiceClient.accessSecretVersion(secretVersionName);

		return response.getPayload().getData();
	}

	/**
	 * Create a new version of the secret with the specified payload under a {@link Secret}.
	 */
	private void createNewSecretVersion(String secretId, ByteString byteStringPayload) {
		SecretName name = SecretName.of(projectIdProvider.getProjectId(), secretId);
		SecretPayload payloadObject = SecretPayload.newBuilder()
				.setData(byteStringPayload)
				.build();

		AddSecretVersionRequest payloadRequest = AddSecretVersionRequest.newBuilder()
				.setParent(name.toString())
				.setPayload(payloadObject)
				.build();
		secretManagerServiceClient.addSecretVersion(payloadRequest);
	}

	/**
	 * Creates a new secret for the GCP Project.
	 *
	 * <p>
	 * Note that the {@link Secret} object does not contain the secret payload. You must
	 * create versions of the secret which stores the payload of the secret.
	 */
	private void createSecret(String secretId) {
		ProjectName projectName = ProjectName.of(projectIdProvider.getProjectId());

		Secret secret = Secret.newBuilder()
				.setReplication(
						Replication.newBuilder()
								.setAutomatic(Replication.Automatic.newBuilder().build())
								.build())
				.build();
		CreateSecretRequest request = CreateSecretRequest.newBuilder()
				.setParent(projectName.toString())
				.setSecretId(secretId)
				.setSecret(secret)
				.build();
		this.secretManagerServiceClient.createSecret(request);
	}

	/**
	 * Returns true if there already exists a secret under the GCP project with the
	 * {@code secretId}.
	 */
	private boolean secretExists(String secretId) {
		ProjectName projectName = ProjectName.of(this.projectIdProvider.getProjectId());
		ListSecretsPagedResponse listSecretsResponse = this.secretManagerServiceClient.listSecrets(projectName);

		return StreamSupport.stream(listSecretsResponse.iterateAll().spliterator(), false)
				.anyMatch(secret -> secret.getName().contains(secretId));
	}
}
