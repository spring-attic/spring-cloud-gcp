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

import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

/**
 * The bean to create Firestore repository factories.
 * @param <S> the entity type of the repository
 * @param <ID> the id type of the entity
 * @param <T> the repository type
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class FirestoreRepositoryFactoryBean<T extends Repository<S, ID>, S, ID> extends
		RepositoryFactoryBeanSupport<T, S, ID> {

	private FirestoreTemplate firestoreTemplate;

	private FirestoreMappingContext firestoreMappingContext;

	/**
	 * Constructor.
	 * @param repositoryInterface the repository interface class.
	 */
	FirestoreRepositoryFactoryBean(Class<T> repositoryInterface) {
		super(repositoryInterface);
	}

	public void setFirestoreTemplate(FirestoreTemplate firestoreTemplate) {
		this.firestoreTemplate = firestoreTemplate;
	}

	public void setFirestoreMappingContext(FirestoreMappingContext mappingContext) {
		super.setMappingContext(mappingContext);
		this.firestoreMappingContext = mappingContext;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory() {
		return new ReactiveFirestoreRepositoryFactory(this.firestoreTemplate, this.firestoreMappingContext);
	}
}
