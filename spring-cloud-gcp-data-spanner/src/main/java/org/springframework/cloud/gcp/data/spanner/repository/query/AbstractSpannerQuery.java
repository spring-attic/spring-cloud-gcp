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

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
abstract class AbstractSpannerQuery<T> implements RepositoryQuery {

	protected final QueryMethod queryMethod;

	protected final SpannerOperations spannerOperations;

	protected final SpannerMappingContext spannerMappingContext;

	protected final Class<T> entityType;

	/**
	 * Constructor
	 * @param type the underlying entity type
	 * @param queryMethod the underlying query method to support.
	 * @param spannerOperations used for executing queries.
	 * @param spannerMappingContext used for getting metadata about entities.
	 */
	AbstractSpannerQuery(Class<T> type, QueryMethod queryMethod,
			SpannerOperations spannerOperations,
			SpannerMappingContext spannerMappingContext) {
		this.queryMethod = queryMethod;
		this.entityType = type;
		this.spannerOperations = spannerOperations;
		this.spannerMappingContext = spannerMappingContext;
	}

	@Override
	public Object execute(Object[] parameters) {
		List<T> rawResult = executeRawResult(parameters);
		return applyProjection(rawResult);
	}

	protected Object applyProjection(List<T> rawResult) {
		if (rawResult == null) {
			return null;
		}
		return rawResult.stream().map(result -> processRawObjectForProjection(result))
				.collect(Collectors.toList());
	}

	@VisibleForTesting
	Object processRawObjectForProjection(Object object) {
		return this.queryMethod.getResultProcessor().processResult(object);
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	protected abstract List<T> executeRawResult(Object[] parameters);
}
