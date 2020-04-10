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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Mike Eltsufin
 */
public class SecretManagerBootstrapConfigurationTests {

	private static final String PROJECT_NAME = "hollow-light-of-the-sealed-land";

	private SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestBootstrapConfiguration.class)
			.child(GcpSecretManagerBootstrapConfiguration.class)
			.child(TestConfiguration.class)
			.web(WebApplicationType.NONE);

	@Test
	public void test() {
		try (ConfigurableApplicationContext c = applicationBuilder.run()) {
			String secret = c.getEnvironment().getProperty("sm://my-secret");
			// String secret = c.getBean("secret", String.class);
			assertThat(secret).isEqualTo("hello");
		}
	}

	@Configuration
	static class TestConfiguration {

		@Value("${secret-manager/test-spring/images/spring.png}")
		private String secret;

		@Bean
		public String secret() {
			return secret;
		}
	}

	@Configuration
	static class TestBootstrapConfiguration {

		@Bean
		public static SecretManagerServiceClient secretManagerClient() throws Exception {
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

			return client;
		}

		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

		@Bean
		public static GcpProjectIdProvider gcpProjectIdProvider() {
			return () -> PROJECT_NAME;
		}
	}

}
