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

import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * This is a placeholder class for a separate PR's implementation.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class PartTreeFirestoreQuery implements RepositoryQuery {

	private final FirestoreTemplate firestoreTemplate;

	private final QueryMethod queryMethod;

	/**
	 * Constructor.
	 * @param firestoreTemplate the template to use to execute the query.
	 * @param queryMethod the query method metadata.
	 */
	public PartTreeFirestoreQuery(FirestoreTemplate firestoreTemplate, QueryMethod queryMethod) {
		this.firestoreTemplate = firestoreTemplate;
		this.queryMethod = queryMethod;
	}

	@Override
	public Object execute(Object[] objects) {
		// Placeholder method.
		return null;
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}
}
