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

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.ReadOption;
import java.util.List;
import org.springframework.lang.Nullable;

/**
 * An interface of operations that can be done with Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface DatastoreOperations {

  /**
   * Get an entity based on a key.
   * @param entityClass the type of the entity to get.
   * @param key the key of the entity
   * @param <T> the class type of the entity.
   * @param readOption optional read options.
   * @return the entity that was found with that key.
   */
  <T> T read(Class<T> entityClass, Key key, @Nullable DatastoreReadOptions readOption);

  /**
   * Get a list of entities based on a list of keys.
   * @param entityClass the class type of the entities to get.
   * @param keys the keys of the entities to get.
   * @param <T> the type of the class
   * @param readOption optional read options.
   * @return the list of entities that were found.
   */
  <T> List<T> read(Class<T> entityClass, Iterable<Key> keys, @Nullable DatastoreReadOptions readOption);

  /**
   * Get a list of all entities of the given class type.
   * @param entityClass the class type of entity to retrieve.
   * @param <T> the type of the entity.
   * @param readOption optional read options.
   * @return a list of entities of the given class type.
   */
  <T> List<T> readAll(Class<T> entityClass, @Nullable DatastoreReadOptions readOption);

  /**
   * Get a list of entities using a query.
   * @param entityClass the type of the entity to retrieve.
   * @param query the query to run
   * @param readOption optional read options.
   * @param <T> the class type of the entity.
   * @return a list of entities found.
   */
  <T> List<T> query(Class<T> entityClass, Query<Entity> query, @Nullable DatastoreReadOptions readOption);

  /**
   * Delete an entity from Cloud Datastore based on a key.
   * @param key the key of the entity to delete.
   */
  void delete(Key key);

  /**
   * Delete multiple entities from Cloud Datastore based on their keys.
   * @param keys the keys of the entities to delete.
   */
  void delete(Key... keys);

  /**
   * Delete an entity from Cloud Datastore.
   * @param entity the object that will be deleted based on its key value.
   */
  void delete(Object entity);

  /**
   * Update an object to Cloud Datastore. Will fail if an entity with the same key column value
   * doesn't exist in Cloud Datastore.
   * @param entity the object to update.
   */
  void update(Object entity);

  /**
   * Update or insert an object to Cloud Datastore.
   * @param entity the object to update or insert.
   */
  void put(Object entity);

  /**
   * Count the number of entities of the given class in Cloud Datastore.
   * @param entityClass the class type to count.
   * @return the number of entities.
   */
  long count(Class entityClass);
}
