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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import java.util.Optional;

import com.google.cloud.spanner.Key;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.cloud.gcp.data.spanner.repository.query.SpannerQueryLookupStrategy;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the Spanner repository factory.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerRepositoryFactoryTests {

	private SpannerRepositoryFactory spannerRepositoryFactory;

	private SpannerTemplate spannerTemplate;

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void setUp() {
		SpannerMappingContext spannerMappingContext = new SpannerMappingContext();
		this.spannerTemplate = mock(SpannerTemplate.class);
		this.spannerRepositoryFactory = new SpannerRepositoryFactory(
				spannerMappingContext, this.spannerTemplate);
	}

	@Test
	public void getEntityInformationTest() {
		EntityInformation<TestEntity, Key> entityInformation = this.spannerRepositoryFactory
				.getEntityInformation(TestEntity.class);
		assertThat(entityInformation.getJavaType()).isEqualTo(TestEntity.class);
		assertThat(entityInformation.getIdType()).isEqualTo(Key.class);

		TestEntity t = new TestEntity();
		t.id = "a";
		t.id2 = 3L;
		assertThat(entityInformation.getId(t))
				.isEqualTo(Key.newBuilder().append(t.id).append(t.id2).build());
	}

	@Test
	public void getEntityInformationNotAvailableTest() {
		this.expectedEx.expect(MappingException.class);
		this.expectedEx.expectMessage("Could not lookup mapping metadata for domain " +
				"class org.springframework.cloud.gcp.data.spanner.repository.support." +
				"SpannerRepositoryFactoryTests$TestEntity!");
		SpannerRepositoryFactory factory = new SpannerRepositoryFactory(
				mock(SpannerMappingContext.class), this.spannerTemplate);
		factory.getEntityInformation(TestEntity.class);
	}

	@Test
	public void getTargetRepositoryTest() {
		RepositoryInformation repoInfo = mock(RepositoryInformation.class);
		// @formatter:off
		Mockito.<Class<?>>when(repoInfo.getRepositoryBaseClass())
				.thenReturn(SimpleSpannerRepository.class);
		Mockito.<Class<?>>when(repoInfo.getDomainType()).thenReturn(TestEntity.class);
		// @formatter:on
		Object repo = this.spannerRepositoryFactory.getTargetRepository(repoInfo);
		assertThat(repo).isInstanceOf(SimpleSpannerRepository.class);
	}

	@Test
	public void getRepositoryBaseClassTest() {
		Class baseClass = this.spannerRepositoryFactory.getRepositoryBaseClass(null);
		assertThat(baseClass).isEqualTo(SimpleSpannerRepository.class);
	}

	@Test
	public void getQueryLookupStrategyTest() {
		Optional<QueryLookupStrategy> qls = this.spannerRepositoryFactory
				.getQueryLookupStrategy(null, mock(QueryMethodEvaluationContextProvider.class));
		assertThat(qls.get()).isInstanceOf(SpannerQueryLookupStrategy.class);
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		long id2;

		@Column(name = "custom_col")
		String something;
	}
}
