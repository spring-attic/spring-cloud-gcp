/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore.repository.support;

import java.lang.reflect.Method;

import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.repository.query.PartTreeFirestoreQuery;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * The class that decides what type of Query Method to use. For Firestore it is always
 * just PartTree.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class FirestoreQueryLookupStrategy implements QueryLookupStrategy {

	private final FirestoreTemplate firestoreTemplate;

	/**
	 * Constructor.
	 * @param firestoreTemplate the template that will be used to execute queries.
	 */
	public FirestoreQueryLookupStrategy(FirestoreTemplate firestoreTemplate) {
		this.firestoreTemplate = firestoreTemplate;
	}

	@Override
	public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata,
			ProjectionFactory projectionFactory, NamedQueries namedQueries) {
		// In this method we usually decide if the query method is a PartTree or an annotated
		// @Query method.
		// There is no choice in Firestore. We only have PartTree.

		return new PartTreeFirestoreQuery(new QueryMethod(method, repositoryMetadata, projectionFactory),
				this.firestoreTemplate, this.firestoreTemplate.getMappingContext());
	}
}
