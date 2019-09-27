/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.firestore.repository.query;

import java.lang.reflect.Method;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

/**
 * A Metadata class for Spring Data Reactive Firestore {@link QueryMethod}.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public class FirestoreQueryMethod extends QueryMethod {

	public FirestoreQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
		super(method, metadata, factory);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * For Spring Data Firestore, all Firestore query methods are Reactive and considered to
	 * be "streaming".
	 */
	@Override
	public boolean isStreamQuery() {
		return true;
	}
}
