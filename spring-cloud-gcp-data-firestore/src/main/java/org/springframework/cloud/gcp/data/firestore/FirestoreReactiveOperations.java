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

import com.google.firestore.v1.StructuredQuery;
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
	 * @return {@link Mono} emitting the saved entity.
	 */
	<T> Mono<T> save(T instance);

	/**
	 * Saves multiple objects to Cloud Firestore. Not atomic. Behaves as insert or update.
	 * @param instances the objects to save.
	 * @param <T> the type of the objects to save.
	 * @return {@link Flux} emitting the saved entities.
	 */
	<T> Flux<T> saveAll(Publisher<T> instances);

	/**
	 * Get all the entities of the given domain type.
	 * @param <T> the type param of the domain type.
	 * @param entityClass the domain type to get.
	 * @return {@link Mono} emitting the found entities.
	 */
	<T> Flux<T> findAll(Class<T> entityClass);

	/**
	 * Delete all entities of a given domain type.
	 * @param <T> the type param of the domain type.
	 * @param entityClass the domain type to delete from Cloud Datastore.
	 * @return {@link Mono} emitting the number of deleted entities.
	 */
	<T> Mono<Long> deleteAll(Class<T> entityClass);

	/**
	 * Test if the entity of the given domain type with a given id exists.
	 * Uses the first emitted element to perform the query.
	 * @param <T> the type param of the domain type.
	 * @param entityClass the domain type of the entity.
	 * @param idPublisher publisher that provides an id.
	 * @return {@link Mono} emitting {@code true} if an entity with the given id exists, {@code false} otherwise.
	 */
	<T> Mono<Boolean> existsById(Publisher<String> idPublisher, Class<T> entityClass);

	/**
	 * Get an entity of the given domain type by id.
	 * Uses the first emitted element to perform the query.
	 * @param <T> the type param of the domain type.
	 * @param idPublisher publisher that provides an id.
	 * @param entityClass the domain type of the entity.
	 * @return {@link Mono} emitting the found entity.
	 */
	<T> Mono<T> findById(Publisher<String> idPublisher, Class<T> entityClass);

	/**
	 * Get an entity of the given domain type by id.
	 * @param <T> the type param of the domain type.
	 * @param idPublisher publisher that provides ids.
	 * @param entityClass the domain type of the entity.
	 * @return {@link Flux} emitting the found entities.
	 */
	<T> Flux<T> findAllById(Publisher<String> idPublisher, Class<T> entityClass);

	/**
	 * Count entities of the given domain.
	 * Note that Firestore doesn't support "count" operation natively,
	 * so id query will be executed and all ids will be retrieved so they could be counted.
	 * @param <T> the type param of the domain type.
	 * @param entityClass the domain type of entities.
	 * @return {@link Mono} emitting the number of entities.
	 */
	<T> Mono<Long> count(Class<T> entityClass);

	/**
	 * Count entities of the given domain corresponding to the predicates given in the query builder.
	 * Note that Firestore doesn't support "count" operation natively,
	 * so id query will be executed and all ids will be retrieved so they could be counted.
	 * @param <T> the type param of the domain type.
	 * @param entityClass the domain type of entities.
	 * @param queryBuilder the query builder that contains predicates;
	 *                     note that id projection and the collection name will be set during execution
	 * @return {@link Mono} emitting the number of entities.
	 */
	<T> Mono<Long> count(Class<T> entityClass, StructuredQuery.Builder queryBuilder);

	/**
	 * Delete entities provided by publisher.
	 * @param <T> the type param of the domain type.
	 * @param entityPublisher publisher that provides entities to be removed.
	 * @return {@link Mono} signaling when operation has completed.
	 */
	<T> Mono<Void> delete(Publisher<T> entityPublisher);

	/**
	 * Delete entities of a given domain type using ids published by producer.
	 * @param <T> the type param of the domain type.
	 * @param idPublisher publisher that provides ids of entities to be removed.
	 * @param entityClass the domain type of entities.
	 * @return {@link Mono} signaling when operation has completed.
	 */
	<T> Mono<Void> deleteById(Publisher<String> idPublisher, Class entityClass);

	/**
	 * Executes a query represented as query builder and returns results of the given domain type.
	 * @param <T> the type param of the domain type.
	 * @param builder the query builder.
	 * @param entityClass the domain type of entities.
	 * @return {@link Flux} emitting the found entities.
	 * @since 1.2.4
	 */
	<T> Flux<T> execute(StructuredQuery.Builder builder, Class<T> entityClass);

	/**
	 * Creates FirestoreReactiveOperations object with a provided parent.
	 * The parent doesn't have to exist in Firestore, but should have a non-empty id field.
	 * All operations and queries will be scoped to that parent's subcollections.
	 * By default, FirestoreReactiveOperations uses the root as a "parent".
	 * @param <T> the type param of the parent.
	 * @param parent the query builder.
	 * @return template with a given parent.
	 * @since 1.2.4
	 */
	<T> FirestoreReactiveOperations withParent(T parent);

}
