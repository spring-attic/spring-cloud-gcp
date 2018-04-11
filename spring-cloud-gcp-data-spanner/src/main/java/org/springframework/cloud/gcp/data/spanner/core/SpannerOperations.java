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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Statement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Defines operations available to use with Spanner.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
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
	 * @param options Spanner read options with which to conduct the read operation.
	 * @param <T> the type of the object to retrieve.
	 * @return an object of the requested type. Returns null if no object could be found
	 * stored with the given key.
	 */
	<T> T read(Class<T> entityClass, Key key, SpannerReadOptions options);

	/**
	 * Finds objects stored from their keys.
	 * @param entityClass the type of the object to retrieve.
	 * @param keys the keys of the objects to retrieve.
	 * @param options Spanner read options with which to conduct the read operation.
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
	 * Finds objects by using an SQL statement.
	 * @param entityClass the type of object to retrieve.
	 * @param statement the SQL statement used to select the objects.
	 * @param options Spanner read options with which to conduct the read operation.
	 * @param <T> the type of object to retrieve.
	 * @return a list of the objects found. If no keys could be found the list will be
	 * empty.
	 */
	<T> List<T> query(Class<T> entityClass, Statement statement,
			SpannerQueryOptions options);

	/**
	 * Finds objects by using an SQL statement.
	 * @param entityClass the type of object to retrieve.
	 * @param statement the SQL statement used to select the objects.
	 * @param <T> the type of object to retrieve.
	 * @return a list of the objects found. If no keys could be found the list will be
	 * empty.
	 */
	<T> List<T> query(Class<T> entityClass, Statement statement);

	/**
	 * Finds all objects of the given type.
	 * @param entityClass the type of the object to retrieve.
	 * @param options Spanner read options with which to conduct the read operation.
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
	 * @param sort the sorting used for the results.
	 * @param options Spanner query options with which to conduct the query operation.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of all objects stored of the given type. If there are no objects an
	 * empty list is returned.
	 */
	<T> List<T> queryAll(Class<T> entityClass, Sort sort, SpannerQueryOptions options);

	/**
	 * Finds all objects of the given type.
	 * @param entityClass the type of the object to retrieve.
	 * @param sort the sorting used for the results.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of all objects stored of the given type. If there are no objects an
	 * empty list is returned.
	 */
	<T> List<T> queryAll(Class<T> entityClass, Sort sort);

	/**
	 * Finds all objects of the given type.
	 * @param entityClass the type of the object to retrieve.
	 * @param pageable the paging options for this request.
	 * @param options Spanner query options with which to conduct the query operation.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of all objects stored of the given type. If there are no objects an
	 * empty list is returned.
	 */
	<T> Page<T> queryAll(Class<T> entityClass, Pageable pageable,
			SpannerQueryOptions options);

	/**
	 * Finds all objects of the given type.
	 * @param entityClass the type of the object to retrieve.
	 * @param pageable the paging options for this request.
	 * @param <T> the type of the object to retrieve.
	 * @return a list of all objects stored of the given type. If there are no objects an
	 * empty list is returned.
	 */
	<T> Page<T> queryAll(Class<T> entityClass, Pageable pageable);

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
	 * Deletes multiple objects from storage.
	 * @param entityClass the type of the object to delete.
	 * @param objects the objects to delete.
	 * @param <T> the type of the object to delete.
	 */
	<T> void delete(Class<T> entityClass, Iterable<? extends T> objects);

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
	 * Update an object already in storage.
	 * @param object the object to update.
	 */
	void update(Object object);

	/**
	 * Update an object in storage.
	 * @param object the object to update.
	 * @param includeColumns the columns to upsert. if none are given then all columns are
	 * used
	 */
	void update(Object object, String... includeColumns);

	/**
	 * Update an object in storage.
	 * @param object the object to update.
	 * @param includeColumns the columns to update. If null or an empty Optional is given, then
	 * all columns are used. Note that an Optional occupied by an empty Set means that no columns
	 * will be used.
	 */
	void update(Object object, Optional<Set<String>> includeColumns);

	/**
	 * Update or insert an object into storage.
	 * @param object the object to update or insert.
	 */
	void upsert(Object object);

	/**
	 * Update or insert an object into storage.
	 * @param object the object to update or insert.
	 * @param includeColumns the columns to upsert. if none are given then all columns are
	 * upserted.
	 */
	void upsert(Object object, String... includeColumns);

	/**
	 * Update or insert an object into storage.
	 * @param object the object to update or insert.
	 * @param includeColumns the columns to upsert. If null or an empty Optional is given, then
	 * all columns are used. Note that an Optional occupied by an empty Set means that no columns
	 * will be used.
	 */
	void upsert(Object object, Optional<Set<String>> includeColumns);

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
	<T> T performReadWriteTransaction(Function<SpannerOperations, T> operations);

	/**
	 * Performs multiple read-only operations in a single transaction.
	 * @param operations the function representing the operations to perform using a
	 * SpannerOperations based on a single transaction.
	 * @param readOptions allows the user to specify staleness for the read transaction
	 * @param <T> the final return type of the operations.
	 * @return the final result of the transaction.
	 */
	<T> T performReadOnlyTransaction(Function<SpannerOperations, T> operations,
			SpannerReadOptions readOptions);
}
