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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;

/**
 * Defines operations available to use with Spanner.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface SpannerOperations {

	/**
	 * Finds a single stored object using a key.
	 * @param entityClass the type of the object to retrieve.
	 * @param key the key of the object.
	 * @param <T> the type of the object to retrieve.
	 * @return an object of the requested type. Returns null if no object could be found
	 * stored with the given key.
	 */
	<T> T read(Class<T> entityClass, Key key);

	/**
	 * Finds a single stored object using a key.
	 * @param entityClass the type of the object to retrieve.
	 * @param key the key of the object.
	 * @param options Cloud Spanner read options with which to conduct the read operation.
	 * @param <T> the type of the object to retrieve.
	 * @return an object of the requested type. Returns null if no object could be found
	 * stored with the given key.
	 */
	<T> T read(Class<T> entityClass, Key key, SpannerReadOptions options);

	/**
	 * Finds objects stored from their keys.
	 * @param entityClass the type of the object to retrieve.
	 * @param keys the keys of the objects to retrieve.
	 * @param options Cloud Spanner read options with which to conduct the read operation.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of objects that could be found using the given keys. If no keys
	 * could be found the list will be empty.
	 */
	<T> List<T> read(Class<T> entityClass, KeySet keys, SpannerReadOptions options);

	/**
	 * Finds objects stored from their keys.
	 * @param entityClass the type of the object to retrieve.
	 * @param keys the keys of the objects to retrieve.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of objects that could be found using the given keys. If no keys
	 * could be found the list will be empty.
	 */
	<T> List<T> read(Class<T> entityClass, KeySet keys);

	/**
	 * Executes a given query string with tags and parameters and applies a given function
	 * to each row of the result.
	 * @param rowFunc the function to apply to each row of the result.
	 * @param statement the SQL statement used to select the objects.
	 * @param options the options with which to run this query.
	 * @return a list of the rows each transformed with the given function.
	 */
	<A> List<A> query(Function<Struct, A> rowFunc, Statement statement,
			SpannerQueryOptions options);

	/**
	 * Finds objects by using an SQL statement.
	 * @param entityClass the type of object to retrieve.
	 * @param statement the SQL statement used to select the objects.
	 * @param options Cloud Spanner read options with which to conduct the read operation.
	 * @param <T> the type of object to retrieve.
	 * @return a list of the objects found. If no keys could be found the list will be
	 * empty.
	 */
	<T> List<T> query(Class<T> entityClass, Statement statement,
			SpannerQueryOptions options);

	/**
	 * Finds all objects of the given type.
	 * @param entityClass the type of the object to retrieve.
	 * @param options Cloud Spanner read options with which to conduct the read operation.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of all objects stored of the given type. If there are no objects an
	 * empty list is returned.
	 */
	<T> List<T> readAll(Class<T> entityClass, SpannerReadOptions options);

	/**
	 * Finds all objects of the given type.
	 * @param entityClass the type of the object to retrieve.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of all objects stored of the given type. If there are no objects an
	 * empty list is returned.
	 */
	<T> List<T> readAll(Class<T> entityClass);

	/**
	 * Finds all objects of the given type.
	 * @param entityClass the type of the object to retrieve.
	 * @param options Cloud Spanner query options with which to conduct the query
	 * operation.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of all objects stored of the given type. If there are no objects an
	 * empty list is returned.
	 */
	<T> List<T> queryAll(Class<T> entityClass, SpannerPageableQueryOptions options);

	/**
	 * Deletes an object based on a key.
	 * @param entityClass the type of the object to delete.
	 * @param key the key of the object to delete from storage.
	 */
	void delete(Class entityClass, Key key);

	/**
	 * Deletes an object from storage.
	 * @param object the object to delete from storage.
	 */
	void delete(Object object);

	/**
	 * Deletes objects from storage in a batch.
	 * @param objects the objects to delete from storage.
	 */
	void deleteAll(Iterable objects);

	/**
	 * Deletes objects given a set of keys.
	 * @param entityClass the type of object to delete.
	 * @param keys the keys of the objects to delete.
	 */
	void delete(Class entityClass, KeySet keys);

	/**
	 * Insert an object into storage.
	 * @param object the object to insert.
	 */
	void insert(Object object);

	/**
	 * Insert objects into storage in batch.
	 * @param objects the objects to insert.
	 */
	void insertAll(Iterable objects);

	/**
	 * Update an object already in storage.
	 * @param object the object to update.
	 */
	void update(Object object);

	/**
	 * Update objects in batch.
	 * @param objects the objects to update.
	 */
	void updateAll(Iterable objects);

	/**
	 * Update an object in storage.
	 * @param object the object to update.
	 * @param includeProperties the properties to upsert. if none are given then all
	 * properties are used
	 */
	void update(Object object, String... includeProperties);

	/**
	 * Update an object in storage.
	 * @param object the object to update.
	 * @param includeProperties the properties to update. If null is given, then all
	 * properties are used. Note that an empty {@code Set} means that no properties will
	 * be used.
	 */
	void update(Object object, Set<String> includeProperties);

	/**
	 * Update or insert an object into storage.
	 * @param object the object to update or insert.
	 */
	void upsert(Object object);

	/**
	 * Update or insert objects into storage in batch.
	 * @param objects the objects to update or insert.
	 */
	void upsertAll(Iterable objects);

	/**
	 * Update or insert an object into storage.
	 * @param object the object to update or insert.
	 * @param includeProperties the properties to upsert. if none are given then all
	 * properties are upserted.
	 */
	void upsert(Object object, String... includeProperties);

	/**
	 * Update or insert an object into storage.
	 * @param object the object to update or insert.
	 * @param includeProperties the properties to update. If null is given, then all
	 * properties are used. Note that an empty {@code Set} means that no properties will
	 * be used.
	 */
	void upsert(Object object, Set<String> includeProperties);

	/**
	 * Count how many objects are stored of the given type.
	 * @param entityClass the type of object to count.
	 * @return the number of stored objects.
	 */
	long count(Class entityClass);

	/**
	 * Performs multiple read and write operations in a single transaction.
	 * @param operations the function representing the operations to perform using a
	 * SpannerOperations based on a single transaction.
	 * @param <T> the final return type of the operations.
	 * @return the final result of the transaction.
	 */
	<T> T performReadWriteTransaction(Function<SpannerTemplate, T> operations);

	/**
	 * Performs multiple read-only operations in a single transaction.
	 * @param operations the function representing the operations to perform using a
	 * SpannerOperations based on a single transaction.
	 * @param readOptions allows the user to specify staleness for the read transaction
	 * @param <T> the final return type of the operations.
	 * @return the final result of the transaction.
	 */
	<T> T performReadOnlyTransaction(Function<SpannerTemplate, T> operations,
			SpannerReadOptions readOptions);
}
