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

package org.springframework.cloud.gcp.data.firestore.mapping;

import com.google.firestore.v1.Document;
import com.google.firestore.v1.Value;

/**
 * An interface used for object mapping for Cloud Firestore.
 *
 * @author Dmitry Solomakha
 * @author Mike Eltsufin
 * @since 1.2.2
 */
public interface FirestoreClassMapper {
	/**
	 * Converts an entity to a Firestore type.
	 *
	 * @param <T> the type of the object to convert
	 * @param sourceValue the object to convert
	 * @return value that can be used to bind to a Firestore query
	 */
	<T> Value toFirestoreValue(T sourceValue);

	/**
	 * Converts an entity to a Firestore document.
	 *
	 * @param <T> the type of the object to convert
	 * @param entity the object to convert
	 * @param documentResourceName the fully-qualified identifier of the document
	 * @return a {@link Document} that can be stored in Firestore
	 */
	<T> Document entityToDocument(T entity, String documentResourceName);

	/**
	 * Converts a Firestore document to an entity.
	 *
	 * @param <T> the type of the target object
	 * @param document the {@link Document} to convert
	 * @param clazz the type of the target entity
	 * @return the entity that the Firestore document was converted to
	 */
	<T> T documentToEntity(Document document, Class<T> clazz);
}
