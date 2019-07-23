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

package org.springframework.cloud.gcp.autoconfigure.datastore.it;

import java.util.function.Supplier;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.StructuredQuery;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.DatastoreRepositoriesAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.DatastoreTransactionManagerAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.GcpDatastoreAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.datastore.GcpDatastoreEmulatorAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;
import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for Datastore Emulator integration with the datastore itself.
 *
 * @author Lucas Soares
 * @author Artem Bilan
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class GcpDatastoreEmulatorIntegrationTests {

	@Test
	public void testDatastoreEmulatorConfiguration() {
		DatastoreOptions.Builder builder = DatastoreOptions.newBuilder();

		new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpDatastoreAutoConfiguration.class,
						GcpContextAutoConfiguration.class,
						DatastoreTransactionManagerAutoConfiguration.class,
						DatastoreRepositoriesAutoConfiguration.class,
						GcpDatastoreEmulatorAutoConfiguration.class))
				.withUserConfiguration(TestConfiguration.class)
				.withPropertyValues("spring.cloud.gcp.project-id=test-project",
						"spring.cloud.gcp.datastore.namespace=test-namespace",
						"spring.cloud.gcp.datastore.emulator.port=8181",
						"spring.cloud.gcp.datastore.emulator.enabled=true",
						"spring.cloud.gcp.datastore.emulator.consistency=0.9")
				.run((context) -> {
					DatastoreTemplate datastore = context.getBean(DatastoreTemplate.class);
					Datastore datastoreClient = (Datastore) ((Supplier) context.getBean(context.getBeanNamesForType(
							ResolvableType.forClassWithGenerics(Supplier.class, Datastore.class))[0])).get();
					GcpProjectIdProvider projectIdProvider = context.getBean(GcpProjectIdProvider.class);

					builder.setServiceFactory(datastoreOptions -> datastoreClient)
							.setProjectId(projectIdProvider.getProjectId());

					EmulatorEntityTest entity = new EmulatorEntityTest();
					entity.setProperty("property-test");

					datastore.save(entity);

					assertThat(entity.getId()).isNotNull();

					assertThat(datastore.findById(entity.getId(), EmulatorEntityTest.class).getProperty())
							.isEqualTo("property-test");
				});

		Datastore datastore = builder.build().getService();

		EntityQuery query = Query.newEntityQueryBuilder().setKind("RandomKind")
				.setFilter(StructuredQuery.PropertyFilter.eq("key", "value"))
				.build();

		assertThatExceptionOfType(DatastoreException.class)
				.isThrownBy(() -> datastore.run(query));
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
	 * Entity to be stored on Datastore. An instance of `LocalDatastoreHelper` as a bean.
	 */
	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	static class EmulatorEntityTest {

		@Id
		private Long id;

		private String property;

		public void setId(Long id) {
			this.id = id;
		}

		Long getId() {
			return this.id;
		}

		void setProperty(String property) {
			this.property = property;
		}

		String getProperty() {
			return this.property;
		}

	}

}
