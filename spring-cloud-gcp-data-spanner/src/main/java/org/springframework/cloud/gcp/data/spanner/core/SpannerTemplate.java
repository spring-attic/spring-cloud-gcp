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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.common.annotations.VisibleForTesting;

import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerLazyList;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.cloud.gcp.data.spanner.repository.query.SpannerStatementQueryExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
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

	protected ReadContext getReadContext() {
		return this.databaseClient.singleUse();
	}

	protected ReadContext getReadContext(Timestamp timestamp) {
		return this.databaseClient.singleUse(TimestampBound.ofReadTimestamp(timestamp));
	}

	public SpannerMappingContext getMappingContext() {
		return this.mappingContext;
	}

	@Override
	public <T> T find(Class<T> entityClass, Key key) {
		return find(entityClass, key, null);
	}

	@Override
	public <T> T find(Class<T> entityClass, Key key, SpannerReadOptions options) {
		List<T> items = find(entityClass, KeySet.singleKey(key), options);
		return items.isEmpty() ? null : items.get(0);
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, KeySet keys) {
		return find(entityClass, keys, null);
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, KeySet keys,
			SpannerReadOptions options) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		return resolveChildEntities(
				this.spannerConverter.mapToList(executeRead(persistentEntity.tableName(),
						keys, persistentEntity.columns(), options), entityClass),
				options != null && options.hasTimestamp() ? options.getTimestamp()
						: null);
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, Statement statement,
			SpannerQueryOptions options) {
		Timestamp timestamp = options != null && options.hasTimestamp()
				? options.getTimestamp()
				: null;
		return resolveChildEntities(this.spannerConverter
				.mapToList(executeQuery(statement, options), entityClass), timestamp);
	}

	private List resolveChildEntities(List entities, Timestamp timestamp) {
		for (Object entity : entities) {
			resolveChildEntity(entity, timestamp);
		}
		return entities;
	}

	@VisibleForTesting
	void resolveChildEntity(Object entity, Timestamp timestamp) {
		SpannerPersistentEntity spannerPersistentEntity = this.mappingContext
				.getPersistentEntity(entity.getClass());

		PersistentPropertyAccessor accessor = spannerPersistentEntity
				.getPropertyAccessor(entity);

		spannerPersistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> ConversionUtils
						.applyIfChildEntityType(spannerPersistentProperty, childType -> {

							BiFunction<Class, String, List> getChildRows = (propType,
									childTable) -> find(propType,
											SpannerStatementQueryExecutor
													.getChildrenRowsQuery(
															spannerPersistentEntity,
															entity, childTable),
											timestamp == null ? null
													: new SpannerQueryOptions()
															.setTimestamp(timestamp));

							/*
							 * If the property is a single item then we retrieve it
							 * immediately. However, to prevent an exploding number of
							 * retrievals, List-type child properties are retrieved
							 * lazily.
							 */
							if (!ConversionUtils.isIterableNonByteArrayType(
									spannerPersistentProperty.getType())) {
								SpannerPersistentEntity childPersistentEntity = this.mappingContext
										.getPersistentEntity(childType);
								accessor.setProperty(spannerPersistentProperty,
										getChildRows
												.apply(childType,
														childPersistentEntity.tableName())
												.get(0));
							}
							else {
								SpannerPersistentEntity childPersistentEntity = this.mappingContext
										.getPersistentEntity(childType);
								accessor.setProperty(spannerPersistentProperty,
										new SpannerLazyList<>(() -> getChildRows.apply(
												childType,
												childPersistentEntity.tableName())));
							}
							return null;
						}));
	}

	@Override
	public <T> List<T> find(Class<T> entityClass, Statement statement) {
		return find(entityClass, statement, null);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass, SpannerReadOptions options) {
		return find(entityClass, KeySet.all(), options);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass) {
		return findAll(entityClass, (SpannerReadOptions) null);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass, Sort sort) {
		return findAll(entityClass, sort, null);
	}

	@Override
	public <T> List<T> findAll(Class<T> entityClass, Sort sort,
			SpannerQueryOptions options) {
		Assert.notNull(sort, "sort must not be null!");

		StringBuilder stringBuilder = new StringBuilder();
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		stringBuilder.append("SELECT * FROM " + persistentEntity.tableName() + " ");
		SpannerStatementQueryExecutor.buildOrderBy(persistentEntity, stringBuilder, sort);
		if (options != null) {
			if (options.hasLimit()) {
				stringBuilder.append(" LIMIT " + options.getLimit());
			}
			if (options.hasLimit()) {
				stringBuilder.append(" OFFSET " + options.getOffset());
			}
		}
		stringBuilder.append(";");
		return find(entityClass, Statement.of(stringBuilder.toString()), options);
	}

	@Override
	public <T> Page<T> findAll(Class<T> entityClass, Pageable pageable,
			SpannerQueryOptions options) {
		Assert.notNull(pageable, "Pageable must not be null!");

		Long count = count(entityClass);
		List<T> list = findAll(entityClass, pageable.getSort(), options);
		return new PageImpl(list, pageable, count);
	}

	@Override
	public <T> Page<T> findAll(Class<T> entityClass, Pageable pageable) {
		return findAll(entityClass, pageable, null);
	}

	@Override
	public void insert(Object object) {
		applyMutationUsingEntity(this.mutationFactory::insert, object);
	}

	@Override
	public void update(Object object) {
		applyMutationTwoArgs(this.mutationFactory::update, object, null);
	}

	@Override
	public void update(Object object, String... includeColumns) {
		applyMutationTwoArgs(this.mutationFactory::update, object,
				includeColumns.length == 0 ? null
						: Optional.of(new HashSet<>(Arrays.asList(includeColumns))));
	}

	@Override
	public void update(Object object, Optional<Set<String>> includeColumns) {
		applyMutationTwoArgs(this.mutationFactory::update, object, includeColumns);
	}

	@Override
	public void upsert(Object object) {
		applyMutationTwoArgs(this.mutationFactory::upsert, object, null);
	}

	@Override
	public void upsert(Object object, String... includeColumns) {
		applyMutationTwoArgs(this.mutationFactory::upsert, object,
				includeColumns.length == 0 ? null
						: Optional.of(new HashSet<>(Arrays.asList(includeColumns))));
	}

	@Override
	public void upsert(Object object, Optional<Set<String>> includeColumns) {
		applyMutationTwoArgs(this.mutationFactory::upsert, object, includeColumns);
	}

	@Override
	public void delete(Object entity) {
		applyMutationUsingEntity(x -> Arrays.asList(this.mutationFactory.delete(x)),
				entity);
	}

	@Override
	public void delete(Class entityClass, Key key) {
		applyMutationTwoArgs((x, y) -> Arrays.asList(this.mutationFactory.delete(x, y)),
				entityClass, key);
	}

	@Override
	public <T> void delete(Class<T> entityClass, Iterable<? extends T> entities) {
		applyMutationTwoArgs((x, y) -> Arrays.asList(this.mutationFactory.delete(x, y)),
				entityClass, entities);
	}

	@Override
	public void delete(Class entityClass, KeySet keys) {
		applyMutationTwoArgs((x, y) -> Arrays.asList(this.mutationFactory.delete(x, y)),
				entityClass, keys);
	}

	@Override
	public long count(Class entityClass) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		Statement statement = Statement.of(String.format(
				"select count(*) from %s", persistentEntity.tableName()));
		try (ResultSet resultSet = executeQuery(statement, null)) {
			resultSet.next();
			return resultSet.getLong(0);
		}
	}

	@Override
	public <T> T performReadWriteTransaction(Function<SpannerOperations, T> operations) {
		return this.databaseClient.readWriteTransaction()
				.run(new TransactionCallable<T>() {
					@Nullable
					@Override
					public T run(TransactionContext transaction) throws Exception {
						ReadWriteTransactionSpannerTemplate transactionSpannerTemplate =
								new ReadWriteTransactionSpannerTemplate(
								SpannerTemplate.this.databaseClient,
								SpannerTemplate.this.mappingContext,
								SpannerTemplate.this.spannerConverter,
								SpannerTemplate.this.mutationFactory, transaction);
						return operations.apply(transactionSpannerTemplate);
					}
				});
	}

	@Override
	public <T> T performReadOnlyTransaction(Function<SpannerOperations, T> operations,
			SpannerReadOptions readOptions) {
		try (ReadOnlyTransaction readOnlyTransaction = readOptions.hasTimestamp()
				? this.databaseClient.readOnlyTransaction(
						TimestampBound.ofReadTimestamp(readOptions.getTimestamp()))
				: this.databaseClient.readOnlyTransaction()) {
			return operations.apply(new ReadOnlyTransactionSpannerTemplate(
					SpannerTemplate.this.databaseClient,
					SpannerTemplate.this.mappingContext,
					SpannerTemplate.this.spannerConverter,
					SpannerTemplate.this.mutationFactory, readOnlyTransaction));
		}
	}

	private ResultSet executeRead(String tableName, KeySet keys, Iterable<String> columns,
			SpannerReadOptions options) {
		if (options == null) {
			return getReadContext().read(tableName, keys, columns);
		}
		else {
			ReadContext readContext = (options.hasTimestamp()
					? getReadContext(options.getTimestamp())
					: getReadContext());
			if (options.hasIndex()) {
				return readContext.readUsingIndex(tableName, options.getIndex(), keys,
						columns, options.getReadOptions());
			}
			return readContext.read(tableName, keys, columns,
							options.getReadOptions());
		}
	}

	private ResultSet executeQuery(Statement statement, SpannerQueryOptions options) {
		if (options == null) {
			return getReadContext().executeQuery(statement);
		}
		else {
			return (options.hasTimestamp() ? getReadContext(options.getTimestamp())
					: getReadContext()).executeQuery(statement,
							options.getQueryOptions());
		}
	}

	protected <T, U> void applyMutationTwoArgs(BiFunction<T, U, List<Mutation>> function,
			T arg1,
			U arg2) {
		this.databaseClient.write(function.apply(arg1, arg2));
	}

	private <T> void applyMutationUsingEntity(Function<T, List<Mutation>> function,
			T arg) {
		applyMutationTwoArgs((T t, Object unused) -> function.apply(t), arg, null);
	}
}
