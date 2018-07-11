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

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.util.Pair;

/**
 * Utility class for common operations.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreDataUtils {

	/**
	 * Constructs a Datastore key from an entity object and its metadata.
	 * @param entity the object
	 * @param keyFactory a key factory that will be populated with ancestor data and key
	 * values.
	 * @param datastorePersistentEntity a persistent entity with the metadata of the
	 * entity object.
	 * @param keyAllocFunc a function to provide a new key value if the object does not
	 * have one already.
	 * @return a Datastore key.
	 */
	public static Key getKey(Object entity, KeyFactory keyFactory,
			DatastorePersistentEntity datastorePersistentEntity,
			Function<IncompleteKey, Key> keyAllocFunc) {
		PersistentProperty idProp = datastorePersistentEntity.getIdProperty();
		if (idProp == null) {
			throw new DatastoreDataException(
					"Cannot construct key for entity types without Id properties: "
							+ entity.getClass());
		}
		PersistentPropertyAccessor accessor = datastorePersistentEntity
				.getPropertyAccessor(entity);
		DatastorePersistentProperty ancestorProp = datastorePersistentEntity
				.ancestorProperty();
		if (ancestorProp != null) {
			List<Pair<String, Object>> ancestors = (List<Pair<String, Object>>) accessor
					.getProperty(ancestorProp);
			if (ancestors != null) {
				ancestors.forEach(pair -> {
					Object val = pair.getSecond();
					PathElement pathElement = useKeyComponent(val,
							() -> PathElement.of(pair.getFirst(), (Long) val),
							() -> PathElement.of(pair.getFirst(), (String) val));
					keyFactory.addAncestor(pathElement);
				});
			}
		}
		keyFactory.setKind(datastorePersistentEntity.kindName());
		Object idVal = accessor.getProperty(datastorePersistentEntity.getIdProperty());
		Key key;
		if (idVal == null) {
			key = keyAllocFunc.apply(keyFactory.newKey());
			accessor.setProperty(idProp, key.getNameOrId());
		}
		else {
			key = useKeyComponent(idVal, () -> keyFactory.newKey((Long) idVal),
					() -> keyFactory.newKey((String) idVal));
		}
		return key;
	}

	// Cloud Datastore keys can be constructed only from String or Long components. This is a
	// helper function that checks key component objects for these two types.
	private static <T> T useKeyComponent(Object val, Supplier<T> longFunc,
			Supplier<T> stringFunc) {
		if (val instanceof Long || val instanceof Integer
				|| val.getClass().equals(long.class)
				|| val.getClass().equals(int.class)) {
			return longFunc.get();
		}
		else if (val instanceof String) {
			return stringFunc.get();
		}
		else {
			throw new DatastoreDataException(
					"Only Long and String are allowed in Datastore keys: " + val);
		}
	}

}
