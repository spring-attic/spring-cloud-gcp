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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;

/**
 * An interface of operations that can be done with Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface DatastoreOperations {

	/**
	 * Get an entity based on a id.
	 * @param id the id of the entity. If this is actually a
	 * {@link com.google.cloud.datastore.Key} then it will be used. Otherwise it will be
	 * attempted to be converted to an integer or string value and it will be assumed to
	 * be a root key value with the Kind determined by the entityClass. param.
	 * @param entityClass the type of the entity to get.
	 * @param <T> the class type of the entity.
	 * @return the entity that was found with that id.
	 */
	<T> T findById(Object id, Class<T> entityClass);

	/**
	 * Saves an instance of an object to Cloud Datastore.
	 * Behaves as update or insert.
	 * Ancestors can be added only to entries with Key ids.
	 * @param instance the instance to save.
	 * @param ancestors ancestors that should be added to the entry
	 * @return the instance that was saved.
	 */
	<T> T save(T instance, Key... ancestors);

	/**
	 * Saves multiple instances of objects to Cloud Datastore.
	 * Behaves as update or insert.
	 * Ancestors can be added only to entries with Key ids.
	 * @param entities the objects to save.
	 * @param ancestors ancestors that should be added to each entry
	 * @return the entities that were saved.
	 */
	<T> Iterable<T> saveAll(Iterable<T> entities, Key... ancestors);

	/**
	 * Delete an entity from Cloud Datastore. Deleting IDs that do not exist in Cloud
	 * Datastore will result in no operation.
	 * @param id the ID of the entity to delete. If this is actually a
	 * {@link com.google.cloud.datastore.Key} then it will be used. Otherwise it will be
	 * attempted to be converted to an integer or string value and it will be assumed to
	 * be a root key value with the Kind determined by the entityClass.
	 * @param entityClass the type of the
	 * @param <T> ths entity type
	 */
	<T> void deleteById(Object id, Class<T> entityClass);

	/**
	 * Delete multiple IDs from Cloud Datastore. Deleting IDs that do not exist in Cloud
	 * Datastore will result in no operation.
	 * @param ids the IDs to delete. If any of these is actually a
	 * {@link com.google.cloud.datastore.Key} then it will be used. Otherwise it will be
	 * attempted to be converted to an integer or string value and it will be assumed to
	 * be a root key value with the Kind determined by the entityClass.
	 * @param entityClass the type of the
	 * @param <T> ths entity type
	 */
	<T> void deleteAllById(Iterable<?> ids, Class<T> entityClass);

	/**
	 * Delete an entity from Cloud Datastore. Deleting entities that don't exist in Cloud
	 * Datastore will result in no operation.
	 * @param entity the entity to delete.
	 * @param <T> the entity type
	 */
	<T> void delete(T entity);

	/**
	 * Deletes multiple entities from Cloud Datastore. Deleting entities that don't exist
	 * in Cloud Datastore will result in no operation.
	 * @param entities the entities to delete.
	 * @param <T> the entity type.
	 */
	<T> void deleteAll(Iterable<T> entities);

	/**
	 * Delete all entities of a given domain type.
	 * @param entityClass the domain type to delete from Cloud Datastore.
	 * @return the number of entities that were deleted.
	 */
	long deleteAll(Class<?> entityClass);

	/**
	 * Count all occurrences of entities of the given domain type.
	 * @param entityClass the domain type to count.
	 * @return the number of entities of the given type.
	 */
	long count(Class<?> entityClass);

	/**
	 * Find all the entities of the given IDs. If an ID is actually a
	 * {@link com.google.cloud.datastore.Key} then it will be used. Otherwise it will be
	 * attempted to be converted to an integer or string value and it will be assumed to
	 * be a root key value with the Kind determined by the entityClass.
	 * @param ids the IDs to search.
	 * @param entityClass the domain type of the objects.
	 * @param <T> the type parameter of the domain type.
	 * @return the entities that were found.
	 */
	<T> Iterable<T> findAllById(Iterable<?> ids, Class<T> entityClass);

	/**
	 * Finds objects by using a Cloud Datastore query.
	 * @param query the query to execute.
	 * @param entityClass the type of object to retrieve.
	 * @param <T> the type of object to retrieve.
	 * @return a list of the objects found. If no keys could be found the list will be
	 * empty.
	 */
	<T> Iterable<T> query(Query<? extends BaseEntity> query, Class<T> entityClass);

	/**
	 * Runs given query and applies given function to each entity in the result.
	 * @param query the query to run.
	 * @param entityFunc the function to apply to each found entity or key.
	 * @param <A> the row type of the query. This type can be either {@code Key} or a
	 * Cloud Datastore entity.
	 * @param <T> the type to map each entity or key to.
	 * @return the mapped entities or keys.
	 */
	<A, T> Iterable<T> query(Query<A> query, Function<A, T> entityFunc);

	/**
	 * Finds Cloud Datastore Keys by using a Cloud Datastore query.
	 * @param query the query to execute that retrieves only Keys.
	 * @return the list of keys found.
	 */
	Iterable<Key> queryKeys(Query<Key> query);

	/**
	 * Get all the entities of the given domain type.
	 * @param entityClass the domain type to get.
	 * @param <T> the type param of the domain type.
	 * @return the entities that were found.
	 */
	<T> Iterable<T> findAll(Class<T> entityClass);

	/**
	 * Get all the entities of the given domain type applying limit, offset and sort
	 * @param entityClass the domain type to get.
	 * @param queryOptions query options
	 * @param <T> the type param of the domain type.
	 * @return the entities that were found.
	 */
	<T> Collection<T> findAll(Class<T> entityClass, DatastoreQueryOptions queryOptions);

	/**
	 * Check if the given ID belongs to an entity in Cloud Datastore. If this is actually
	 * a {@link com.google.cloud.datastore.Key} then it will be used. Otherwise it will be
	 * attempted to be converted to an integer or string value and it will be assumed to
	 * be a root key value with the Kind determined by the entityClass.
	 * @param id the ID to search for.
	 * @param entityClass the domain type of the entities to search for.
	 * @param <T> the type param of the domain type.
	 * @return true if the given ID refers to an existing entity. False otherwise.
	 */
	<T> boolean existsById(Object id, Class<T> entityClass);

	/**
	 * Performs multiple read and write operations in a single transaction.
	 * @param operations the function that uses {@link DatastoreOperations}
	 * to perform operations in a transaction.
	 * @param <A> the final return type of the operations.
	 * @return the final result of the transaction.
	 */
	<A> A performTransaction(Function<DatastoreOperations, A> operations);

	/**
	 * Get a Datastore entity based on a id and convert it to a map.
	 * @param key the key of the entity
	 * @param valueType type values should be converted to
	 * @return if an entity for a given key exists, returns the map representation of it,
	 * {@code null} otherwise
	 */
	<T> Map<String, T> findByIdAsMap(Key key, Class<T> valueType);

	/**
	 * Save a map as a Datastore entity, using map keys as field names
	 * @param key the key for the entity
	 * @param map a map
	 */
	<V> void writeMap(Key key, Map<String, V> map);

	/**
	 * Create a {@link com.google.cloud.datastore.Key} from kind name and id.
	 * @param kind the Cloud Datastore kind name
	 * @param id object to be used as id; if it is a Long, the value is used, otherwise it is
	 * converted to String
	 * @return created key
	 */
	Key createKey(String kind, Object id);
}
