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

import java.lang.reflect.Method;

import com.google.common.annotations.VisibleForTesting;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * Query lookup strategy for Query Methods for Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreQueryLookupStrategy implements QueryLookupStrategy {

	private final DatastoreOperations datastoreOperations;

	private final DatastoreMappingContext datastoreMappingContext;

	private QueryMethodEvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	public DatastoreQueryLookupStrategy(DatastoreMappingContext datastoreMappingContext,
			DatastoreOperations datastoreOperations,
			QueryMethodEvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser expressionParser) {
		Assert.notNull(datastoreMappingContext,
				"A non-null DatastoreMappingContext is required.");
		Assert.notNull(datastoreOperations,
				"A non-null DatastoreOperations is required.");
		Assert.notNull(evaluationContextProvider,
				"A non-null EvaluationContextProvider is required.");
		Assert.notNull(expressionParser, "A non-null SpelExpressionParser is required.");
		this.datastoreMappingContext = datastoreMappingContext;
		this.evaluationContextProvider = evaluationContextProvider;
		this.datastoreOperations = datastoreOperations;
		this.expressionParser = expressionParser;
	}

	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
			ProjectionFactory factory, NamedQueries namedQueries) {
		DatastoreQueryMethod queryMethod = createQueryMethod(method, metadata, factory);
		Class<?> entityType = getEntityType(queryMethod);

		if (queryMethod.hasAnnotatedQuery()) {
			String sql = queryMethod.getQueryAnnotation().value();
			return createGqlDatastoreQuery(entityType, queryMethod, sql);
		}
		else if (namedQueries.hasQuery(queryMethod.getNamedQueryName())) {
			String sql = namedQueries.getQuery(queryMethod.getNamedQueryName());
			return createGqlDatastoreQuery(entityType, queryMethod, sql);
		}

		return new PartTreeDatastoreQuery<>(queryMethod, this.datastoreOperations,
				this.datastoreMappingContext, entityType);
	}

	@VisibleForTesting
	<T> GqlDatastoreQuery<T> createGqlDatastoreQuery(Class<T> entityType,
			DatastoreQueryMethod queryMethod, String gql) {
		return new GqlDatastoreQuery<>(entityType, queryMethod, this.datastoreOperations,
				gql, this.evaluationContextProvider, this.expressionParser,
				this.datastoreMappingContext);
	}

	@VisibleForTesting
	Class<?> getEntityType(QueryMethod queryMethod) {
		return queryMethod.getResultProcessor().getReturnedType().getDomainType();
	}

	@VisibleForTesting
	DatastoreQueryMethod createQueryMethod(Method method, RepositoryMetadata metadata,
			ProjectionFactory factory) {
		return new DatastoreQueryMethod(method, metadata, factory);
	}

}
