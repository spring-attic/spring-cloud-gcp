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

package org.springframework.cloud.gcp.data.datastore.repository.query;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;

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
 * @author Chengyuan Zhao
 */
public class DatastoreQueryLookupStrategyTests {

	private DatastoreTemplate datastoreTemplate;

	private DatastoreMappingContext datastoreMappingContext;

	private DatastoreQueryMethod queryMethod;

	private DatastoreQueryLookupStrategy datastoreQueryLookupStrategy;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	@Before
	public void initMocks() {
		this.datastoreTemplate = mock(DatastoreTemplate.class);
		this.datastoreMappingContext = new DatastoreMappingContext();
		this.queryMethod = mock(DatastoreQueryMethod.class);
		this.evaluationContextProvider = mock(QueryMethodEvaluationContextProvider.class);
		this.datastoreQueryLookupStrategy = getDatastoreQueryLookupStrategy();
	}

	@Test
	public void resolveSqlQueryTest() {
		String queryName = "fakeNamedQueryName";
		String query = "fake query";
		when(this.queryMethod.getNamedQueryName()).thenReturn(queryName);
		Query queryAnnotation = mock(Query.class);
		when(this.queryMethod.getQueryAnnotation()).thenReturn(queryAnnotation);
		NamedQueries namedQueries = mock(NamedQueries.class);

		Parameters parameters = mock(Parameters.class);

		Mockito.<Parameters>when(this.queryMethod.getParameters())
				.thenReturn(parameters);

		when(parameters.getNumberOfParameters()).thenReturn(1);
		when(parameters.getParameter(anyInt())).thenAnswer(invocation -> {
			Parameter param = mock(Parameter.class);
			when(param.getName()).thenReturn(Optional.of("tag"));
			Mockito.<Class>when(param.getType()).thenReturn(Object.class);
			return param;
		});

		when(namedQueries.hasQuery(eq(queryName))).thenReturn(true);
		when(namedQueries.getQuery(eq(queryName))).thenReturn(query);

		this.datastoreQueryLookupStrategy.resolveQuery(null, null, null, namedQueries);

		verify(this.datastoreQueryLookupStrategy, times(1)).createGqlDatastoreQuery(
				eq(Object.class), same(this.queryMethod), eq(query));
	}

	private DatastoreQueryLookupStrategy getDatastoreQueryLookupStrategy() {
		DatastoreQueryLookupStrategy spannerQueryLookupStrategy = spy(
				new DatastoreQueryLookupStrategy(this.datastoreMappingContext,
						this.datastoreTemplate, this.evaluationContextProvider));
		doReturn(Object.class).when(spannerQueryLookupStrategy).getEntityType(any());
		doReturn(this.queryMethod).when(spannerQueryLookupStrategy)
				.createQueryMethod(any(), any(), any());
		return spannerQueryLookupStrategy;
	}
}
