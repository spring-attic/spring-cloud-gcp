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
import org.springframework.cloud.gcp.data.spanner.core.SpannerPageableQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.repository.SpannerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SimpleSpannerRepository<T, ID> implements SpannerRepository<T, ID> {

	private final SpannerTemplate spannerTemplate;

	private final Class<T> entityType;

	public SimpleSpannerRepository(SpannerTemplate spannerTemplate, Class<T> entityType) {
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
	public <S extends T> S save(S entity) {
		Assert.notNull(entity, "A non-null entity is required for saving.");
		this.spannerTemplate.upsert(entity);
		return entity;
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		Assert.notNull(entities, "A non-null list of entities is required for saving.");
		this.spannerTemplate.upsertAll(entities);
		return entities;
	}

	@Override
	public Optional<T> findById(ID key) {
		Assert.notNull(key, "A non-null ID is required.");
		return doIfKey(key, k -> {
			T result = this.spannerTemplate.read(this.entityType, k);
			return Optional.<T>ofNullable(result);
		});
	}

	@Override
	public boolean existsById(ID key) {
		Assert.notNull(key, "A non-null ID is required.");
		return findById(key).isPresent();
	}

	@Override
	public Iterable<T> findAll() {
		return this.spannerTemplate.readAll(this.entityType);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> iterable) {
		KeySet.Builder builder = KeySet.newBuilder();
		for (Object id : iterable) {
			doIfKey(id, builder::addKey);
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
	public void deleteAll(Iterable<? extends T> entities) {
		Assert.notNull(entities, "A non-null list of entities is required.");
		this.spannerTemplate.deleteAll(entities);
	}

	@Override
	public void deleteAll() {
		this.spannerTemplate.delete(this.entityType, KeySet.all());
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		return this.spannerTemplate.queryAll(this.entityType,
				new SpannerPageableQueryOptions().setSort(sort));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		return new PageImpl<>(this.spannerTemplate.queryAll(this.entityType,
				new SpannerPageableQueryOptions().setLimit(pageable.getPageSize())
						.setOffset(pageable.getOffset()).setSort(pageable.getSort())),
				pageable, this.spannerTemplate.count(this.entityType));
	}

	private <A> A doIfKey(Object key, Function<Key, A> operation) {
		Key k = this.spannerTemplate.getSpannerEntityProcessor().writeToKey(key);
		return operation.apply(k);
	}

}
