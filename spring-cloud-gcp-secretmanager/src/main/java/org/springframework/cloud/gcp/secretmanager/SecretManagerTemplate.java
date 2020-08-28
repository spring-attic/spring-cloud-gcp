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
import com.google.cloud.secretmanager.v1beta1.DeleteSecretRequest;
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

	/**
	 * Default value for the latest version of the secret.
	 */
	public static final String LATEST_VERSION = "latest";

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
		createNewSecretVersion(secretId, ByteString.copyFromUtf8(payload), projectIdProvider.getProjectId());
	}

	@Override
	public void createSecret(String secretId, byte[] payload) {
		createNewSecretVersion(secretId, ByteString.copyFrom(payload), projectIdProvider.getProjectId());
	}

	@Override
	public void createSecret(String secretId, byte[] payload, String projectId) {
		createNewSecretVersion(secretId, ByteString.copyFrom(payload), projectId);
	}


	@Override
	public String getSecretString(String secretIdentifier) {
		return getSecretByteString(secretIdentifier).toStringUtf8();
	}

	@Override
	public byte[] getSecretBytes(String secretIdentifier) {
		return getSecretByteString(secretIdentifier).toByteArray();
	}

	@Override
	public boolean secretExists(String secretId) {
		return secretExists(secretId, this.projectIdProvider.getProjectId());
	}

	@Override
	public boolean secretExists(String secretId, String projectId) {
		SecretName secretName = SecretName.of(projectId, secretId);
		try {
			this.secretManagerServiceClient.getSecret(secretName);
		}
		catch (NotFoundException ex) {
			return false;
		}

		return true;
	}

	@Override
	public void disableSecretVersion(String secretId, String version) {
		disableSecretVersion(secretId, version, this.projectIdProvider.getProjectId());
	}

	@Override
	public void disableSecretVersion(String secretId, String version, String projectId) {
		SecretVersionName secretVersionName = SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();
		this.secretManagerServiceClient.disableSecretVersion(secretVersionName);
	}

	@Override
	public void enableSecretVersion(String secretId, String version) {
		enableSecretVersion(secretId, version, this.projectIdProvider.getProjectId());
	}

	@Override
	public void enableSecretVersion(String secretId, String version, String projectId) {
		SecretVersionName secretVersionName = SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();
		this.secretManagerServiceClient.enableSecretVersion(secretVersionName);
	}

	@Override
	public void deleteSecret(String secretId) {
		deleteSecret(secretId, this.projectIdProvider.getProjectId());
	}

	@Override
	public void deleteSecret(String secretId, String projectId) {
		SecretName name = SecretName.of(projectId, secretId);
		DeleteSecretRequest request = DeleteSecretRequest.newBuilder()
				.setName(name.toString())
				.build();
		this.secretManagerServiceClient.deleteSecret(request);
	}

	@Override
	public void deleteSecretVersion(String secretId, String version, String projectId) {
		SecretVersionName secretVersionName = SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();
		this.secretManagerServiceClient.destroySecretVersion(secretVersionName);
	}

	ByteString getSecretByteString(String secretIdentifier) {
		SecretVersionName secretVersionName =
				SecretManagerPropertyUtils.getSecretVersionName(secretIdentifier, projectIdProvider);

		if (secretVersionName == null) {
			secretVersionName = getDefaultSecretVersionName(secretIdentifier);
		}

		return getSecretByteString(secretVersionName);
	}

	ByteString getSecretByteString(SecretVersionName secretVersionName) {
		AccessSecretVersionResponse response =
				secretManagerServiceClient.accessSecretVersion(secretVersionName);
		return response.getPayload().getData();
	}

	/**
	 * Create a new version of the secret with the specified payload under a {@link Secret}.
	 * Will also create the parent secret if it does not already exist.
	 */
	private void createNewSecretVersion(String secretId, ByteString payload, String projectId) {
		if (!secretExists(secretId, projectId)) {
			createSecretInternal(secretId, projectId);
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
	private void createSecretInternal(String secretId, String projectId) {
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

	private SecretVersionName getDefaultSecretVersionName(String secretId) {
		return SecretVersionName.newBuilder()
				.setProject(this.projectIdProvider.getProjectId())
				.setSecret(secretId)
				.setSecretVersion(LATEST_VERSION)
				.build();
	}
}
