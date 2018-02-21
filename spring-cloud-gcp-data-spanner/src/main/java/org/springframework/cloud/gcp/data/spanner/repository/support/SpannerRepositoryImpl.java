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

import java.util.Optional;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 */
public class SpannerRepositoryImpl implements SpannerRepository {

	private final SpannerOperations spannerOperations;

	private final Class entityType;

	public SpannerRepositoryImpl(SpannerOperations spannerOperations, Class entityType) {
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
	public Optional findById(Object o) {
		Assert.notNull(o, "A non-null ID is required.");
		return Optional.ofNullable(this.spannerOperations.find(this.entityType, Key.of(o)));
	}

	@Override
	public boolean existsById(Object o) {
		Assert.notNull(o, "A non-null ID is required.");
		return findById(o).isPresent();
	}

	@Override
	public Iterable findAll() {
		return this.spannerOperations.findAll(this.entityType);
	}

	@Override
	public Iterable findAllById(Iterable iterable) {
		KeySet.Builder builder = KeySet.newBuilder();
		for (Object id : iterable) {
			builder.addKey(Key.of(id));
		}
		return this.spannerOperations.find(this.entityType, builder.build());
	}

	@Override
	public long count() {
		return this.spannerOperations.count(this.entityType);
	}

	@Override
	public void deleteById(Object o) {
		Assert.notNull(o, "A non-null ID is required.");
		this.spannerOperations.delete(this.entityType, Key.of(o));
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
}
