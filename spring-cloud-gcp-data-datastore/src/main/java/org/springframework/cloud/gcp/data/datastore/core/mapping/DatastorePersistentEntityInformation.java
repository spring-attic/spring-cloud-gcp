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

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 * Holds ID type information about a Datastore persistent entity.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastorePersistentEntityInformation<T, ID>
		extends AbstractEntityInformation<T, ID> {

	private final DatastorePersistentEntity<T> persistentEntity;

	/**
	 * Creates a new {@link DatastorePersistentEntityInformation} for the given
	 * {@link DatastorePersistentEntity}.
	 *
	 * @param entity must not be {@literal null}.
	 */
	public DatastorePersistentEntityInformation(DatastorePersistentEntity<T> entity) {
		super(entity.getType());
		this.persistentEntity = entity;
	}

	@Override
	public ID getId(T entity) {
		return (ID) this.persistentEntity.getPropertyAccessor(entity)
				.getProperty(this.persistentEntity.getIdProperty());
	}

	@Override
	public Class<ID> getIdType() {
		return (Class<ID>) this.persistentEntity.getIdPropertyOrFail().getType();
	}
}
