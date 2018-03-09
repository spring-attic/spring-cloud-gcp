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

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class PartTreeSpannerQuery implements RepositoryQuery {

	private final QueryMethod queryMethod;

	private final SpannerOperations spannerOperations;

	private final SpannerMappingContext spannerMappingContext;

	private final PartTree tree;

	private final Class entityType;

	/**
	 * Constructor
	 * @param type the underlying entity type
	 * @param queryMethod the underlying query method to support.
	 * @param spannerOperations used for executing queries.
	 * @param spannerMappingContext used for getting metadata about entities.
	 */
	public PartTreeSpannerQuery(Class type, QueryMethod queryMethod,
			SpannerOperations spannerOperations,
			SpannerMappingContext spannerMappingContext) {
		this.queryMethod = queryMethod;
		this.entityType = type;
		this.tree = new PartTree(queryMethod.getName(), type);
		this.spannerOperations = spannerOperations;
		this.spannerMappingContext = spannerMappingContext;
	}

	@Override
	public Object execute(Object[] parameters) {
		return SpannerStatementQueryExecutor.executeQuery(this.entityType, this.tree,
				parameters, this.spannerOperations, this.spannerMappingContext);
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}
}
