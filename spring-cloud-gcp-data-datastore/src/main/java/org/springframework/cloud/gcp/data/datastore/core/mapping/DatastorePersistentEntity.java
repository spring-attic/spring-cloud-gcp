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

import org.springframework.context.ApplicationContextAware;
import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * Persistent entity for Google Cloud Datastore
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface DatastorePersistentEntity<T> extends
		MutablePersistentEntity<T, DatastorePersistentProperty>, ApplicationContextAware {

	/**
	 * Gets the name of the Datastore Entity.
	 * @return the name of the Entity.
	 */
	String kindName();

	/**
	 * Gets the ID property, and will throw {@link DatastoreDataException} if the entity
	 * does not have an ID property.
	 * @return the ID property.
	 */
	DatastorePersistentProperty getIdPropertyOrFail();
}
