/*
 * Copyright 2017-2019 the original author or authors.
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

import java.util.function.Supplier;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.health.DatastoreHealthIndicator;
import org.springframework.cloud.gcp.autoconfigure.datastore.health.DatastoreHealthIndicatorAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.data.rest.webmvc.spi.BackendIdConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for Datastore auto-config.
 *
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 */
public class GcpDatastoreAutoConfigurationTests {

	/** Mock datastore for use in configuration. */
	public static Datastore MOCK_CLIENT = mock(Datastore.class);

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					DatastoreTransactionManagerAutoConfiguration.class,
					DatastoreRepositoriesAutoConfiguration.class,
					DatastoreHealthIndicatorAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
					"spring.cloud.gcp.datastore.namespace=testNamespace",
					"spring.cloud.gcp.datastore.host=localhost:8081",
					"management.health.datastore.enabled=false");

	@Test
	public void testUserDatastoreBean() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class))
				.withUserConfiguration(TestConfigurationWithDatastoreBean.class)
				.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
						"spring.cloud.gcp.datastore.namespace=testNamespace",
						"spring.cloud.gcp.datastore.host=localhost:8081",
						"management.health.datastore.enabled=false");

		runner.run(context -> {
			assertThat(getDatastoreBean(context))
					.isSameAs(MOCK_CLIENT);
		});
	}

	@Test
	public void testUserDatastoreBeanNamespace() {
		ApplicationContextRunner runner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class,
						GcpContextAutoConfiguration.class))
				.withUserConfiguration(TestConfigurationWithDatastoreBeanNamespaceProvider.class)
				.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
						"spring.cloud.gcp.datastore.namespace=testNamespace",
						"spring.cloud.gcp.datastore.host=localhost:8081",
						"management.health.datastore.enabled=false");

		this.expectedException.expectMessage("failed to start");
		runner.run(context -> getDatastoreBean(context));
	}

	@Test
	public void testDatastoreSimpleClient() {
		this.contextRunner.run((context) -> assertThat(context.getBean(Datastore.class)).isNotNull());
	}

	@Test
	public void testDatastoreOptionsCorrectlySet() {
		this.contextRunner.run((context) -> {
			DatastoreOptions datastoreOptions = getDatastoreBean(context).getOptions();
			assertThat(datastoreOptions.getProjectId()).isEqualTo("test-project");
			assertThat(datastoreOptions.getNamespace()).isEqualTo("testNamespace");
			assertThat(datastoreOptions.getHost()).isEqualTo("localhost:8081");
		});
	}

	@Test
	public void testDatastoreEmulatorCredentialsConfig() {
		this.contextRunner.run((context) -> {
			CredentialsProvider defaultCredentialsProvider = context.getBean(CredentialsProvider.class);
			assertThat(defaultCredentialsProvider).isNotInstanceOf(NoCredentialsProvider.class);

			DatastoreOptions datastoreOptions = getDatastoreBean(context).getOptions();
			assertThat(datastoreOptions.getCredentials()).isInstanceOf(NoCredentials.class);
		});
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
	public void testIdConverterCreated() {
		this.contextRunner.run((context) -> {
			BackendIdConverter idConverter = context.getBean(BackendIdConverter.class);
			assertThat(idConverter).isNotNull();
			assertThat(idConverter).isInstanceOf(DatastoreKeyIdConverter.class);
		});
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
	public void testDatastoreHealthIndicatorNotCreated() {
		this.contextRunner.run((context) -> assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean(DatastoreHealthIndicator.class)));
	}

	private Datastore getDatastoreBean(ApplicationContext context) {
		return (Datastore) ((Supplier) context.getBean(
				context.getBeanNamesForType(ResolvableType.forClassWithGenerics(Supplier.class, Datastore.class))[0]))
						.get();
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

	/**
	 * Spring Boot config for tests with custom Datastore Bean.
	 */
	@Configuration
	static class TestConfigurationWithDatastoreBean {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}

		@Bean
		public GcpProjectIdProvider gcpProjectIdProvider() {
			return () -> "project123";
		}

		@Bean
		public Datastore datastore() {
			return MOCK_CLIENT;
		}
	}

	/**
	 * Spring Boot config for tests with custom Datastore Bean.
	 */
	@Configuration
	static class TestConfigurationWithDatastoreBeanNamespaceProvider {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}

		@Bean
		public Datastore datastore() {
			return MOCK_CLIENT;
		}

		@Bean
		public DatastoreNamespaceProvider datastoreNamespaceProvider() {
			return () -> "blah";
		}
	}
}
