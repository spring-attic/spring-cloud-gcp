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

package org.springframework.cloud.gcp.autoconfigure.secretmanager.it;

import java.util.stream.StreamSupport;

import com.google.cloud.secretmanager.v1beta1.AddSecretVersionRequest;
import com.google.cloud.secretmanager.v1beta1.CreateSecretRequest;
import com.google.cloud.secretmanager.v1beta1.ProjectName;
import com.google.cloud.secretmanager.v1beta1.Replication;
import com.google.cloud.secretmanager.v1beta1.Secret;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient.ListSecretsPagedResponse;
import com.google.cloud.secretmanager.v1beta1.SecretName;
import com.google.cloud.secretmanager.v1beta1.SecretPayload;
import com.google.protobuf.ByteString;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.secretmanager.GcpSecretManagerBootstrapConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class SecretManagerIntegrationTests {

	private static final String TEST_SECRET_ID = "my-secret";

	private ConfigurableApplicationContext context;

	private GcpProjectIdProvider projectIdProvider;

	private SecretManagerServiceClient client;

	@BeforeClass
	public static void prepare() {
		assumeThat(System.getProperty("it.secretmanager"))
				.as("Secret manager integration tests are disabled. "
						+ "Please use '-Dit.secretmanager=true' to enable them.")
				.isEqualTo("true");
	}

	@Before
	public void setupSecretManager() {
		this.context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.properties("spring.cloud.gcp.secretmanager.bootstrap.enabled=true")
				.run();

		this.projectIdProvider = this.context.getBeanFactory().getBean(GcpProjectIdProvider.class);
		this.client = this.context.getBeanFactory().getBean(SecretManagerServiceClient.class);

		createSecret(TEST_SECRET_ID, "the secret data");
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testConfiguration() {
		assertThat(this.context.getEnvironment().getProperty("my-secret"))
				.isEqualTo("the secret data.");

		byte[] byteArraySecret = this.context.getEnvironment().getProperty("my-secret", byte[].class);
		assertThat(byteArraySecret).isEqualTo("the secret data.".getBytes());
	}

	@Test
	public void testConfigurationDisabled() {
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.properties("spring.cloud.gcp.secretmanager.enabled=false")
				.run();

		assertThat(context.getEnvironment()
				.getProperty("spring-cloud-gcp.secrets.my-secret")).isNull();
	}

	/**
	 * Creates the secret with the specified payload if the secret does not already exist.
	 */
	private void createSecret(String secretId, String payload) {
		if (!secretExists(secretId)) {
			ProjectName projectName = ProjectName.of(projectIdProvider.getProjectId());

			// Creates the secret.
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
			client.createSecret(request);

			// Create the secret payload.
			SecretName name = SecretName.of(projectIdProvider.getProjectId(), TEST_SECRET_ID);
			SecretPayload payloadObject = SecretPayload.newBuilder()
					.setData(ByteString.copyFromUtf8("the secret data."))
					.build();
			AddSecretVersionRequest payloadRequest = AddSecretVersionRequest.newBuilder()
					.setParent(name.toString())
					.setPayload(payloadObject)
					.build();
			client.addSecretVersion(payloadRequest);
		}
	}

	private boolean secretExists(String secretId) {
		ProjectName projectName = ProjectName.of(projectIdProvider.getProjectId());
		ListSecretsPagedResponse listSecretsResponse = this.client.listSecrets(projectName);
		return StreamSupport.stream(listSecretsResponse.iterateAll().spliterator(), false)
				.anyMatch(secret -> secret.getName().contains(secretId));
	}
}
