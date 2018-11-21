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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * Implementation of {@link ObjectToKeyFactory} where the key factories are provided by
 * the Datastore Service.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreServiceObjectToKeyFactory implements ObjectToKeyFactory {

	private final Datastore datastore;

	public DatastoreServiceObjectToKeyFactory(Datastore datastore) {
		Assert.notNull(datastore, "A non-null Datastore service is required.");
		this.datastore = datastore;
	}

	@Override
	public IncompleteKey getIncompleteKey(String kindName) {
		return this.datastore.newKeyFactory().setKind(kindName).newKey();
	}

	@Override
	public Key getKeyFromId(Object id, String kindName) {
		Assert.notNull(id, "Cannot get key for null ID value.");
		if (id instanceof Key) {
			return (Key) id;
		}
		KeyFactory keyFactory = getKeyFactory();
		keyFactory.setKind(kindName);
		Key key;
		if (id instanceof String) {
			key = keyFactory.newKey((String) id);
		}
		else if (id instanceof Long) {
			key = keyFactory.newKey((long) id);
		}
		else {
			// We will use configurable custom converters to try to convert other types to
			// String or long
			// in the future.
			throw new DatastoreDataException(
					"Keys can only be created using String or long values.");
		}
		return key;
	}

	@Override
	public Key getKeyFromObject(Object entity, DatastorePersistentEntity datastorePersistentEntity) {
		Assert.notNull(entity, "Cannot get key for null entity object.");
		Assert.notNull(datastorePersistentEntity, "Persistent entity must not be null.");
		PersistentProperty idProp = datastorePersistentEntity.getIdPropertyOrFail();
		Object idVal = datastorePersistentEntity.getPropertyAccessor(entity).getProperty(idProp);
		if (idVal == null) {
			return null;
		}
		else {
			return getKeyFromId(idVal, datastorePersistentEntity.kindName());
		}
	}

	@Override
	public Key allocateKeyForObject(Object entity,
			DatastorePersistentEntity datastorePersistentEntity, Key... ancestors) {
		Assert.notNull(entity, "Cannot get key for null entity object.");
		Assert.notNull(datastorePersistentEntity, "Persistent entity must not be null.");
		PersistentProperty idProp = datastorePersistentEntity.getIdPropertyOrFail();

		KeyFactory keyFactory = getKeyFactory().setKind(datastorePersistentEntity.kindName());
		Class idPropType = idProp.getType();
		if (ancestors != null && ancestors.length > 0) {
			if (!idPropType.equals(Key.class)) {
				throw new DatastoreDataException("Only Key types are allowed for descendants id");
			}
			for (Key ancestor : ancestors) {
				keyFactory.addAncestor(DatastoreTemplate.keyToPathElement(ancestor));
			}
		}
		Key allocatedKey = this.datastore.allocateId(keyFactory.newKey());

		Object value;
		if (idPropType.equals(Key.class)) {
			value = allocatedKey;
		}
		else if (idPropType.equals(Long.class)) {
			value = allocatedKey.getId();
		}
		else {
			value = allocatedKey.getId().toString();
		}

		datastorePersistentEntity.getPropertyAccessor(entity).setProperty(idProp, value);
		return allocatedKey;
	}

	private KeyFactory getKeyFactory() {
		return this.datastore.newKeyFactory();
	}
}
