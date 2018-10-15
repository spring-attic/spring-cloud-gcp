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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Abstract class for implementing Cloud Datastore query methods.
 *
 * @param <T> The domain type of the repository class containing this query method.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public abstract class AbstractDatastoreQuery<T> implements RepositoryQuery {

	final DatastoreMappingContext datastoreMappingContext;

	final DatastoreQueryMethod queryMethod;

	final DatastoreTemplate datastoreTemplate;

	final Class<T> entityType;

	public AbstractDatastoreQuery(DatastoreQueryMethod queryMethod,
			DatastoreTemplate datastoreTemplate,
			DatastoreMappingContext datastoreMappingContext, Class<T> entityType) {
		this.queryMethod = queryMethod;
		this.datastoreTemplate = datastoreTemplate;
		this.datastoreMappingContext = datastoreMappingContext;
		this.entityType = entityType;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	protected List applyProjection(List<T> rawResult) {
		if (rawResult == null) {
			return Collections.emptyList();
		}
		return rawResult.stream().map(this::processRawObjectForProjection)
				.collect(Collectors.toList());
	}

	@VisibleForTesting
	Object processRawObjectForProjection(T object) {
		return this.queryMethod.getResultProcessor().processResult(object);
	}
}
