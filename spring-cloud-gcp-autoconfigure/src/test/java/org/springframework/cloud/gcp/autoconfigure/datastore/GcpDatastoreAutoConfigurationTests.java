/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.datastore;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
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
 * @author Chengyuan Zhao
 */
public class GcpDatastoreAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class,
					GcpContextAutoConfiguration.class, DatastoreTransactionManagerAutoConfiguration.class,
					DatastoreRepositoriesAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
					"spring.cloud.gcp.datastore.namespace-id=testNamespace");

	@Test
	public void testDatastoreOperationsCreated() {
		this.contextRunner.run(context -> assertThat(context.getBean(DatastoreOperations.class)).isNotNull());
	}

	@Test
	public void testTestRepositoryCreated() {
		this.contextRunner.run(context -> assertThat(context.getBean(TestRepository.class)).isNotNull());
	}

	@Test
	public void datastoreTransactionManagerCreated() {
		this.contextRunner.run(context -> {
			DatastoreTransactionManager transactionManager = context
					.getBean(DatastoreTransactionManager.class);
			assertThat(transactionManager).isNotNull();
			assertThat(transactionManager)
					.isInstanceOf(DatastoreTransactionManager.class);
		});
	}

	@AutoConfigurationPackage
	static class TestConfiguration {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}
	}
}
