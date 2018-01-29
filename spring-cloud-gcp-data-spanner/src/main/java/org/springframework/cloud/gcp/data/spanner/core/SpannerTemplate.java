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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.Arrays;
import java.util.List;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;

import org.springframework.beans.BeanUtils;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerObjectMapper;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.util.Assert;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerTemplate implements SpannerOperations {

	private final DatabaseClient databaseClient;

	private final SpannerMappingContext mappingContext;

	private final SpannerObjectMapper objectMapper;

	private final SpannerMutationFactory mutationFactory;

	public SpannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext, SpannerObjectMapper spannerObjectMapper,
			SpannerMutationFactory spannerMutationFactory) {
		Assert.notNull(databaseClient,
				"A valid database client for Spanner is required.");
		Assert.notNull(mappingContext,
				"A valid mapping context for Spanner is required.");
		Assert.notNull(spannerObjectMapper,
				"A valid results mapper for Spanner is required.");
		Assert.notNull(spannerMutationFactory,
				"A valid Spanner mutation factory is required.");
		this.databaseClient = databaseClient;
		this.mappingContext = mappingContext;
		this.objectMapper = spannerObjectMapper;
		this.mutationFactory = spannerMutationFactory;
	}

	private ReadContext getReadContext() {
		return this.databaseClient.singleUse();
	}

	@Override
	public SpannerMappingContext getMappingContext() {
		return this.mappingContext;
	}

	@Override
	public DatabaseClient getDatabaseClient() {
		return this.databaseClient;
	}

	@Override
	public <T> T find(Class<T> entityClass, Key key) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		Struct row = getReadContext().readRow(persistentEntity.tableName(), key,
				persistentEntity.columns());
		if (row == null) {
			return null;
		}
		T object = BeanUtils.instantiateClass(entityClass);
		this.objectMapper.map(row, object);
		return object;
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, KeySet keys,
			Options.ReadOption... options) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		ResultSet resultSet = getReadContext().read(persistentEntity.tableName(), keys,
				persistentEntity.columns(), options);
		return this.objectMapper.mapToUnmodifiableList(resultSet, entityClass);
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, Statement statement,
			Options.QueryOption... options) {
		ResultSet resultSet = getReadContext().executeQuery(statement, options);
		return this.objectMapper.mapToUnmodifiableList(resultSet, entityClass);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass, Options.ReadOption... options) {
		return this.find(entityClass, KeySet.all(), options);
	}

	@Override
	public void insert(Object object) {
		Mutation mutation = this.mutationFactory.insert(object);
		this.databaseClient.write(Arrays.asList(mutation));
	}

	@Override
	public void update(Object object) {
		Mutation mutation = this.mutationFactory.update(object);
		this.databaseClient.write(Arrays.asList(mutation));
	}

	@Override
	public void upsert(Object object) {
		Mutation mutation = this.mutationFactory.upsert(object);
		this.databaseClient.write(Arrays.asList(mutation));
	}

	@Override
	public void delete(Class entityClass, Key key) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		String tableName = persistentEntity.tableName();
		Mutation mutation = Mutation.delete(tableName, key);
		this.databaseClient.write(Arrays.asList(mutation));
	}

	@Override
	public void delete(Object entity) {
		Mutation mutation = this.mutationFactory.delete(entity);
		this.databaseClient.write(Arrays.asList(mutation));
	}

	@Override
	public <T> void delete(Class<T> entityClass, Iterable<? extends T> entities) {
		Mutation mutation = this.mutationFactory.delete(entityClass, entities);
		this.databaseClient.write(Arrays.asList(mutation));
	}

	@Override
	public void delete(Class entityClass, KeySet keys) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		String tableName = persistentEntity.tableName();
		Mutation delete = Mutation.delete(tableName, keys);
		this.databaseClient.write(Arrays.asList(delete));
	}

	@Override
	public long count(Class entityClass) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		ResultSet resultSet = this.databaseClient.singleUse().executeQuery(Statement.of(
				String.format("select count(*) from %s", persistentEntity.tableName())));
		resultSet.next();
		return resultSet.getLong(0);
	}
}
