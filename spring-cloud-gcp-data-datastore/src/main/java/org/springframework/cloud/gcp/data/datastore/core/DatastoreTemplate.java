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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.util.Assert;

/**
 * An implementation of {@link DatastoreOperations}.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreTemplate implements DatastoreOperations {

	private final Datastore datastore;

	private final DatastoreEntityConverter datastoreEntityConverter;

	private final DatastoreMappingContext datastoreMappingContext;

	public DatastoreTemplate(Datastore datastore,
			DatastoreEntityConverter datastoreEntityConverter,
			DatastoreMappingContext datastoreMappingContext) {
		Assert.notNull(datastore, "A non-null Datastore service object is required.");
		Assert.notNull(datastoreEntityConverter,
				"A non-null DatastoreEntityConverter is required.");
		Assert.notNull(datastoreMappingContext,
				"A non-null DatastoreMappingContext is required.");
		this.datastore = datastore;
		this.datastoreEntityConverter = datastoreEntityConverter;
		this.datastoreMappingContext = datastoreMappingContext;
	}

	private Key getKeyFromId(Object id, Class entityClass) {
		Assert.notNull(id, "Cannot get key for null ID value.");
		if (id instanceof Key) {
			return (Key) id;
		}
		KeyFactory keyFactory = this.datastore.newKeyFactory();
		keyFactory.setKind(
				this.datastoreMappingContext.getPersistentEntity(entityClass).kindName());
		Key key;
		if (id instanceof String) {
			key = keyFactory.newKey((String) id);
		}
		else if (id == long.class) {
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
	public <T> T findById(Object id, Class<T> entityClass) {
		return this.datastoreEntityConverter.read(entityClass,
				this.datastore.get(getKeyFromId(id, entityClass)));
	}

	private Key getKey(Object entity) {
		DatastorePersistentEntity datastorePersistentEntity = this.datastoreMappingContext
				.getPersistentEntity(entity.getClass());
		PersistentProperty idProp = datastorePersistentEntity.getIdProperty();
		if (idProp == null) {
			throw new DatastoreDataException(
					"Cannot construct key for entity types without ID properties: "
							+ entity.getClass());
		}
		return getKeyFromId(
				datastorePersistentEntity.getPropertyAccessor(entity).getProperty(idProp),
				entity.getClass());
	}

	private Entity convertToEntity(Object entity) {
		Builder builder = Entity.newBuilder(getKey(entity));
		this.datastoreEntityConverter.write(entity, builder);
		return builder.build();
	}

	@Override
	public <T> void save(T instance) {
		this.datastore.put(convertToEntity(instance));
	}

	@Override
	public <T> void deleteById(Object id, Class<T> entityClass) {
		this.datastore.delete(getKeyFromId(id, entityClass));
	}

	@Override
	public <T> void delete(T entity) {
		this.datastore.delete(getKey(entity));
	}

	@Override
	public void deleteAll(Class<?> entityClass) {
		this.datastore.delete(findAll(entityClass).parallelStream().map(this::getKey)
				.toArray(Key[]::new));
	}

	@Override
	public long count(Class<?> entityClass) {
		return findAll(entityClass).size();
	}

	@Override
	public <T> Collection<T> findAllById(Iterable<?> ids, Class<T> entityClass) {
		List<Key> keysToFind = new ArrayList<>();
		ids.forEach(x -> keysToFind.add(getKeyFromId(x, entityClass)));
		return convertEntities(
				this.datastore.get(keysToFind.toArray(new Key[keysToFind.size()])),
				entityClass);
	}

	private <T> Collection<T> convertEntities(Iterator<Entity> entities,
			Class<T> entityClass) {
		List<T> results = new ArrayList<>();
		entities.forEachRemaining(entity -> results
				.add(this.datastoreEntityConverter.read(entityClass, entity)));
		return results;
	}

	@Override
	public <T> Collection<T> findAll(Class<T> entityClass) {
		return convertEntities(
				this.datastore.run(Query.newEntityQueryBuilder()
						.setKind(this.datastoreMappingContext
								.getPersistentEntity(entityClass).kindName())
						.build()),
				entityClass);
	}

	@Override
	public <T> boolean existsById(Object id, Class<T> entityClass) {
		return findById(id, entityClass) != null;
	}
}
