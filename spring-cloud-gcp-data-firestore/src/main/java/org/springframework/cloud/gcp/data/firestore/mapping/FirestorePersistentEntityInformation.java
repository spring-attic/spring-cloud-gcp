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

package org.springframework.cloud.gcp.data.firestore.mapping;

import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 * A metadata holder for Firestore persistent entities.
 *
 * @param <T> the domain type.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class FirestorePersistentEntityInformation<T> extends AbstractEntityInformation<T, String> {

	private final FirestorePersistentEntity<T> persistentEntity;

	/**
	 * Constructor.
	 * @param persistentEntity the persistent entity of the type.
	 */
	public FirestorePersistentEntityInformation(FirestorePersistentEntity<T> persistentEntity) {
		super(persistentEntity.getType());
		this.persistentEntity = persistentEntity;
	}

	@Override
	public String getId(T entity) {
		return (String) this.persistentEntity.getPropertyAccessor(entity)
				.getProperty(this.persistentEntity.getIdProperty());
	}

	@Override
	public Class<String> getIdType() {
		return String.class;
	}
}
