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

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1beta1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1beta1.ProjectName;
import com.google.cloud.secretmanager.v1beta1.Replication;
import com.google.cloud.secretmanager.v1beta1.Secret;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
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
 * @since 1.2.2
 */
public class SecretManagerTemplate implements SecretManagerOperations {

	private final SecretManagerServiceClient secretManagerServiceClient;

	private final GcpProjectIdProvider projectIdProvider;

	public SecretManagerTemplate(
			SecretManagerServiceClient secretManagerServiceClient,
			GcpProjectIdProvider projectIdProvider) {
		this.secretManagerServiceClient = secretManagerServiceClient;
		this.projectIdProvider = projectIdProvider;
	}

	@Override
	public void createSecret(String secretId, String payload) {
		createNewSecretVersionWithProject(secretId, ByteString.copyFromUtf8(payload), projectIdProvider.getProjectId());
	}

	@Override
	public void createSecret(String secretId, byte[] payload) {
		createNewSecretVersionWithProject(secretId, ByteString.copyFrom(payload), projectIdProvider.getProjectId());
	}

	@Override
	public String getSecretString(String secretId) {
		return getSecretString(secretId, "latest");
	}

	@Override
	public String getSecretString(String secretId, String versionName) {
		return getSecretByteString(secretId, versionName).toStringUtf8();
	}

	@Override
	public byte[] getSecretBytes(String secretId) {
		return getSecretBytes(secretId, "latest");
	}

	@Override
	public byte[] getSecretBytes(String secretId, String versionName) {
		return getSecretByteString(secretId, versionName).toByteArray();
	}

	@Override
	public ByteString getSecretByteString(String secretId, String versionName) {
		return getSecretByteStringWithProject(secretId, versionName, this.projectIdProvider.getProjectId());
	}

	@Override
	public boolean secretExists(String secretId) {
		return secretExistsWithProject(secretId, this.projectIdProvider.getProjectId());
	}

	/**
	 * Create a new version of the secret with the specified payload under a {@link Secret}.
	 * Will also create the parent secret if it does not already exist.
	 */
	private void createNewSecretVersionWithProject(String secretId, ByteString payload, String projectId) {
		if (!secretExistsWithProject(secretId, projectId)) {
			createSecretWithProject(secretId, projectId);
		}

		SecretName name = SecretName.of(projectId, secretId);
		AddSecretVersionRequest payloadRequest = AddSecretVersionRequest.newBuilder()
				.setParent(name.toString())
				.setPayload(SecretPayload.newBuilder().setData(payload))
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
	private void createSecretWithProject(String secretId, String projectId) {
		ProjectName projectName = ProjectName.of(projectId);

		Secret secret = Secret.newBuilder()
				.setReplication(
						Replication.newBuilder().setAutomatic(
								Replication.Automatic.getDefaultInstance()))
				.build();
		CreateSecretRequest request = CreateSecretRequest.newBuilder()
				.setParent(projectName.toString())
				.setSecretId(secretId)
				.setSecret(secret)
				.build();
		this.secretManagerServiceClient.createSecret(request);
	}

	@Override
	public void createSecretWithProject(String secretId, String payload, String projectId) {
		createNewSecretVersionWithProject(secretId, ByteString.copyFromUtf8(payload), projectId);
	}

	@Override
	public void createSecretWithProject(String secretId, byte[] payload, String projectId) {
		createNewSecretVersionWithProject(secretId, ByteString.copyFrom(payload), projectId);
	}

	@Override
	public String getSecretStringWithProject(String secretId, String projectId) {
		return getSecretStringWithProject(secretId, "latest", projectId);
	}

	@Override
	public String getSecretStringWithProject(String secretId, String versionName, String projectId) {
		return getSecretByteStringWithProject(secretId, versionName, projectId).toStringUtf8();
	}

	@Override
	public byte[] getSecretBytesWithProject(String secretId, String projectId) {
		return getSecretBytesWithProject(secretId, "latest", projectId);
	}

	@Override
	public byte[] getSecretBytesWithProject(String secretId, String versionName, String projectId) {
		return getSecretByteStringWithProject(secretId, versionName, projectId).toByteArray();
	}

	@Override
	public ByteString getSecretByteStringWithProject(String secretId, String versionName, String projectId) {
		SecretVersionName secretVersionName = SecretVersionName.of(
				projectId,
				secretId,
				versionName);

		AccessSecretVersionResponse response = secretManagerServiceClient.accessSecretVersion(secretVersionName);

		return response.getPayload().getData();
	}

	@Override
	public boolean secretExistsWithProject(String secretId, String projectId) {
		SecretName secretName = SecretName.of(projectId, secretId);
		try {
			this.secretManagerServiceClient.getSecret(secretName);
		}
		catch (NotFoundException ex) {
			return false;
		}

		return true;
	}
}
