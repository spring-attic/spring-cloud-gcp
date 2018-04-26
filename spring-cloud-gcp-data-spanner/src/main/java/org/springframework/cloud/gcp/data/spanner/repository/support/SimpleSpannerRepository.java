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
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 */
public class SimpleSpannerRepository<T, ID> implements SpannerRepository<T, ID> {

	private final SpannerTemplate spannerTemplate;

	private final Class entityType;

	public SimpleSpannerRepository(SpannerTemplate spannerTemplate, Class entityType) {
		Assert.notNull(spannerTemplate, "A valid SpannerTemplate object is required.");
		Assert.notNull(entityType, "A valid entity type is required.");
		this.spannerTemplate = spannerTemplate;
		this.entityType = entityType;
	}

	@Override
	public SpannerOperations getSpannerTemplate() {
		return this.spannerTemplate;
	}

	@Override
	public <A> A performReadOnlyTransaction(
			Function<SpannerRepository<T, ID>, A> operations) {
		return this.spannerTemplate
				.performReadOnlyTransaction(
						transactionSpannerOperations -> operations
								.apply(new SimpleSpannerRepository<T, ID>(
										transactionSpannerOperations, this.entityType)),
						null);
	}

	@Override
	public <A> A performReadWriteTransaction(
			Function<SpannerRepository<T, ID>, A> operations) {
		return this.spannerTemplate
				.performReadWriteTransaction(transactionSpannerOperations -> operations
						.apply(new SimpleSpannerRepository<T, ID>(transactionSpannerOperations,
								this.entityType)));
	}

	@Override
	public Object save(Object entity) {
		Assert.notNull(entity, "A non-null entity is required for saving.");
		this.spannerTemplate.upsert(entity);
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
		return doIfKey(key, k -> {
			Object result = this.spannerTemplate.read(this.entityType, k);
			return Optional.ofNullable(result);
		});
	}

	@Override
	public boolean existsById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		return findById(key).isPresent();
	}

	@Override
	public Iterable findAll() {
		return this.spannerTemplate.readAll(this.entityType);
	}

	@Override
	public Iterable findAllById(Iterable iterable) {
		KeySet.Builder builder = KeySet.newBuilder();
		for (Object id : iterable) {
			doIfKey(id, k -> builder.addKey(k));
		}
		return this.spannerTemplate.read(this.entityType, builder.build());
	}

	@Override
	public long count() {
		return this.spannerTemplate.count(this.entityType);
	}

	@Override
	public void deleteById(Object key) {
		Assert.notNull(key, "A non-null ID is required.");
		doIfKey(key, k -> {
			this.spannerTemplate.delete(this.entityType, k);
			return null;
		});
	}

	@Override
	public void delete(Object entity) {
		Assert.notNull(entity, "A non-null entity is required.");
		this.spannerTemplate.delete(entity);
	}

	@Override
	public void deleteAll(Iterable entities) {
		Assert.notNull(entities, "A non-null list of entities is required.");
		this.spannerTemplate.delete(this.entityType, entities);
	}

	@Override
	public void deleteAll() {
		this.spannerTemplate.delete(this.entityType, KeySet.all());
	}

	@Override
	public Iterable findAll(Sort sort) {
		return this.spannerTemplate.queryAll(this.entityType,
				new SpannerQueryOptions().setSort(sort));
	}

	@Override
	public Page findAll(Pageable pageable) {
		return new PageImpl<>(this.spannerTemplate.queryAll(this.entityType,
				new SpannerQueryOptions().setLimit(pageable.getPageSize())
						.setOffset(pageable.getOffset()).setSort(pageable.getSort())),
				pageable, this.spannerTemplate.count(this.entityType));
	}

	private <T> T doIfKey(Object key, Function<Key, T> operation) {
		Key k = this.spannerTemplate.getSpannerEntityProcessor().writeToKey(key);
		return operation.apply(k);
	}

}
