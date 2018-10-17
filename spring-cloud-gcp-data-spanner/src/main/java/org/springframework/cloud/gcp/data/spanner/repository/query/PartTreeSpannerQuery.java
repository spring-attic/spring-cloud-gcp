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

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class PartTreeSpannerQuery<T> extends AbstractSpannerQuery<T> {

	private final PartTree tree;

	/**
	 * Constructor
	 * @param type the underlying entity type
	 * @param queryMethod the underlying query method to support.
	 * @param spannerTemplate used for executing queries.
	 * @param spannerMappingContext used for getting metadata about entities.
	 */
	public PartTreeSpannerQuery(Class<T> type, SpannerQueryMethod queryMethod,
			SpannerTemplate spannerTemplate,
			SpannerMappingContext spannerMappingContext) {
		super(type, queryMethod, spannerTemplate, spannerMappingContext);
		this.tree = new PartTree(queryMethod.getName(), type);
	}

	@Override
	protected List executeRawResult(Object[] parameters) {
		if (isCountOrExistsQuery()) {
			return SpannerStatementQueryExecutor.executeQuery(
					struct -> isCountQuery() ? struct.getLong(0) : struct.getBoolean(0),
					this.entityType, this.tree, parameters, this.spannerTemplate,
					this.spannerMappingContext);
		}
		if (this.tree.isDelete()) {
			return this.spannerTemplate
					.performReadWriteTransaction(getDeleteFunction(parameters));
		}
		return SpannerStatementQueryExecutor.executeQuery(this.entityType, this.tree,
						parameters, this.spannerTemplate, this.spannerMappingContext);
	}

	private Function<SpannerTemplate, List> getDeleteFunction(Object[] parameters) {
		return transactionTemplate -> {
			List<T> entitiesToDelete = SpannerStatementQueryExecutor
					.executeQuery(this.entityType, this.tree, parameters,
							transactionTemplate, this.spannerMappingContext);
			transactionTemplate.deleteAll(entitiesToDelete);

			List result = null;
			if (this.queryMethod.isCollectionQuery()) {
				result = entitiesToDelete;
			}
			else if (this.queryMethod.getReturnedObjectType() != void.class) {
				result = Collections.singletonList(entitiesToDelete.size());
			}
			return result;
		};
	}

	private boolean isCountOrExistsQuery() {
		return isCountQuery() || isExistsQuery();
	}

	private boolean isCountQuery() {
		return this.tree.isCountProjection();
	}

	private boolean isExistsQuery() {
		return this.tree.isExistsProjection();
	}

}
