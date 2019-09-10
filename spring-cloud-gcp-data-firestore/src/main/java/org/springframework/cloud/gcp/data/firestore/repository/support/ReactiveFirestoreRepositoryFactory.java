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

package org.springframework.cloud.gcp.data.firestore.repository.support;

import java.util.Optional;

import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.SimpleFirestoreReactiveRepository;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentEntity;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestorePersistentEntityInformation;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.ReactiveRepositoryFactorySupport;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.lang.Nullable;

/**
 * A factory for reactive Firestore repositories.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class ReactiveFirestoreRepositoryFactory extends ReactiveRepositoryFactorySupport {

	private final FirestoreTemplate firestoreTemplate;

	private final FirestoreMappingContext firestoreMappingContext;

	/**
	 * Constructor.
	 * @param firestoreTemplate the template that will be used by created repositories.
	 * @param firestoreMappingContext the mapping context used to look up type metadata.
	 */
	public ReactiveFirestoreRepositoryFactory(FirestoreTemplate firestoreTemplate,
			FirestoreMappingContext firestoreMappingContext) {
		this.firestoreTemplate = firestoreTemplate;
		this.firestoreMappingContext = firestoreMappingContext;
	}

	@Override
	public <T, ID> EntityInformation<T, ID> getEntityInformation(Class<T> aClass) {
		return (EntityInformation<T, ID>) new FirestorePersistentEntityInformation<T>(
				(FirestorePersistentEntity<T>) this.firestoreMappingContext.getPersistentEntity(aClass));
	}

	@Override
	protected Object getTargetRepository(RepositoryInformation repositoryInformation) {
		return getTargetRepositoryViaReflection(repositoryInformation, this.firestoreTemplate,
				repositoryInformation.getDomainType());
	}

	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
		return SimpleFirestoreReactiveRepository.class;
	}

	@Override
	protected Optional<QueryLookupStrategy> getQueryLookupStrategy(@Nullable QueryLookupStrategy.Key key,
			QueryMethodEvaluationContextProvider evaluationContextProvider) {
		return Optional.of(new FirestoreQueryLookupStrategy(this.firestoreTemplate));
	}
}
