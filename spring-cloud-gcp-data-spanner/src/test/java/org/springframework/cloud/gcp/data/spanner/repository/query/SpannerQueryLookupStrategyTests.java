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

package org.springframework.cloud.gcp.data.spanner.repository.query;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Statement;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerWriteConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests Spanner Query Method lookups.
 *
 * @author Chengyuan Zhao
 */
public class SpannerQueryLookupStrategyTests {

	private SpannerTemplate spannerTemplate;

	private SpannerMappingContext spannerMappingContext;

	private SpannerQueryMethod queryMethod;

	private SpannerQueryLookupStrategy spannerQueryLookupStrategy;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser spelExpressionParser;

	@Before
	public void initMocks() {
		this.spannerMappingContext = new SpannerMappingContext();
		this.spannerTemplate = mock(SpannerTemplate.class);
		this.queryMethod = mock(SpannerQueryMethod.class);
		this.evaluationContextProvider = mock(QueryMethodEvaluationContextProvider.class);
		this.spelExpressionParser = new SpelExpressionParser();
		this.spannerQueryLookupStrategy = getSpannerQueryLookupStrategy();

		when(this.queryMethod.getQueryAnnotation()).thenReturn(new Query() {
			@Override
			public Class<? extends Annotation> annotationType() {
				return Query.class;
			}

			@Override
			public String value() {
				return "";
			}

			@Override
			public boolean dmlStatement() {
				return false;
			}
		});
	}

	@Test
	public void resolveSqlQueryTest() {
		String queryName = "fakeNamedQueryName";
		String query = "fake query";
		when(this.queryMethod.getNamedQueryName()).thenReturn(queryName);
		NamedQueries namedQueries = mock(NamedQueries.class);

		Parameters parameters = mock(Parameters.class);

		// @formatter:off
		Mockito.<Parameters>when(this.queryMethod.getParameters()).thenReturn(parameters);
		// @formatter:off

		when(parameters.getNumberOfParameters()).thenReturn(1);
		when(parameters.getParameter(anyInt())).thenAnswer((invocation) -> {
			Parameter param = mock(Parameter.class);
			when(param.getName()).thenReturn(Optional.of("tag"));
			// @formatter:off
			Mockito.<Class>when(param.getType()).thenReturn(Object.class);
			// @formatter:on
			return param;
		});

		when(namedQueries.hasQuery(eq(queryName))).thenReturn(true);
		when(namedQueries.getQuery(eq(queryName))).thenReturn(query);

		this.spannerQueryLookupStrategy.resolveQuery(null, null, null, namedQueries);

		verify(this.spannerQueryLookupStrategy, times(1)).createSqlSpannerQuery(
				eq(Object.class), same(this.queryMethod), eq(query), eq(false));
	}

	@Test
	public void resolvePartTreeQueryTest() {
		String queryName = "fakeNamedQueryName";
		when(this.queryMethod.getNamedQueryName()).thenReturn(queryName);
		NamedQueries namedQueries = mock(NamedQueries.class);
		when(namedQueries.hasQuery(any())).thenReturn(false);

		this.spannerQueryLookupStrategy.resolveQuery(null, null, null, namedQueries);

		verify(this.spannerQueryLookupStrategy, times(1))
				.createPartTreeSpannerQuery(eq(Object.class), same(this.queryMethod));
	}

	private SpannerQueryLookupStrategy getSpannerQueryLookupStrategy() {
		SpannerQueryLookupStrategy spannerQueryLookupStrategy = spy(
				new SpannerQueryLookupStrategy(this.spannerMappingContext,
						this.spannerTemplate, this.evaluationContextProvider,
						this.spelExpressionParser));
		doReturn(Object.class).when(spannerQueryLookupStrategy).getEntityType(any());
		doReturn(null).when(spannerQueryLookupStrategy).createPartTreeSpannerQuery(any(),
				any());
		doReturn(this.queryMethod).when(spannerQueryLookupStrategy)
				.createQueryMethod(any(), any(), any());
		return spannerQueryLookupStrategy;
	}

	@Test
	public void getChildrenRowsQueryTest() {
		TestEntity t = new TestEntity();
		t.id = "key";
		t.id2 = "key2";
		Statement statement = SpannerStatementQueryExecutor.getChildrenRowsQuery(
				Key.newBuilder().append(t.id).append(t.id2).build(),
				this.spannerMappingContext.getPersistentEntity(ChildEntity.class), new SpannerWriteConverter(),
				this.spannerMappingContext);
		assertThat(statement.getSql())
				.isEqualTo("SELECT id3, id, id_2 FROM child_test_table WHERE (id = @tag0 AND id_2 = @tag1)");
		assertThat(statement.getParameters()).hasSize(2);
		assertThat(statement.getParameters().get("tag0").getString()).isEqualTo("key");
		assertThat(statement.getParameters().get("tag1").getString()).isEqualTo("key2");
	}

	@Test
	public void getColumnsStringForSelectTest() {
		TestEntity t = new TestEntity();
		t.id = "key";
		t.id2 = "key2";
		String columnsStringForSelect = SpannerStatementQueryExecutor.getColumnsStringForSelect(
				this.spannerMappingContext.getPersistentEntity(TestEntity.class),
				this.spannerMappingContext, true);

		assertThat(columnsStringForSelect)
				.isEqualTo("other, id, custom_col, id_2, " +
						"ARRAY (SELECT AS STRUCT id3, id, id_2 " +
						"FROM child_test_table WHERE child_test_table.id = custom_test_table.id " +
						"AND child_test_table.id_2 = custom_test_table.id_2) as childEntities");
	}


	@Test
	public void getColumnsStringForSelectMultipleTest() {
		Statement childrenRowsQuery = SpannerStatementQueryExecutor.buildQuery(
				KeySet.newBuilder().addKey(Key.of("k1.1", "k1.2")).addKey(Key.of("k2.1", "k2.2")).build(),
				this.spannerMappingContext.getPersistentEntity(TestEntity.class), new SpannerWriteConverter(),
				this.spannerMappingContext);

		assertThat(childrenRowsQuery.getSql())
				.isEqualTo(
						"SELECT other, id, custom_col, id_2, ARRAY (SELECT AS STRUCT id3, id, id_2 " +
								"FROM child_test_table WHERE child_test_table.id = custom_test_table.id " +
								"AND child_test_table.id_2 = custom_test_table.id_2) as childEntities " +
								"FROM custom_test_table WHERE (id = @tag0 AND id_2 = @tag1) " +
								"OR (id = @tag2 AND id_2 = @tag3)");
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		@Column(name = "id_2")
		String id2;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		@Interleaved
		List<ChildEntity> childEntities;
	}

	@Table(name = "child_test_table")
	private static class ChildEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		String id_2;

		@PrimaryKey(keyOrder = 3)
		String id3;
	}
}
