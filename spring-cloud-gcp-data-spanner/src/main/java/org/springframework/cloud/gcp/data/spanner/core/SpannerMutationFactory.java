/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.List;
import java.util.Set;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;

/**
 * Interface for a factory that creates Cloud Spanner mutation operation objects.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface SpannerMutationFactory {

	/**
	 * Stores an object and its interleaved child properties in Cloud Spanner.
	 * There will be 1 mutation for each entity corresponding to a row in Cloud Spanner. If there
	 * are no interleaved children then the returned list will contain the single mutation for the
	 * given object.
	 * @param object the object to store.
	 * @return the mutation operations which will store the object and its children.
	 */
	List<Mutation> insert(Object object);

	/**
	 * Updates or inserts an object and its interleaved child properties in Cloud Spanner.
	 * There will be 1 mutation for each entity corresponding to a row in Cloud Spanner.
	 * If there are no interleaved children then the returned list will contain the single
	 * mutation for the given object.
	 * @param object the object to update or newly insert.
	 * @param includeProperties the properties to use in the operation. if null then all
	 * properties are used.
	 * @return the mutation operations which will store the object and its children.
	 */
	List<Mutation> upsert(Object object, Set<String> includeProperties);

	/**
	 * Updates an object and its interleaved child properties in Cloud Spanner. There will
	 * be 1 mutation for each entity corresponding to a row in Cloud Spanner. If there are
	 * no interleaved children then the returned list will contain the single mutation for
	 * the given object.
	 * @param object the object to update.
	 * @param includeProperties the properties to use in the operation. if null then all
	 * properties are used.
	 * @return the mutation operations which will store the object and its children.
	 */
	List<Mutation> update(Object object, Set<String> includeProperties);

	/**
	 * Deletes several objects from Spanner.
	 * @param entityClass the type of the objects to delete.
	 * @param entities a list of objects to delete. Each object can be a subtype of
	 * entityClass.
	 * @param <T> the type of object to delete.
	 * @return the delete mutation.
	 */
	<T> Mutation delete(Class<T> entityClass, Iterable<? extends T> entities);

	/**
	 * Deletes a single object from Spanner.
	 * @param object the object to delete.
	 * @param <T> the type of the object to delete.
	 * @return the delete mutation.
	 */
	<T> Mutation delete(T object);

	/**
	 * Deletes a set of keys from Spanner.
	 * @param entityClass the type of the entity to delete.
	 * @param keys the keys of the objects to delete.
	 * @return the delete mutation.
	 */
	Mutation delete(Class entityClass, KeySet keys);

	/**
	 * Deletes a key from Spanner.
	 * @param entityClass the type of the entity to delete.
	 * @param key the key of the object to delete.
	 * @return the delete mutation.
	 */
	Mutation delete(Class entityClass, Key key);
}
