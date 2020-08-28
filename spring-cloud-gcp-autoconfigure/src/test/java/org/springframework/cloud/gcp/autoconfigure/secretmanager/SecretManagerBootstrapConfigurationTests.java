/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.secretmanager;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretPayload;
import com.google.cloud.secretmanager.v1beta1.SecretVersionName;
import com.google.protobuf.ByteString;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GcpSecretManagerBootstrapConfiguration}.
 *
 * @author Mike Eltsufin
 * @author Daniel Zou
 */
public class SecretManagerBootstrapConfigurationTests {

	private static final String PROJECT_NAME = "hollow-light-of-the-sealed-land";

	private SpringApplicationBuilder applicationBuilder =
			new SpringApplicationBuilder(
					TestBootstrapConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
					.properties(
							"spring.cloud.gcp.secretmanager.project-id=" + PROJECT_NAME,
							"spring.config.use-legacy-processing=true")
					.web(WebApplicationType.NONE);

	@Test
	public void testGetProperty() {
		try (ConfigurableApplicationContext c = applicationBuilder.run()) {
			String secret = c.getEnvironment().getProperty("sm://my-secret");
			assertThat(secret).isEqualTo("hello");
		}
	}

	@Test
	public void testGetProperty_otherVersion() {
		try (ConfigurableApplicationContext c = applicationBuilder.run()) {
			String secret = c.getEnvironment().getProperty(
					"sm://my-secret/1");
			assertThat(secret).isEqualTo("hello v1");
		}
	}

	@Test
	public void testGetProperty_otherProject() {
		try (ConfigurableApplicationContext c = applicationBuilder.run()) {
			String secret = c.getEnvironment().getProperty(
					"sm://projects/other-project/secrets/other-secret/versions/3");
			assertThat(secret).isEqualTo("goodbye");
		}
	}

	@Test
	public void testValueAnnotation() {
		try (ConfigurableApplicationContext c = applicationBuilder.run()) {
			String secret = c.getBean("secret", String.class);
			assertThat(secret).isEqualTo("hello");

			secret = c.getBean("fullyQualifiedSecret", String.class);
			assertThat(secret).isEqualTo("hello");
		}
	}

	@Test
	public void configurationDisabled() {
		SpringApplicationBuilder disabledConfigurationApp =
				new SpringApplicationBuilder(
						TestBootstrapConfiguration.class, GcpSecretManagerBootstrapConfiguration.class)
						.properties("spring.cloud.gcp.secretmanager.project-id=" + PROJECT_NAME)
						.properties("spring.cloud.gcp.secretmanager.enabled=false")
						.web(WebApplicationType.NONE);

		try (ConfigurableApplicationContext c = disabledConfigurationApp.run()) {
			String secret = c.getEnvironment().getProperty("sm://my-secret");
			assertThat(secret).isEqualTo(null);
		}
	}

	@Configuration
	static class TestBootstrapConfiguration {

		@Value("${sm://my-secret}")
		private String secret;

		@Value("${sm://" + PROJECT_NAME + "/my-secret/latest}")
		private String fullyQualifiedSecret;

		@Bean
		public String secret() {
			return secret;
		}

		@Bean
		public String fullyQualifiedSecret() {
			return secret;
		}

		@Bean
		public static SecretManagerServiceClient secretManagerClient() {
			SecretManagerServiceClient client = mock(SecretManagerServiceClient.class);

			SecretVersionName secretVersionName =
					SecretVersionName.newBuilder()
							.setProject(PROJECT_NAME)
							.setSecret("my-secret")
							.setSecretVersion("latest")
							.build();

			when(client.accessSecretVersion(secretVersionName)).thenReturn(
					AccessSecretVersionResponse.newBuilder()
							.setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("hello")))
							.build());

			secretVersionName =
					SecretVersionName.newBuilder()
							.setProject(PROJECT_NAME)
							.setSecret("my-secret")
							.setSecretVersion("1")
							.build();

			when(client.accessSecretVersion(secretVersionName)).thenReturn(
					AccessSecretVersionResponse.newBuilder()
							.setPayload(SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("hello v1")))
							.build());

			secretVersionName =
					SecretVersionName.newBuilder()
							.setProject("other-project")
							.setSecret("other-secret")
							.setSecretVersion("3")
							.build();

			when(client.accessSecretVersion(secretVersionName)).thenReturn(
					AccessSecretVersionResponse.newBuilder()
							.setPayload(
									SecretPayload.newBuilder().setData(ByteString.copyFromUtf8("goodbye")))
							.build());

			return client;
		}

		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

		// This is added in here to verify that the Secret Manager property source locator bean
		// still gets created even if another PropertySourceLocator bean exists in the environment.
		@Bean
		public static PropertySourceLocator defaultPropertySourceLocator() {
			return locatorEnvironment -> null;
		}
	}
}
