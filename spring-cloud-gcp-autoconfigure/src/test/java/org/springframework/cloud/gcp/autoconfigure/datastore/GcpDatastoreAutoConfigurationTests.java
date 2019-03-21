/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.datastore;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for Datastore auto-config.
 *
 * @author Chengyuan Zhao
 */
public class GcpDatastoreAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					DatastoreTransactionManagerAutoConfiguration.class,
					DatastoreRepositoriesAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
					"spring.cloud.gcp.datastore.namespace=testNamespace",
					"spring.cloud.gcp.datastore.emulator-host=localhost:8081");

	@Test
	public void testDatastoreOptionsCorrectlySet() {
		this.contextRunner.run((context) -> {
			DatastoreOptions datastoreOptions = context.getBean(Datastore.class)
					.getOptions();
			assertThat(datastoreOptions.getProjectId()).isEqualTo("test-project");
			assertThat(datastoreOptions.getNamespace()).isEqualTo("testNamespace");
			assertThat(datastoreOptions.getHost()).isEqualTo("localhost:8081");
		});
	}

	@Test
	public void testDatastoreEmulatorCredentialsConfig() {
		this.contextRunner.run((context) -> {
			CredentialsProvider defaultCredentialsProvider = context
					.getBean(CredentialsProvider.class);
			assertThat(defaultCredentialsProvider)
					.isNotInstanceOf(NoCredentialsProvider.class);

			DatastoreOptions datastoreOptions = context.getBean(Datastore.class)
					.getOptions();
			assertThat(datastoreOptions.getCredentials())
					.isInstanceOf(NoCredentials.class);
		});
	}

	@Test
	public void testDatastoreOperationsCreated() {
		this.contextRunner
				.run((context) -> assertThat(context.getBean(DatastoreOperations.class))
						.isNotNull());
	}

	@Test
	public void testTestRepositoryCreated() {
		this.contextRunner
				.run((context) -> assertThat(context.getBean(TestRepository.class))
						.isNotNull());
	}

	@Test
	public void datastoreTransactionManagerCreated() {
		this.contextRunner.run((context) -> {
			DatastoreTransactionManager transactionManager = context
					.getBean(DatastoreTransactionManager.class);
			assertThat(transactionManager).isNotNull();
			assertThat(transactionManager)
					.isInstanceOf(DatastoreTransactionManager.class);
		});
	}

	/**
	 * Spring Boot config for tests.
	 */
	@AutoConfigurationPackage
	static class TestConfiguration {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}

	}

}
