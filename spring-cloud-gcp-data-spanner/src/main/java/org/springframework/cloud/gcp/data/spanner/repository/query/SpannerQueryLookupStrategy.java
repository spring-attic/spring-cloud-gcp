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

import java.lang.reflect.Method;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class SpannerQueryLookupStrategy implements QueryLookupStrategy {

	private final SpannerOperations spannerOperations;

	private final SpannerMappingContext spannerMappingContext;

	private EvaluationContextProvider evaluationContextProvider;

	private SpelExpressionParser expressionParser;

	public SpannerQueryLookupStrategy(SpannerMappingContext spannerMappingContext,
			SpannerOperations spannerOperations,
			EvaluationContextProvider evaluationContextProvider,
			SpelExpressionParser expressionParser) {
		Assert.notNull(spannerMappingContext,
				"A valid SpannerMappingContext is required.");
		Assert.notNull(spannerOperations, "A valid SpannerOperations is required.");
		Assert.notNull(evaluationContextProvider,
				"A valid EvaluationContextProvider is required.");
		Assert.notNull(expressionParser, "A valid SpelExpressionParser is required.");
		this.spannerMappingContext = spannerMappingContext;
		this.evaluationContextProvider = evaluationContextProvider;
		this.spannerOperations = spannerOperations;
		this.expressionParser = expressionParser;
	}

	protected Class getEntityType(QueryMethod queryMethod) {
		return queryMethod.getResultProcessor().getReturnedType().getDomainType();
	}

	protected SpannerQueryMethod createQueryMethod(Method method,
			RepositoryMetadata metadata, ProjectionFactory factory) {
		return new SpannerQueryMethod(method, metadata, factory,
				this.spannerMappingContext);
	}

	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
			ProjectionFactory factory, NamedQueries namedQueries) {
		SpannerQueryMethod queryMethod = createQueryMethod(method, metadata, factory);
		Class entityType = getEntityType(queryMethod);

		if (queryMethod.hasAnnotatedQuery()) {
			String sql = queryMethod.getQueryAnnotation().value();
			return createSqlSpannerQuery(entityType, queryMethod, sql);
		}
		else if (namedQueries.hasQuery(queryMethod.getNamedQueryName())) {
			String sql = namedQueries.getQuery(queryMethod.getNamedQueryName());
			return createSqlSpannerQuery(entityType, queryMethod, sql);
		}


		return createPartTreeSpannerQuery(entityType, queryMethod);
	}

	protected SqlSpannerQuery createSqlSpannerQuery(Class entityType,
			QueryMethod queryMethod, String sql) {
		return new SqlSpannerQuery(entityType, queryMethod, this.spannerOperations, sql,
				this.evaluationContextProvider, this.expressionParser,
				this.spannerMappingContext);
	}

	protected PartTreeSpannerQuery createPartTreeSpannerQuery(Class entityType,
			QueryMethod queryMethod) {
		return new PartTreeSpannerQuery(entityType, queryMethod, this.spannerOperations,
				this.spannerMappingContext);
	}
}
