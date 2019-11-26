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

import java.util.Map;

import com.google.firestore.v1.Document;
import com.google.firestore.v1.Value;

/**
 * An interface used for object mapping for Cloud Firestore.
 *
 * @author Dmitry Solomakha
 */
public interface FirestoreClassMapper {
	/**
	 * Converts an entity to a Firestore type.
	 *
	 * @param <T> the type of the object to convert
	 * @param entity the object to convert
	 * @return value that can be saved in Firestore
	 */
	<T> Value convertToFirestoreValue(T entity);

	/**
	 * Converts an entity to a map where key is the property name and the value is a Firestore type.
	 *
	 * @param <T> the type of the object to convert
	 * @param entity the object to convert
	 * @return a map where key is the property name and the value is a Firestore type
	 */
	<T> Map<String, Value> convertToFirestoreTypes(T entity);

	/**
	 * Converts a Firestore document to a Java object.
	 *
	 * @param <T> the type of the target object
	 * @param document the document to convert
	 * @param clazz the type of the target object
	 * @return Java object that the Firestore document was concerted to
	 */
	<T> T convertToCustomClass(Document document, Class<T> clazz);
}
