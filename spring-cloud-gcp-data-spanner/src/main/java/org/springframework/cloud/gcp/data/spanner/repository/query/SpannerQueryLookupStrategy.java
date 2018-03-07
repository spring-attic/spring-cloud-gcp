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
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class SpannerQueryLookupStrategy implements QueryLookupStrategy {

	private final SpannerOperations spannerOperations;

	private final SpannerMappingContext spannerMappingContext;

	public SpannerQueryLookupStrategy(SpannerMappingContext spannerMappingContext,
			SpannerOperations spannerOperations) {
		Assert.notNull(spannerMappingContext,
				"A valid SpannerMappingContext is required.");
		Assert.notNull(spannerOperations, "A valid SpannerOperations is required.");
		this.spannerMappingContext = spannerMappingContext;
		this.spannerOperations = spannerOperations;
	}

	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata metadata,
			ProjectionFactory factory, NamedQueries namedQueries) {
		QueryMethod queryMethod = new QueryMethod(method, metadata, factory);

		if (namedQueries.hasQuery(queryMethod.getNamedQueryName())) {
			String sql = namedQueries.getQuery(queryMethod.getNamedQueryName());
			throw new UnsupportedOperationException("Could not use SQL: " + sql
					+ "Only method-name based methods are supported currently.");
		}

		return new PartTreeSpannerQuery(
				queryMethod.getResultProcessor().getReturnedType().getDomainType(),
				queryMethod, this.spannerOperations, this.spannerMappingContext);
	}
}
