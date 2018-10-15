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

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.lang.Nullable;

/**
 * A metadata class for Query Methods for Spring Data Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreQueryMethod extends QueryMethod {

	private final Method method;

	/**
	 * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct
	 * query to use for following invocations of the method given.
	 *
	 * @param method must not be {@literal null}.
	 * @param metadata must not be {@literal null}.
	 * @param factory must not be {@literal null}.
	 */
	DatastoreQueryMethod(Method method, RepositoryMetadata metadata,
			ProjectionFactory factory) {
		super(method, metadata, factory);
		this.method = method;
	}

	/**
	 * Returns whether the method has an annotated query.
	 *
	 * @return True if this query method has annotation that holds the query string.
	 */
	boolean hasAnnotatedQuery() {
		return getQueryAnnotation() != null;
	}

	/**
	 * Get the type of the collection of the returned object.
	 * @return the collection type of returned object if it is collection-like.
	 * {@code null} otherwise
	 */
	Class getCollectionReturnType() {
		return isCollectionQuery() ? this.method.getReturnType() : null;
	}

	/**
	 * Return whether this method is an exists query.
	 *
	 * @return True if this query method is an exists query, and false otherwise.
	 */
	boolean isExistsQuery() {
		Query annotation = getQueryAnnotation();
		return annotation != null && annotation.exists();
	}

	/**
	 * Return whether this method is a count query.
	 *
	 * @return True if this query method is a count query, and false otherwise.
	 */
	boolean isCountQuery() {
		Query annotation = getQueryAnnotation();
		return annotation != null && annotation.count();
	}

	/**
	 * Returns the {@link Query} annotation that is applied to the method or {@code null}
	 * if none available.
	 *
	 * @return the query annotation that is applied.
	 */
	@Nullable
	Query getQueryAnnotation() {
		return AnnotatedElementUtils.findMergedAnnotation(this.method, Query.class);
	}
}
