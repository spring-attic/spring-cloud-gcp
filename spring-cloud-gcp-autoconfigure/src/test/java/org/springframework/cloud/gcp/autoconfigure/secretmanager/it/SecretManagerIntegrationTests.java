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

import java.util.concurrent.TimeUnit;

import com.google.api.gax.rpc.NotFoundException;
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
import io.grpc.StatusRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Duration;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.awaitility.Awaitility.await;

public class SecretManagerIntegrationTests {

	private static final Log LOGGER = LogFactory.getLog(SecretManagerIntegrationTests.class);

	private static final String TEST_SECRET_ID = "spring-cloud-gcp-it-secret";

	private static final String VERSIONED_SECRET_ID = "spring-cloud-gcp-it-versioned-secret";

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
		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.run();

		this.projectIdProvider = context.getBeanFactory().getBean(GcpProjectIdProvider.class);
		this.client = context.getBeanFactory().getBean(SecretManagerServiceClient.class);

		// Clean up the test secrets in the project from prior runs.
		deleteSecret(TEST_SECRET_ID);
		deleteSecret(VERSIONED_SECRET_ID);
	}

	@Test
	public void testConfiguration() {
		createSecret(TEST_SECRET_ID, "the secret data.");

		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.run();

		assertThat(context.getEnvironment().getProperty("sm://" + TEST_SECRET_ID))
				.isEqualTo("the secret data.");

		byte[] byteArraySecret = context.getEnvironment().getProperty(
				"sm://" + TEST_SECRET_ID + "/latest", byte[].class);
		assertThat(byteArraySecret).isEqualTo("the secret data.".getBytes());
	}

	@Test
	public void testConfigurationDisabled() {
		createSecret(TEST_SECRET_ID, "the secret data.");

		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.properties("spring.cloud.gcp.secretmanager.enabled=false")
				.run();

		assertThat(context.getEnvironment().getProperty(
				"sm://" + TEST_SECRET_ID, String.class)).isNull();
	}

	@Test
	public void testSecretsWithSpecificVersion() {
		createSecret(VERSIONED_SECRET_ID, "the secret data");
		createSecret(VERSIONED_SECRET_ID, "the secret data v2");
		createSecret(VERSIONED_SECRET_ID, "the secret data v3");

		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.run();

		String versionedSecret = context.getEnvironment().getProperty(
				"sm://" + VERSIONED_SECRET_ID + "/2", String.class);
		assertThat(versionedSecret).isEqualTo("the secret data v2");
	}

	@Test
	public void testMissingSecret() {
		createSecret(VERSIONED_SECRET_ID, "the secret data");

		ConfigurableApplicationContext context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.run();

		assertThatThrownBy(() ->
				context.getEnvironment().getProperty("sm://" + VERSIONED_SECRET_ID + "/2", String.class))
				.hasCauseInstanceOf(StatusRuntimeException.class)
				.hasMessageContaining("NOT_FOUND");
	}
	/**
	 * Creates the secret with the specified payload if the secret does not already exist.
	 * Otherwise creates a new version of the secret under the existing {@code secretId}.
	 */
	private void createSecret(String secretId, String payload) {
		ProjectName projectName = ProjectName.of(projectIdProvider.getProjectId());

		if (!secretExists(secretId)) {
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
		}

		createSecretPayload(secretId, payload);
	}

	private void createSecretPayload(String secretId, String data) {
		// Create the secret payload.
		SecretName name = SecretName.of(projectIdProvider.getProjectId(), secretId);
		SecretPayload payloadObject = SecretPayload.newBuilder()
				.setData(ByteString.copyFromUtf8(data))
				.build();
		AddSecretVersionRequest payloadRequest = AddSecretVersionRequest.newBuilder()
				.setParent(name.toString())
				.setPayload(payloadObject)
				.build();
		client.addSecretVersion(payloadRequest);
	}

	private boolean secretExists(String secretId) {
		try {
			SecretVersionName secretVersionName =
					SecretVersionName.newBuilder()
							.setProject(projectIdProvider.getProjectId())
							.setSecret(secretId)
							.setSecretVersion("latest")
							.build();
			this.client.accessSecretVersion(secretVersionName);
		}
		catch (NotFoundException e) {
			return false;
		}

		return true;
	}

	private void deleteSecret(String secretId) {
		try {
			this.client.deleteSecret(SecretName.of(this.projectIdProvider.getProjectId(), secretId));

			// Wait for the secret to be successfully removed from the project in the backend.
			await().pollInterval(Duration.ONE_SECOND)
					.atMost(20, TimeUnit.SECONDS).until(() -> !secretExists(secretId));
		}
		catch (NotFoundException e) {
			LOGGER.debug("Skipped deleting " + secretId + " because it does not exist.");
		}
	}
}
