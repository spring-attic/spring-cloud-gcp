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

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;

/**
 * Interface for a factory that creates Spanner mutation operation objects.
 *
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public interface SpannerMutationFactory {

	/**
	 * Stores a single object in Spanner.
	 * @param object The object to store.
	 * @return The mutation operation which will store the object.
	 */
	Mutation insert(Object object);

	/**
	 * Updates or inserts a single object in Spanner. The columns' values corresponding to
	 * the object's fields are treated according to Mutation.Op.INSERT_OR_UPDATE.
	 * @param object The object to update or newly insert.
	 * @return The mutation operation to perform the action.
	 */
	Mutation upsert(Object object);

	/**
	 * Updates a single object in Spanner. The columns' values corresponding to the
	 * object's fields are treated according to Mutation.Op.UPDATE.
	 * @param object The object to update.
	 * @return The mutation operation to perform the action.
	 */
	Mutation update(Object object);

	/**
	 * Deletes several objects from Spanner.
	 * @param entityClass The type of the objects to delete.
	 * @param entities A list of objects to delete. Each object can be a subtype of
	 * entityClass.
	 * @param <T> The type of object to delete.
	 * @return The delete mutation.
	 */
	<T> Mutation delete(Class<T> entityClass, Iterable<? extends T> entities);

	/**
	 * Deletes a single object from Spanner.
	 * @param object The object to delete.
	 * @return The delete mutation.
	 */
	Mutation delete(Object object);

	/**
	 * Deletes a set of keys from Spanner.
	 * @param entityClass The type of the entity to delete.
	 * @param keys The keys of the objects to delete.
	 * @return The delete mutation.
	 */
	Mutation delete(Class entityClass, KeySet keys);

	/**
	 * Deletes a key from Spanner.
	 * @param entityClass The type of the entity to delete.
	 * @param key The key of the object to delete.
	 * @return The delete mutation.
	 */
	Mutation delete(Class entityClass, Key key);
}
