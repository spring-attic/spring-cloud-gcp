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

package org.springframework.cloud.gcp.data.datastore.repository.support;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
public class DatastoreRepositoryFactoryTests {

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
		assertEquals(TestEntity.class, entityInformation.getJavaType());
		assertEquals(String.class, entityInformation.getIdType());

		TestEntity t = new TestEntity();
		t.id = "a";
		assertEquals("a", entityInformation.getId(t));
	}

	@Test(expected = MappingException.class)
	public void getEntityInformationNotAvailableTest() {
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
		assertEquals(SimpleDatastoreRepository.class, repo.getClass());
	}

	@Test
	public void getRepositoryBaseClassTest() {
		Class baseClass = this.datastoreRepositoryFactory.getRepositoryBaseClass(null);
		assertEquals(SimpleDatastoreRepository.class, baseClass);
	}

	@Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;

		@Field(name = "custom_col")
		String something;
	}
}
