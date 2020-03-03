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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1beta1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1beta1.Replication;
import com.google.cloud.secretmanager.v1beta1.Secret;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient.ListSecretsPagedResponse;
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
		Secret secretToAdd = Secret.newBuilder()
				.setReplication(
						Replication.newBuilder()
								.setAutomatic(Replication.Automatic.newBuilder()).build())
				.build();

		CreateSecretRequest createSecretRequest = CreateSecretRequest.newBuilder()
				.setParent("projects/my-project")
				.setSecretId("my-secret")
				.setSecret(secretToAdd)
				.build();
		verify(this.client).createSecret(createSecretRequest);

		// Verifies the secret payload is added correctly.
		AddSecretVersionRequest addSecretVersionRequest = AddSecretVersionRequest.newBuilder()
				.setParent("projects/my-project/secrets/my-secret")
				.setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("hello world!")))
				.build();
		verify(this.client).addSecretVersion(addSecretVersionRequest);
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

		// Verifies the secret payload is added correctly.
		AddSecretVersionRequest addSecretVersionRequest = AddSecretVersionRequest.newBuilder()
				.setParent("projects/my-project/secrets/my-secret")
				.setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("hello world!")))
				.build();
		verify(this.client).addSecretVersion(addSecretVersionRequest);
	}

	@Test
	public void testAccessSecretBytes() {
		byte[] result = this.secretManagerTemplate.getSecretBytes("my-secret");
		verify(this.client).accessSecretVersion(
				SecretVersionName.of("my-project", "my-secret", "latest"));
		assertThat(result).isEqualTo("get after it.".getBytes());

		result = this.secretManagerTemplate.getSecretBytes("my-secret", "1");
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

		result = this.secretManagerTemplate.getSecretString("my-secret", "1");
		verify(this.client).accessSecretVersion(
				SecretVersionName.of("my-project", "my-secret", "1"));
		assertThat(result).isEqualTo("get after it.");
	}

	private ListSecretsPagedResponse createListSecretsResponse(String... secretIds) {
		ListSecretsPagedResponse listResponse = mock(ListSecretsPagedResponse.class);

		List<Secret> secrets = Arrays.stream(secretIds)
				.map(secretId -> Secret.newBuilder().setName(secretId).build())
				.collect(Collectors.toList());

		when(listResponse.iterateAll()).thenReturn(secrets);
		return listResponse;
	}
}
