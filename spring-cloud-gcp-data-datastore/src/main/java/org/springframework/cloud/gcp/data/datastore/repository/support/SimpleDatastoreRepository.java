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

import com.google.cloud.datastore.Datastore;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.repository.DatastoreRepository;
import org.springframework.data.domain.Page;
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
public class SimpleDatastoreRepository<T,ID> implements DatastoreRepository<T, ID>{

  private final DatastoreTemplate datastoreTemplate;

  public SimpleDatastoreRepository(DatastoreTemplate datastoreTemplate){
    Assert.notNull(datastoreTemplate, "A non-null DatastoreTemplate is required.");
    this.datastoreTemplate = datastoreTemplate;
  }

  @Override
  public <A> A performTransaction(Function<DatastoreRepository<T, ID>, A> operations) {
    return null;
  }

  @Override
  public Iterable<T> findAll(Sort sort) {
    throw new DatastoreDataException("Sorting findAll is not yet supported.");
  }

  @Override
  public Page<T> findAll(Pageable pageable) {
    throw new DatastoreDataException("Pageable findAll is not yet supported.");
  }

  @Override
  public <S extends T> S save(S entity) {
    return this.datastoreTemplate.save(entity);
  }

  @Override
  public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
    return null;
  }

  @Override
  public Optional<T> findById(ID id) {
    return null;
  }

  @Override
  public boolean existsById(ID id) {
    return false;
  }

  @Override
  public Iterable<T> findAll() {
    return null;
  }

  @Override
  public Iterable<T> findAllById(Iterable<ID> ids) {
    return null;
  }

  @Override
  public long count() {
    return 0;
  }

  @Override
  public void deleteById(ID id) {

  }

  @Override
  public void delete(T entity) {

  }

  @Override
  public void deleteAll(Iterable<? extends T> entities) {

  }

  @Override
  public void deleteAll() {

  }
}
