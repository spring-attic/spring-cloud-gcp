/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.datastore;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.datastore.Datastore;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.health.DatastoreHealthIndicator;
import org.springframework.cloud.gcp.autoconfigure.datastore.health.DatastoreHealthIndicatorAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.health.DatastoreHealthIndicatorConfiguration;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for Datastore auto-config.
 *
 * @author Chengyuan Zhao
 * @author Raghavan N S
 * @author Srinivasa Meenavalli
 */
public class GcpDatastoreAutoConfigurationTests {

	private DatastoreHealthIndicator datastoreHealthIndicator;
	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					DatastoreTransactionManagerAutoConfiguration.class,
					DatastoreRepositoriesAutoConfiguration.class,
					DatastoreHealthIndicatorAutoConfiguration.class,
					DatastoreHealthIndicatorConfiguration.class,
					DatastoreHealthIndicator.class))
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
					"spring.cloud.gcp.datastore.namespace-id=testNamespace");
	@Before
	public void setUp() {
		datastoreHealthIndicator = new DatastoreHealthIndicator(mock(Datastore.class));
	}

	@Test
	public void testDatastoreOperationsCreated() {
		this.contextRunner.run((context) -> assertThat(context.getBean(DatastoreOperations.class)).isNotNull());
	}

	@Test
	public void testTestRepositoryCreated() {
		this.contextRunner.run((context) -> assertThat(context.getBean(TestRepository.class)).isNotNull());
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

	@Test
	public void testDatastoreTemplateCreated() {
		this.contextRunner.run((context) -> assertThat(context.getBean(DatastoreTemplate.class)).isNotNull());
	}

	@Test
	public void testDatastoreCreated() {
		this.contextRunner.run((context) -> assertThat(context.getBean(Datastore.class)).isNotNull());
	}

	@Test
	public void testDatastoreHealthIndicatorCreated() {
		this.contextRunner.run((context) -> assertThat(context.getBean(DatastoreHealthIndicator.class)).isNotNull());
	}

	@Test
	public void testDatastoreHealthIndicator() {
		this.contextRunner.run((context) -> {
			assertThat(datastoreHealthIndicator.health().getStatus().getCode()).isEqualTo(Status.UP.toString());
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
