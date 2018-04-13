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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 */
public class SimpleSpannerRepository implements SpannerRepository {

	private final SpannerOperations spannerOperations;

	private final Class entityType;

	public SimpleSpannerRepository(SpannerOperations spannerOperations, Class entityType) {
		Assert.notNull(spannerOperations,
				"A valid SpannerOperations object is required.");
		Assert.notNull(entityType, "A valid entity type is required.");
		this.spannerOperations = spannerOperations;
		this.entityType = entityType;
	}

	@Override
	public SpannerOperations getSpannerOperations() {
		return this.spannerOperations;
	}

	@Override
	public Object save(Object entity) {
		Assert.notNull(entity, "A non-null entity is required for saving.");
		this.spannerOperations.upsert(entity);
		return entity;
	}

	@Override
	public Iterable saveAll(Iterable entities) {
		Assert.notNull(entities, "A non-null list of entities is required for saving.");
		for (Object entity : entities) {
			save(entity);
		}
		return entities;
	}

	@Override
	public Optional findById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		return doIfKey(key, k -> Optional
				.ofNullable(this.spannerOperations.read(this.entityType, k)));
	}

	@Override
	public boolean existsById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		return findById(key).isPresent();
	}

	@Override
	public Iterable findAll() {
		return this.spannerOperations.readAll(this.entityType);
	}

	@Override
	public Iterable findAllById(Iterable iterable) {
		KeySet.Builder builder = KeySet.newBuilder();
		for (Object id : iterable) {
			doIfKey(id, k -> builder.addKey(k));
		}
		return this.spannerOperations.read(this.entityType, builder.build());
	}

	@Override
	public long count() {
		return this.spannerOperations.count(this.entityType);
	}

	@Override
	public void deleteById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		doIfKey(key, k -> {
			this.spannerOperations.delete(this.entityType, k);
			return null;
		});
	}

	@Override
	public void delete(Object entity) {
		Assert.notNull(entity, "A non-null entity is required.");
		this.spannerOperations.delete(entity);
	}

	@Override
	public void deleteAll(Iterable entities) {
		Assert.notNull(entities, "A non-null list of entities is required.");
		this.spannerOperations.delete(this.entityType, entities);
	}

	@Override
	public void deleteAll() {
		this.spannerOperations.delete(this.entityType, KeySet.all());
	}

	@Override
	public Iterable findAll(Sort sort) {
		return this.spannerOperations.queryAll(this.entityType, sort);
	}

	@Override
	public Page findAll(Pageable pageable) {
		return this.spannerOperations.queryAll(this.entityType, pageable);
	}

	private <T> T doIfKey(Object key, Function<Key, T> operation) {
		Key k;
		boolean isIterable = Iterable.class.isAssignableFrom(key.getClass());
		boolean isArray = Object[].class.isAssignableFrom(key.getClass());
		if (isIterable || isArray) {
			Key.Builder kb = Key.newBuilder();
			for (Object keyPart : (isArray ? (Arrays.asList((Object[]) key))
					: ((Iterable) key))) {
				kb.appendObject(keyPart);
			}
			k = kb.build();
			if (k.size() == 0) {
				throw new SpannerDataException(
						"A key must have at least one component, but 0 were given.");
			}
		}
		else {
			k = key instanceof Key ? (Key) key : Key.of(key);
		}
		return operation.apply(k);
	}
}
