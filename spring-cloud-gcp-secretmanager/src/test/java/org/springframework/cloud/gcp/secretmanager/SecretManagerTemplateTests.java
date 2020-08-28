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
import com.google.cloud.secretmanager.v1beta1.Replication;
import com.google.cloud.secretmanager.v1beta1.Secret;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretName;
import com.google.cloud.secretmanager.v1beta1.SecretPayload;
import com.google.cloud.secretmanager.v1beta1.SecretVersionName;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SecretManagerTemplateTests {

	private SecretManagerServiceClient client;

	private SecretManagerTemplate secretManagerTemplate;

	@Before
	public void setupMocks() {
		this.client = mock(SecretManagerServiceClient.class);
		when(this.client.accessSecretVersion(any(SecretVersionName.class)))
				.thenReturn(
						AccessSecretVersionResponse.newBuilder()
								.setPayload(SecretPayload.newBuilder()
										.setData(ByteString.copyFromUtf8("get after it.")))
								.build());

		this.secretManagerTemplate = new SecretManagerTemplate(this.client, () -> "my-project");
	}

	@Test
	public void testCreateSecretIfMissing() {
		// This means that no previous secrets exist.
		when(this.client.getSecret(any(SecretName.class))).thenThrow(NotFoundException.class);

		this.secretManagerTemplate.createSecret("my-secret", "hello world!");

		// Verify the secret is created correctly.
		verifyCreateSecretRequest("my-secret", "my-project");

		// Verifies the secret payload is added correctly.
		verifyAddSecretRequest("my-secret", "hello world!", "my-project");
	}

	@Test
	public void testCreateSecretIfMissing_withProject() {
		when(this.client.getSecret(any(SecretName.class))).thenThrow(NotFoundException.class);

		this.secretManagerTemplate.createSecret(
				"my-secret", "hello world!".getBytes(), "custom-project");

		verifyCreateSecretRequest("my-secret", "custom-project");
		verifyAddSecretRequest("my-secret", "hello world!", "custom-project");
	}

	@Test
	public void testCreateSecretIfAlreadyPresent() {
		// The secret 'my-secret' already exists.
		when(this.client.getSecret(SecretName.of("my-project", "my-secret")))
				.thenReturn(Secret.getDefaultInstance());

		// Verify that the secret is not created.
		this.secretManagerTemplate.createSecret("my-secret", "hello world!");
		verify(this.client).getSecret(SecretName.of("my-project", "my-secret"));
		verify(this.client, never()).createSecret(any());
		verifyAddSecretRequest("my-secret", "hello world!", "my-project");
	}

	@Test
	public void testCreateSecretIfAlreadyPresent_withProject() {
		when(this.client.getSecret(SecretName.of("custom-project", "my-secret")))
				.thenReturn(Secret.getDefaultInstance());

		this.secretManagerTemplate.createSecret(
				"my-secret", "hello world!".getBytes(), "custom-project");
		verify(this.client).getSecret(SecretName.of("custom-project", "my-secret"));
		verify(this.client, never()).createSecret(any());
		verifyAddSecretRequest("my-secret", "hello world!", "custom-project");
	}

	@Test
	public void testCreateByteSecretIfMissing() {
		// This means that no previous secrets exist.
		when(this.client.getSecret(any(SecretName.class))).thenThrow(NotFoundException.class);

		this.secretManagerTemplate.createSecret("my-secret", "hello world!".getBytes());

		verifyCreateSecretRequest("my-secret", "my-project");
		verifyAddSecretRequest("my-secret", "hello world!", "my-project");
	}

	@Test
	public void testCreateByteSecretIfMissing_withProject() {
		// This means that no previous secrets exist.
		when(this.client.getSecret(any(SecretName.class))).thenThrow(NotFoundException.class);

		this.secretManagerTemplate.createSecret("my-secret", "hello world!".getBytes(), "custom-project");

		verifyCreateSecretRequest("my-secret", "custom-project");
		verifyAddSecretRequest("my-secret", "hello world!", "custom-project");
	}

	@Test
	public void testCreateByteSecretIfAlreadyPresent() {
		// The secret 'my-secret' already exists.
		when(this.client.getSecret(SecretName.of("my-project", "my-secret")))
				.thenReturn(Secret.getDefaultInstance());

		// Verify that the secret is not created.
		this.secretManagerTemplate.createSecret("my-secret", "hello world!".getBytes());
		verify(this.client).getSecret(SecretName.of("my-project", "my-secret"));
		verify(this.client, never()).createSecret(any());
		verifyAddSecretRequest("my-secret", "hello world!", "my-project");
	}

	@Test
	public void testCreateByteSecretIfAlreadyPresent_withProject() {
		// The secret 'my-secret' already exists.
		when(this.client.getSecret(SecretName.of("custom-project", "my-secret")))
				.thenReturn(Secret.getDefaultInstance());

		// Verify that the secret is not created.
		this.secretManagerTemplate.createSecret("my-secret", "hello world!".getBytes(), "custom-project");
		verify(this.client).getSecret(SecretName.of("custom-project", "my-secret"));
		verify(this.client, never()).createSecret(any());
		verifyAddSecretRequest("my-secret", "hello world!", "custom-project");
	}

	@Test
	public void testAccessSecretBytes() {
		byte[] result = this.secretManagerTemplate.getSecretBytes("my-secret");
		verify(this.client).accessSecretVersion(
				SecretVersionName.of("my-project", "my-secret", "latest"));
		assertThat(result).isEqualTo("get after it.".getBytes());

		result = this.secretManagerTemplate.getSecretBytes("sm://my-secret/1");
		verify(this.client).accessSecretVersion(
				SecretVersionName.of("my-project", "my-secret", "1"));
		assertThat(result).isEqualTo("get after it.".getBytes());
	}

	@Test
	public void testAccessSecretString() {
		String result = this.secretManagerTemplate.getSecretString("my-secret");
		verify(this.client).accessSecretVersion(
				SecretVersionName.of("my-project", "my-secret", "latest"));
		assertThat(result).isEqualTo("get after it.");

		result = this.secretManagerTemplate.getSecretString("sm://my-secret/1");
		verify(this.client).accessSecretVersion(
				SecretVersionName.of("my-project", "my-secret", "1"));
		assertThat(result).isEqualTo("get after it.");
	}

	@Test
	public void testEnableSecretVersion() {
		this.secretManagerTemplate.enableSecretVersion("my-secret", "1");
		verifyEnableSecretVersionRequest("my-secret", "1", "my-project");

		this.secretManagerTemplate.enableSecretVersion("my-secret", "1", "custom-project");
		verifyEnableSecretVersionRequest("my-secret", "1", "custom-project");
	}

	@Test
	public void testDeleteSecret() {
		this.secretManagerTemplate.deleteSecret("my-secret");
		verifyDeleteSecretRequest("my-secret", "my-project");

		this.secretManagerTemplate.deleteSecret("my-secret", "custom-project");
		verifyDeleteSecretRequest("my-secret", "custom-project");
	}

	@Test
	public void testDeleteSecretVersion() {
		this.secretManagerTemplate.deleteSecretVersion("my-secret", "10", "custom-project");
		verifyDeleteSecretVersionRequest("my-secret", "10", "custom-project");
	}

	@Test
	public void testDisableSecretVersion() {
		this.secretManagerTemplate.disableSecretVersion("my-secret", "1");
		verifyDisableSecretVersionRequest("my-secret", "1", "my-project");

		this.secretManagerTemplate.disableSecretVersion("my-secret", "1", "custom-project");
		verifyDisableSecretVersionRequest("my-secret", "1", "custom-project");
	}

	private void verifyCreateSecretRequest(String secretId, String projectId) {
		Secret secretToAdd = Secret.newBuilder()
				.setReplication(
						Replication.newBuilder()
								.setAutomatic(Replication.Automatic.newBuilder()).build())
				.build();

		CreateSecretRequest createSecretRequest = CreateSecretRequest.newBuilder()
				.setParent("projects/" + projectId)
				.setSecretId(secretId)
				.setSecret(secretToAdd)
				.build();

		verify(this.client).createSecret(createSecretRequest);
	}

	private void verifyAddSecretRequest(String secretId, String payload, String projectId) {
		AddSecretVersionRequest addSecretVersionRequest = AddSecretVersionRequest.newBuilder()
				.setParent("projects/" + projectId + "/secrets/" + secretId)
				.setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8(payload)))
				.build();
		verify(this.client).addSecretVersion(addSecretVersionRequest);
	}

	private void verifyEnableSecretVersionRequest(String secretId, String version, String projectId) {
		SecretVersionName secretVersionName = SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();
		verify(this.client).enableSecretVersion(secretVersionName);
	}

	private void verifyDeleteSecretRequest(String secretId, String projectId) {
		SecretName name = SecretName.of(projectId, secretId);
		DeleteSecretRequest request = DeleteSecretRequest.newBuilder()
				.setName(name.toString())
				.build();
		verify(this.client).deleteSecret(request);
	}

	private void verifyDeleteSecretVersionRequest(String secretId, String version, String projectId) {
		SecretVersionName secretVersionName = SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();
		verify(this.client).destroySecretVersion(secretVersionName);
	}

	private void verifyDisableSecretVersionRequest(String secretId, String version, String projectId) {
		SecretVersionName secretVersionName = SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();
		verify(this.client).disableSecretVersion(secretVersionName);
	}
}
