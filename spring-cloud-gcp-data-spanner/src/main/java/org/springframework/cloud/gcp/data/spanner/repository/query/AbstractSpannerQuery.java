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

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.annotations.VisibleForTesting;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Chengyuan Zhao
 */
abstract class AbstractSpannerQuery implements RepositoryQuery {

	protected final QueryMethod queryMethod;

	protected final SpannerOperations spannerOperations;

	protected final SpannerMappingContext spannerMappingContext;

	protected final Class entityType;

	/**
	 * Constructor
	 * @param type the underlying entity type
	 * @param queryMethod the underlying query method to support.
	 * @param spannerOperations used for executing queries.
	 * @param spannerMappingContext used for getting metadata about entities.
	 */
	AbstractSpannerQuery(Class type, QueryMethod queryMethod,
			SpannerOperations spannerOperations,
			SpannerMappingContext spannerMappingContext) {
		this.queryMethod = queryMethod;
		this.entityType = type;
		this.spannerOperations = spannerOperations;
		this.spannerMappingContext = spannerMappingContext;
	}

	@Override
	public Object execute(Object[] parameters) {
		Object rawResult = executeRawResult(parameters);
		if (rawResult == null) {
			return null;
		}
		if (ConversionUtils.isIterableNonByteArrayType(rawResult.getClass())) {
			return StreamSupport.stream(((Iterable) rawResult).spliterator(), true)
					.map(result -> processRawObjectForProjection(result))
					.collect(Collectors.toList());
		}
		return processRawObjectForProjection(rawResult);
	}

	@VisibleForTesting
	Object processRawObjectForProjection(Object object) {
		return this.queryMethod.getResultProcessor().processResult(object);
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	protected abstract Object executeRawResult(Object[] parameters);
}
