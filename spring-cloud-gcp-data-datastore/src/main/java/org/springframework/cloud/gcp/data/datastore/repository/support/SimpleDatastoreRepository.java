/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.repository.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.Key;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreResultsCollection;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreResultsIterable;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.cloud.gcp.data.datastore.repository.query.DatastorePageable;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Implementation of {@link DatastoreRepository}.
 * @param <T> the type of the entities
 * @param <ID> the id type of the entities
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SimpleDatastoreRepository<T, ID> implements DatastoreRepository<T, ID> {

	private final DatastoreOperations datastoreTemplate;

	private final Class<T> entityType;

	public SimpleDatastoreRepository(DatastoreOperations datastoreTemplate,
			Class<T> entityType) {
		Assert.notNull(datastoreTemplate, "A non-null DatastoreOperations is required.");
		Assert.notNull(entityType, "A non-null entity type is required.");
		this.datastoreTemplate = datastoreTemplate;
		this.entityType = entityType;
	}

	@Override
	public <A> A performTransaction(Function<DatastoreRepository<T, ID>, A> operations) {
		return this.datastoreTemplate.performTransaction((template) -> operations
				.apply(new SimpleDatastoreRepository<>(template, this.entityType)));
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		Assert.notNull(sort, "A non-null Sort is required.");
		return this.datastoreTemplate
				.findAll(this.entityType, new DatastoreQueryOptions.Builder().setSort(sort).build());
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Assert.notNull(pageable, "A non-null Pageable is required.");
		Collection<T> entities = this.datastoreTemplate
				.findAll(this.entityType,
						new DatastoreQueryOptions.Builder().setLimit(pageable.getPageSize())
								.setOffset((int) pageable.getOffset()).setSort(pageable.getSort())
								.setCursor(getCursor(pageable)).build());

		Long totalCount = getOrComputeTotalCount(pageable, () -> this.datastoreTemplate.count(this.entityType));
		Pageable cursorPageable = DatastorePageable.from(pageable,
				entities instanceof DatastoreResultsCollection ? ((DatastoreResultsCollection) entities).getCursor()
						: null,
				totalCount);

		return new PageImpl<>(entities != null ? new ArrayList<>(entities) : Collections.emptyList(), cursorPageable,
				totalCount);
	}

	@Override
	public <S extends T> S save(S entity) {
		return this.datastoreTemplate.save(entity);
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		return this.datastoreTemplate.saveAll(entities);
	}

	@Override
	public Optional<T> findById(ID id) {
		return Optional.ofNullable(this.datastoreTemplate.findById(id, this.entityType));
	}

	@Override
	public boolean existsById(ID id) {
		return this.datastoreTemplate.existsById(id, this.entityType);
	}

	@Override
	public Iterable<T> findAll() {
		return this.datastoreTemplate.findAll(this.entityType);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {
		return this.datastoreTemplate.findAllById(ids, this.entityType);
	}

	@Override
	public long count() {
		return this.datastoreTemplate.count(this.entityType);
	}

	@Override
	public void deleteById(ID id) {
		this.datastoreTemplate.deleteById(id, this.entityType);
	}

	@Override
	public void delete(T entity) {
		this.datastoreTemplate.delete(entity);
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		this.datastoreTemplate.deleteAll(entities);
	}

	@Override
	public void deleteAll() {
		this.datastoreTemplate.deleteAll(this.entityType);
	}

	@Override
	public <S extends T> Optional<S> findOne(Example<S> example) {
		Iterable<S> entities = this.datastoreTemplate.queryByExample(example,
				new DatastoreQueryOptions.Builder().setLimit(1).build());
		Iterator<S> iterator = entities.iterator();
		return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example) {
		return this.datastoreTemplate.queryByExample(example, null);
	}

	@Override
	public <S extends T> Iterable<S> findAll(Example<S> example, Sort sort) {
		return this.datastoreTemplate.queryByExample(example,
				new DatastoreQueryOptions.Builder().setSort(sort).build());
	}

	@Override
	public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
		Assert.notNull(pageable, "A non-null pageable is required.");

		Iterable<S> entities = this.datastoreTemplate.queryByExample(example,
				new DatastoreQueryOptions.Builder().setLimit(pageable.getPageSize())
						.setOffset((int) pageable.getOffset()).setSort(pageable.getSort())
						.setCursor(getCursor(pageable)).build());
		List<S> result = StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());

		Long totalCount = getOrComputeTotalCount(pageable, () -> count(example));
		Pageable cursorPageable = DatastorePageable.from(pageable,
				entities instanceof DatastoreResultsIterable ? ((DatastoreResultsIterable) entities).getCursor() : null,
				totalCount);

		return new PageImpl<>(result, cursorPageable, totalCount);
	}

	@Override
	public <S extends T> long count(Example<S> example) {
		Iterable<Key> keys = this.datastoreTemplate.keyQueryByExample(example, null);

		return StreamSupport.stream(keys.spliterator(), false).count();
	}

	@Override
	public <S extends T> boolean exists(Example<S> example) {
		Iterable<Key> keys = this.datastoreTemplate.keyQueryByExample(example,
				new DatastoreQueryOptions.Builder().setLimit(1).build());
		return StreamSupport.stream(keys.spliterator(), false).findAny().isPresent();
	}

	private static Cursor getCursor(Pageable pageable) {
		return pageable instanceof DatastorePageable ?  ((DatastorePageable) pageable).getCursor() : null;
	}

	private static Long getOrComputeTotalCount(Pageable pageable, Supplier<Long> countCall) {
		return pageable instanceof DatastorePageable ? ((DatastorePageable) pageable).getTotalCount() : countCall.get();
	}
}
