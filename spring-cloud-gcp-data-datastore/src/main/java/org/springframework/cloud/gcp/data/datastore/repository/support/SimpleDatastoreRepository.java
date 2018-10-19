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

package org.springframework.cloud.gcp.data.datastore.repository.support;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Implementation of {@link DatastoreRepository}.
 *
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
		return this.datastoreTemplate.performTransaction(template -> operations
				.apply(new SimpleDatastoreRepository<>(template, this.entityType)));
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		Assert.notNull(sort, "A non-null Sort is required.");
		return this.datastoreTemplate
				.findAll(this.entityType, new DatastoreQueryOptions(null, null, sort));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Assert.notNull(pageable, "A non-null Pageable is required.");
		return new PageImpl<>(
				new ArrayList<>(this.datastoreTemplate
						.findAll(this.entityType,
								new DatastoreQueryOptions(pageable.getPageSize(), (int) pageable.getOffset(),
										pageable.getSort()))),
				pageable, this.datastoreTemplate.count(this.entityType));
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
}
