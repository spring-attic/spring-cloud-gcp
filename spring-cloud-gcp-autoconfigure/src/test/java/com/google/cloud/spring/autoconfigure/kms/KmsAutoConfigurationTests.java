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

package com.google.cloud.spring.autoconfigure.kms;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.spring.kms.KmsTemplate;
import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link GcpKmsAutoConfiguration}.
 *
 * @author Emmanouil Gkatziouras
 */
public class KmsAutoConfigurationTests {

	private static final String PROJECT_NAME = "hollow-light-of-the-sealed-land";
	private static final String LOCATION_NAME = "global";
	private static final String KEY_RING_NAME = "key-ring-id";
	private static final String KEY_ID_NAME = "key-id";

	private SpringApplicationBuilder applicationBuilder =
			new SpringApplicationBuilder(
					TestBootstrapConfiguration.class, GcpKmsAutoConfiguration.class)
			.properties(
					"spring.cloud.gcp.kms.project-id=" + PROJECT_NAME,
					"spring.cloud.bootstrap.enabled=true",
					"spring.cloud.gcp.sql.enabled=false"
			).web(WebApplicationType.NONE);

	@Test
	public void testKeyManagementClientCreated() {
		try (ConfigurableApplicationContext c = applicationBuilder.run()) {
			KeyManagementServiceClient client = c.getBean(KeyManagementServiceClient.class);
			assertThat(client).isNotNull();
		}
	}

	@Test
	public void testKmsTemplateCreated() {
		try (ConfigurableApplicationContext c = applicationBuilder.run()) {
			KmsTemplate kmsTemplate = c.getBean(KmsTemplate.class);
			assertThat(kmsTemplate).isNotNull();
		}
	}

	@Configuration
	static class TestBootstrapConfiguration {

		@Bean
		public static KeyManagementServiceClient keyManagementClient() {
			KeyManagementServiceClient client = mock(KeyManagementServiceClient.class);
			return client;
		}

		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

	}

}
