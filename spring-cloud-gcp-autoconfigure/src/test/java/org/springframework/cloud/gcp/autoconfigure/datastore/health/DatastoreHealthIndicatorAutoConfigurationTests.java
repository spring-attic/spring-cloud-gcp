/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.datastore.health;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.datastore.Datastore;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.GcpDatastoreAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for Datastore health check auto-config.
 *
 * @author Raghavan N S
 * @author Srinivasa Meenavalli
 * @author Mike Eltsufin
 *
 * @since 1.2
 */
public class DatastoreHealthIndicatorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					DatastoreHealthIndicatorAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
					"spring.cloud.gcp.datastore.namespace-id=testNamespace",
					"management.health.datastore.enabled=true");

	@Test
	public void testDatastoreCreated() {
		this.contextRunner.run((context) -> assertThat(context.getBean(Datastore.class)).isNotNull());
	}

	@Test
	public void testDatastoreHealthIndicatorCreated() {
		this.contextRunner.run((context) -> assertThat(context.getBean(DatastoreHealthIndicator.class)).isNotNull());
	}

	/**
	 * Spring Boot config for tests.
	 */
	@Configuration
	static class TestConfiguration {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}
	}
}
