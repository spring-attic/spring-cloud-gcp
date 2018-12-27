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

package org.springframework.cloud.gcp.data.datastore.repository.support;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the Datastore Repository factory.
 *
 * @author Chengyuan Zhao
 */
public class DatastoreRepositoryFactoryTests {

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private DatastoreRepositoryFactory datastoreRepositoryFactory;

	private DatastoreTemplate datastoreTemplate;

	@Before
	public void setUp() {
		DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();
		this.datastoreTemplate = mock(DatastoreTemplate.class);
		this.datastoreRepositoryFactory = new DatastoreRepositoryFactory(
				datastoreMappingContext, this.datastoreTemplate);
	}

	@Test
	public void getEntityInformationTest() {
		EntityInformation<TestEntity, String> entityInformation = this.datastoreRepositoryFactory
				.getEntityInformation(TestEntity.class);
		assertThat(entityInformation.getJavaType()).isEqualTo(TestEntity.class);
		assertThat(entityInformation.getIdType()).isEqualTo(String.class);

		TestEntity t = new TestEntity();
		t.id = "a";
		assertThat(entityInformation.getId(t)).isEqualTo("a");
	}

	@Test
	public void getEntityInformationNotAvailableTest() {
		this.expectedException.expect(MappingException.class);
		this.expectedException.expectMessage("Could not lookup mapping metadata for domain class: " +
				"org.springframework.cloud.gcp.data.datastore.repository.support." +
				"DatastoreRepositoryFactoryTests$TestEntity");
		DatastoreRepositoryFactory factory = new DatastoreRepositoryFactory(
				mock(DatastoreMappingContext.class), this.datastoreTemplate);
		factory.getEntityInformation(TestEntity.class);
	}

	@Test
	public void getTargetRepositoryTest() {
		RepositoryInformation repoInfo = mock(RepositoryInformation.class);
		Mockito.<Class<?>>when(repoInfo.getRepositoryBaseClass())
				.thenReturn(SimpleDatastoreRepository.class);
		Mockito.<Class<?>>when(repoInfo.getDomainType()).thenReturn(TestEntity.class);
		Object repo = this.datastoreRepositoryFactory.getTargetRepository(repoInfo);
		assertThat(repo.getClass()).isEqualTo(SimpleDatastoreRepository.class);
	}

	@Test
	public void getRepositoryBaseClassTest() {
		Class baseClass = this.datastoreRepositoryFactory.getRepositoryBaseClass(null);
		assertThat(baseClass).isEqualTo(SimpleDatastoreRepository.class);
	}

	@Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;

		@Field(name = "custom_col")
		String something;
	}
}
