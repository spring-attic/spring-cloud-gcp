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


import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * @author Felipe M Amaral
 * @since 1.2.4
 */
public class FirebaseAuthenticationProjectIdEvaluatorTests {

	/**
	 * Project ID used by GcpProjectIdProvider.
	 */
	public static final String GCP_PROVIDER_PROJECT_ID = "spring-gcp-test-project";

	/**
	 * Project ID used by FirebaseAuthenticationProperties.
	 */
	public static final String FIREBASE_PROJECT_ID = "spring-firebase-test-project";

	@Test
	public void testFirebaseProjectIdNotSet() throws Exception {
		new ApplicationContextRunner()
				.withBean(FirebaseAuthenticationProjectIdEvaluator.class)
				.withConfiguration(AutoConfigurations.of(TestConfig.class))
				.run(context -> {
					FirebaseAuthenticationProjectIdEvaluator evaluator = context.getBean(FirebaseAuthenticationProjectIdEvaluator.class);
					assertThat(evaluator.getProjectId()).isEqualTo(GCP_PROVIDER_PROJECT_ID);
				});
	}

	@Test
	public void testFirebaseProjectIdSet() throws Exception {
		new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.gcp.security.firebase.project-id=" + FIREBASE_PROJECT_ID)
				.withBean(FirebaseAuthenticationProjectIdEvaluator.class)
				.withConfiguration(AutoConfigurations.of(TestConfig.class))
				.run(context -> {
					FirebaseAuthenticationProjectIdEvaluator evaluator = context.getBean(FirebaseAuthenticationProjectIdEvaluator.class);
					assertThat(evaluator.getProjectId()).isEqualTo(FIREBASE_PROJECT_ID);
				});
	}

	@EnableConfigurationProperties(FirebaseAuthenticationProperties.class)
	static class TestConfig {

		@Bean
		public GcpProjectIdProvider projectIdProvider() {
			return () -> GCP_PROVIDER_PROJECT_ID;
		}

	}
}
