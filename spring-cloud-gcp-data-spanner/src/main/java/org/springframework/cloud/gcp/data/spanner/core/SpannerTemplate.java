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
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.util.Assert;

/**
 * @author Ray Tsang
 * @author Chengyuan Zhao
 */
public class SpannerTemplate implements SpannerOperations {

	private final DatabaseClient databaseClient;

	private final SpannerMappingContext mappingContext;

	private final SpannerConverter spannerConverter;

	private final SpannerMutationFactory mutationFactory;

	public SpannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext, SpannerConverter spannerConverter,
			SpannerMutationFactory spannerMutationFactory) {
		Assert.notNull(databaseClient,
				"A valid database client for Spanner is required.");
		Assert.notNull(mappingContext,
				"A valid mapping context for Spanner is required.");
		Assert.notNull(spannerConverter,
				"A valid results mapper for Spanner is required.");
		Assert.notNull(spannerMutationFactory,
				"A valid Spanner mutation factory is required.");
		this.databaseClient = databaseClient;
		this.mappingContext = mappingContext;
		this.spannerConverter = spannerConverter;
		this.mutationFactory = spannerMutationFactory;
	}

	private ReadContext getReadContext() {
		return this.databaseClient.singleUse();
	}

	public SpannerMappingContext getMappingContext() {
		return this.mappingContext;
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
		return this.spannerConverter.read(entityClass, row);
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, KeySet keys,
			Options.ReadOption... options) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		ResultSet resultSet = getReadContext().read(persistentEntity.tableName(), keys,
				persistentEntity.columns(), options);
		return this.spannerConverter.mapToList(resultSet, entityClass);
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, Statement statement,
			Options.QueryOption... options) {
		ResultSet resultSet = getReadContext().executeQuery(statement, options);
		return this.spannerConverter.mapToList(resultSet, entityClass);
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, String statement,
			QueryOption... options) {
		return find(entityClass, Statement.of(statement), options);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass, Options.ReadOption... options) {
		return this.find(entityClass, KeySet.all(), options);
	}

	@Override
	public void insert(Object object) {
		applyMutationUsingEntity(this.mutationFactory::insert, object);
	}

	@Override
	public void update(Object object) {
		applyMutationUsingEntity(this.mutationFactory::update, object);
	}

	@Override
	public void upsert(Object object) {
		applyMutationUsingEntity(this.mutationFactory::upsert, object);
	}

	@Override
	public void delete(Object entity) {
		applyMutationUsingEntity(this.mutationFactory::delete, entity);
	}

	@Override
	public void delete(Class entityClass, Key key) {
		applyMutationWithClass(this.mutationFactory::delete, entityClass, key);
	}

	@Override
	public <T> void delete(Class<T> entityClass, Iterable<? extends T> entities) {
		applyMutationWithClass(this.mutationFactory::delete, entityClass, entities);
	}

	@Override
	public void delete(Class entityClass, KeySet keys) {
		applyMutationWithClass(this.mutationFactory::delete, entityClass, keys);
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

	private <T, U> void applyMutationWithClass(BiFunction<T, U, Mutation> function,
			T arg1,
			U arg2) {
		this.databaseClient.write(Arrays.asList(function.apply(arg1, arg2)));
	}

	private <T> void applyMutationUsingEntity(Function<T, Mutation> function, T arg) {
		applyMutationWithClass((T t, Object unused) -> function.apply(t), arg, null);
	}
}
