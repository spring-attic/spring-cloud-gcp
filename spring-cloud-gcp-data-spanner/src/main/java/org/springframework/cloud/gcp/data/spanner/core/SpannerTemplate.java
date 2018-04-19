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
import java.util.StringJoiner;
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

import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.repository.query.SpannerStatementQueryExecutor;
import org.springframework.data.domain.Sort;
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

	public SpannerConverter getSpannerConverter() {
		return this.spannerConverter;
	}

	@Override
	public <T> T read(Class<T> entityClass, Key key) {
		return read(entityClass, key, null);
	}

	@Override
	public <T> T read(Class<T> entityClass, Key key, SpannerReadOptions options) {
		List<T> items = read(entityClass, KeySet.singleKey(key), options);
		return items.isEmpty() ? null : items.get(0);
	}

	@Override
	public <T> List<T> read(Class<T> entityClass, KeySet keys) {
		return read(entityClass, keys, null);
	}

	@Override
	public <T> List<T> read(Class<T> entityClass, KeySet keys,
			SpannerReadOptions options) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		return this.spannerConverter.mapToList(executeRead(persistentEntity.tableName(),
				keys, persistentEntity.columns(), options), entityClass);
	}

	@Override
	public <T> List<T> query(Class<T> entityClass, String sql, List<String> tags,
			Object[] params,
			SpannerQueryOptions options) {
		String finalSql = sql;
		boolean allowPartialRead = false;
		if (options != null) {
			allowPartialRead = options.isAllowPartialRead();
			finalSql = applySortingPagingQueryOptions(options, sql);
		}
		return this.spannerConverter.mapToList(
				executeQuery(SpannerStatementQueryExecutor
						.buildStatementFromSqlWithArgs(finalSql, tags, params), options),
				entityClass, Optional.empty(),
				allowPartialRead);
	}

	@Override
	public <T> List<T> query(Class<T> entityClass, Statement statement) {
		return this.spannerConverter.mapToList(executeQuery(statement, null), entityClass,
				Optional.empty(), true);
	}

	@Override
	public <T> List<T> readAll(Class<T> entityClass, SpannerReadOptions options) {
		return read(entityClass, KeySet.all(), options);
	}

	@Override
	public <T> List<T> readAll(Class<T> entityClass) {
		return readAll(entityClass, (SpannerReadOptions) null);
	}

	@Override
	public <T> List<T> queryAll(Class<T> entityClass, SpannerQueryOptions options) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		String sql = "SELECT * FROM " + persistentEntity.tableName();
		return query(entityClass, applySortingPagingQueryOptions(options, sql), null,
				null, options);
	}

	public String applySortingPagingQueryOptions(SpannerQueryOptions options,
			String sql) {
		String r = applySort(options.getSort(), sql);
			if (options.hasLimit()) {
			r += " LIMIT " + options.getLimit();
			}
			if (options.hasLimit()) {
			r += " OFFSET " + options.getOffset();
			}
		return r;
	}

	private String applySort(Sort sort, String sql) {
		if (sort == null || sort.isUnsorted()) {
			return sql;
		}
		String s = "SELECT * FROM (" + sql + ") ORDER BY ";
		StringJoiner sj = new StringJoiner(" , ");
		sort.iterator().forEachRemaining(
				o -> sj.add(o.getProperty() + (o.isAscending() ? " ASC" : " DESC")));
		return s + sj.toString();
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
		applyMutationUsingEntity(this.mutationFactory::delete, entity);
	}

	@Override
	public void delete(Class entityClass, Key key) {
		applyMutationTwoArgs(this.mutationFactory::delete, entityClass, key);
	}

	@Override
	public <T> void delete(Class<T> entityClass, Iterable<? extends T> entities) {
		applyMutationTwoArgs(this.mutationFactory::delete, entityClass, entities);
	}

	@Override
	public void delete(Class entityClass, KeySet keys) {
		applyMutationTwoArgs(this.mutationFactory::delete, entityClass, keys);
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
	public <T> T performReadWriteTransaction(Function<SpannerTemplate, T> operations) {
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
	public <T> T performReadOnlyTransaction(Function<SpannerTemplate, T> operations,
			SpannerReadOptions readOptions) {
		SpannerReadOptions options = readOptions == null ? new SpannerReadOptions()
				: readOptions;
		try (ReadOnlyTransaction readOnlyTransaction = options.hasTimestamp()
				? this.databaseClient.readOnlyTransaction(
						TimestampBound.ofReadTimestamp(options.getTimestamp()))
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

	@VisibleForTesting
	public ResultSet executeQuery(Statement statement, SpannerQueryOptions options) {
		if (options == null) {
			return getReadContext().executeQuery(statement);
		}
		else {
			return (options.hasTimestamp() ? getReadContext(options.getTimestamp())
					: getReadContext()).executeQuery(statement,
							options.getQueryOptions());
		}
	}

	protected <T, U> void applyMutationTwoArgs(BiFunction<T, U, Mutation> function,
			T arg1,
			U arg2) {
		this.databaseClient.write(Arrays.asList(function.apply(arg1, arg2)));
	}

	private <T> void applyMutationUsingEntity(Function<T, Mutation> function, T arg) {
		applyMutationTwoArgs((T t, Object unused) -> function.apply(t), arg, null);
	}
}
