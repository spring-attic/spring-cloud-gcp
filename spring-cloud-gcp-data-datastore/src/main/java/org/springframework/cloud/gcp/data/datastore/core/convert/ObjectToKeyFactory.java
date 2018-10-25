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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;

/**
 * An interface for creating Datastore Keys from objects and ID values.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public interface ObjectToKeyFactory {

	/**
	 * Get an {@link IncompleteKey} (a Key without ID part) from a kind name.
	 * @param kindName the kind name
	 * @return an IncompleteKey.
	 */
	IncompleteKey getIncompleteKey(String kindName);

	/**
	 * Get a {@link Key} from a provided ID value and a kind name. If the given ID value is
	 * already a Key then this is the Key returned. Otherwise a Key is created with the
	 * given kind name and the given ID value as the only and root value.
	 * @param id the ID value that can be the root single ID value or a fully formed Key.
	 * @param kindName the kind name used if the ID value provided is not a fully formed
	 * Key.
	 * @return a Key.
	 */
	Key getKeyFromId(Object id, String kindName);

	/**
	 * Get a {@link Key} from an entity.
	 * @param entity the entity that whose ID value we want to form into a Key.
	 * @param datastorePersistentEntity the metadata of the given entity.
	 * @return a Key.
	 */
	Key getKeyFromObject(Object entity, DatastorePersistentEntity datastorePersistentEntity);

	/**
	 * Allocates a new ID {@link Key} for the given entity object and sets the allocated ID value in the
	 * object.
	 * Only Key ids are allowed in entities if ancestors are present.
	 * @param entity the object for which to get and set the ID value.
	 * @param datastorePersistentEntity the persistent entity metadata for the entity object.
	 * @param ancestors ancestors that should be added to the entity
	 * @return the newly allocated Key.
	 */
	Key allocateKeyForObject(Object entity, DatastorePersistentEntity datastorePersistentEntity, Key... ancestors);
}
