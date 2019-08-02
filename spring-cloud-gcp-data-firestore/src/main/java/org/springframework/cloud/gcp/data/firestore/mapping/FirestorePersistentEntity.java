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

import org.springframework.data.mapping.model.MutablePersistentEntity;

/**
 * Persistent entity for Google Cloud Firestore.
 *
 * @param <T> the type of the property described by this persistent property
 *
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
public interface FirestorePersistentEntity<T> extends
		MutablePersistentEntity<T, FirestorePersistentProperty> {

	/**
	 * Gets the name of the Firestore Collection.
	 *
	 * @return the name of the Firestore Collection that stores these entities.
	 */
	String collectionName();

	/**
	 * Gets the ID property, and will throw {@link Exception} if the entity
	 * does not have an ID property.
	 *
	 * @return the ID property.
	 */
	FirestorePersistentProperty getIdPropertyOrFail();
}
