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

import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1beta1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1beta1.ProjectName;
import com.google.cloud.secretmanager.v1beta1.Replication;
import com.google.cloud.secretmanager.v1beta1.Secret;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient.ListSecretsPagedResponse;
import com.google.cloud.secretmanager.v1beta1.SecretPayload;
import com.google.cloud.secretmanager.v1beta1.SecretVersionName;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;

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
		ListSecretsPagedResponse response = createListSecretsResponse();
		when(this.client.listSecrets(any(ProjectName.class))).thenReturn(response);
		when(this.client.accessSecretVersion(any(SecretVersionName.class)))
				.thenReturn(AccessSecretVersionResponse.getDefaultInstance());

		this.secretManagerTemplate = new SecretManagerTemplate(this.client, () -> "my-project");
	}

	@Test
	public void testCreateSecretIfMissing() {
		this.secretManagerTemplate.createSecret("my-secret", "hello world!");

		verify(this.client).listSecrets(ProjectName.of("my-project"));

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
		ListSecretsPagedResponse response = createListSecretsResponse("my-secret");
		when(this.client.listSecrets(any(ProjectName.class))).thenReturn(response);

		// Verify that the secret is not created.
		this.secretManagerTemplate.createSecret("my-secret", "hello world!");
		verify(this.client).listSecrets(ProjectName.of("my-project"));
		verify(this.client, never()).createSecret(any());

		// Verifies the secret payload is added correctly.
		AddSecretVersionRequest addSecretVersionRequest = AddSecretVersionRequest.newBuilder()
				.setParent("projects/my-project/secrets/my-secret")
				.setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("hello world!")))
				.build();
		verify(this.client).addSecretVersion(addSecretVersionRequest);
	}

	@Test
	public void testAccessSecret() {
		this.secretManagerTemplate.getSecretString("my-secret", "latest");
		verify(this.client).accessSecretVersion(
				SecretVersionName.of("my-project", "my-secret", "latest"));
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
