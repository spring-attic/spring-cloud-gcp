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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SpannerRepositoryFactoryTests {

	private SpannerRepositoryFactory spannerRepositoryFactory;

	private SpannerMappingContext spannerMappingContext;

	private SpannerOperations spannerOperations;

	@Before
	public void setUp() {
		this.spannerMappingContext = new SpannerMappingContext();
		this.spannerOperations = mock(SpannerOperations.class);
		this.spannerRepositoryFactory = new SpannerRepositoryFactory(
				this.spannerMappingContext, this.spannerOperations);
	}

	@Test
	public void getEntityInformationTest() {
		EntityInformation entityInformation = this.spannerRepositoryFactory
				.getEntityInformation(TestEntity.class);
		assertEquals(TestEntity.class, entityInformation.getJavaType());
		assertEquals(String.class, entityInformation.getIdType());
	}

	@Test(expected = MappingException.class)
	public void getEntityInformationNotAvailableTest() {
		SpannerRepositoryFactory factory = new SpannerRepositoryFactory(
				mock(SpannerMappingContext.class), this.spannerOperations);
		factory.getEntityInformation(TestEntity.class);
	}

	@Test
	public void getTargetRepositoryTest() {
		RepositoryInformation repoInfo = mock(RepositoryInformation.class);
		when(repoInfo.getRepositoryBaseClass())
				.thenReturn((Class) SpannerRepositoryImpl.class);
		when(repoInfo.getDomainType()).thenReturn((Class) TestEntity.class);
		Object repo = this.spannerRepositoryFactory.getTargetRepository(repoInfo);
		assertEquals(SpannerRepositoryImpl.class, repo.getClass());
	}

	@Test
	public void getRepositoryBaseClassTest() {
		Class baseClass = this.spannerRepositoryFactory.getRepositoryBaseClass(null);
		assertEquals(SpannerRepositoryImpl.class, baseClass);
	}

	@SpannerTable(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@SpannerColumn(name = "custom_col")
		String something;
	}
}
