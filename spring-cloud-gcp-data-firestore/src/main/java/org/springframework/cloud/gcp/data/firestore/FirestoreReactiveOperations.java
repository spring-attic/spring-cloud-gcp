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

package org.springframework.cloud.gcp.data.firestore;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * An interface of operations that can be done with Cloud Firestore.
 *
 * @author Dmitry Solomakha
 * @since 1.2
 */
public interface FirestoreReactiveOperations {
	/**
	 * Saves an instance of an object to Cloud Firestore. Behaves as an insert only (not
	 * update).
	 * @param <T> the type of the object to save.
	 * @param instance the instance to save.
	 * @return the instance that was saved.
	 */
	<T> Mono<T> save(T instance);

	/**
	 * Saves multiple objects to Cloud Firestore. Not atomic. Behaves as insert or update.
	 * @param instances the objects to save.
	 * @param <T> the type of the objects to save.
	 * @return a {@link Flux} of the instances that were saved
	 */
	<T> Flux<T> saveAll(Publisher<T> instances);

	/**
	 * Get all the entities of the given domain type.
	 * @param <T> the type param of the domain type.
	 * @param entityClass the domain type to get.
	 * @return the entities that were found.
	 */
	<T> Flux<T> findAll(Class<T> entityClass);

	/**
	 * Delete all entities of a given domain type.
	 * @param <T> the type param of the domain type.
	 * @param entityClass the domain type to delete from Cloud Datastore.
	 * @return the number of entities that were deleted.
	 */
	<T> Mono<Long> deleteAll(Class<T> entityClass);
}
