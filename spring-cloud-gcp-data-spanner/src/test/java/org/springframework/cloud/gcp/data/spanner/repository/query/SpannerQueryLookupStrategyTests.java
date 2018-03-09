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

package org.springframework.cloud.gcp.data.spanner.repository.query;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.query.QueryMethod;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SpannerQueryLookupStrategyTests {

	private SpannerOperations spannerOperations;

	private SpannerMappingContext spannerMappingContext;

	private QueryMethod queryMethod;

	private SpannerQueryLookupStrategy spannerQueryLookupStrategy;

	@Before
	public void initMocks() {
		this.spannerOperations = mock(SpannerOperations.class);
		this.spannerMappingContext = new SpannerMappingContext();
		this.queryMethod = mock(QueryMethod.class);
		this.spannerQueryLookupStrategy = getSpannerQueryLookupStrategy();
	}

	@Test
	public void resolveSqlQueryTest() {
		String queryName = "fakeNamedQueryName";
		String query = "fake query";
		when(this.queryMethod.getNamedQueryName()).thenReturn(queryName);
		NamedQueries namedQueries = mock(NamedQueries.class);

		when(namedQueries.hasQuery(eq(queryName))).thenReturn(true);
		when(namedQueries.getQuery(eq(queryName))).thenReturn(query);

		this.spannerQueryLookupStrategy.resolveQuery(null, null, null, namedQueries);

		verify(this.spannerQueryLookupStrategy, times(1))
				.createSqlSpannerQuery(eq(Object.class), same(this.queryMethod), eq(query));
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
						this.spannerOperations));
		doReturn(Object.class).when(spannerQueryLookupStrategy).getEntityType(any());
		doReturn(null).when(spannerQueryLookupStrategy).createPartTreeSpannerQuery(any(),
				any());
		doReturn(this.queryMethod).when(spannerQueryLookupStrategy)
				.createQueryMethod(any(), any(), any());
		return spannerQueryLookupStrategy;
	}
}
