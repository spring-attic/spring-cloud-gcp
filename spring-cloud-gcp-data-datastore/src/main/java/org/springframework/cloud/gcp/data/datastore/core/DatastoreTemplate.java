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
import java.util.Iterator;
import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Entity.Builder;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.common.collect.ImmutableList;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreDataUtils;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.lang.Nullable;
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

	@Override
	public <T> T read(Class<T> entityClass, Key key,
			@Nullable DatastoreReadOptions readOption) {
		List<T> results = read(entityClass, ImmutableList.of(key), readOption);
		return results == null || results.isEmpty() ? null : results.get(0);
	}

	@Override
	public <T> List<T> read(Class<T> entityClass, Iterable<Key> keys,
			@Nullable DatastoreReadOptions readOption) {
		Iterator<Entity> entities;
		if (readOption == null || !readOption.hasReadOptions()) {
			entities = this.datastore.get(keys);
		}
		else {
			entities = this.datastore.get(keys, readOption.getReadOptions());
		}
		return convertEntities(entityClass, entities);
	}

	private <T> List<T> convertEntities(Class<T> entityClass, Iterator<Entity> entities) {
		List<T> results = new ArrayList<>();
		entities.forEachRemaining(entity -> results
				.add(this.datastoreEntityConverter.read(entityClass, entity)));
		return results;
	}

	@Override
	public <T> List<T> readAll(Class<T> entityClass,
			@Nullable DatastoreReadOptions readOption) {
		return query(entityClass,
				Query.newEntityQueryBuilder()
						.setKind(this.datastoreMappingContext
								.getPersistentEntity(entityClass).kindName())
						.build(),
				readOption);
	}

	@Override
	public <T> List<T> query(Class<T> entityClass, Query<Entity> query,
			@Nullable DatastoreReadOptions readOption) {
		QueryResults<Entity> entities;
		if (readOption == null || !readOption.hasReadOptions()) {
			entities = this.datastore.run(query);
		}
		else {
			entities = this.datastore.run(query, readOption.getReadOptions());
		}
		return convertEntities(entityClass, entities);
	}

	@Override
	public void delete(Key key) {
		this.datastore.delete(key);
	}

	@Override
	public void delete(Key... keys) {
		this.datastore.delete(keys);
	}

	@Override
	public void delete(Object entity) {
		this.datastore.delete(getKey(entity));
	}

	private Key getKey(Object entity) {
		return DatastoreDataUtils.getKey(entity, this.datastore.newKeyFactory(),
				this.datastoreMappingContext.getPersistentEntity(entity.getClass()),
				x -> this.datastore.allocateId(x));
	}

	@Override
	public void update(Object entity) {
		this.datastore.update(convertToEntity(entity));
	}

	private Entity convertToEntity(Object entity) {
		Builder builder = Entity.newBuilder(getKey(entity));
		this.datastoreEntityConverter.write(entity, builder);
		return builder.build();
	}

	@Override
	public void put(Object entity) {
		this.datastore.put(convertToEntity(entity));
	}

	@Override
	public long count(Class entityClass) {
		return readAll(entityClass, null).size();
	}
}
