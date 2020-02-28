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

package org.springframework.cloud.gcp.autoconfigure.security;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.security.firebase.FirebaseJwtTokenDecoder;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;


/**
 * @author Vinicius Carvalho
 * @since 1.2.2
 */
public class FirebaseAuthenticationAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(FirebaseAuthenticationAutoConfiguration.class, TestConfig.class));

	@Test
	public void testAutoConfigurationLoaded() throws Exception {
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.security.firebase.enabled=true")
				.run(context -> {
					FirebaseJwtTokenDecoder decoder = context.getBean(FirebaseJwtTokenDecoder.class);
					assertThat(decoder).isNotNull();
				});
	}

	@Test
	public void testAutoConfigurationNotLoaded() throws Exception {
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.security.firebase.enabled=false")
				.run(context -> {
					assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
							.isThrownBy(() -> context.getBean(FirebaseJwtTokenDecoder.class));
				});
	}



	static class TestConfig {

		@Bean
		public GcpProjectIdProvider projectIdProvider() {
			return () -> "spring-firebase-test-project";
		}

		@Bean
		public CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

	}

}
