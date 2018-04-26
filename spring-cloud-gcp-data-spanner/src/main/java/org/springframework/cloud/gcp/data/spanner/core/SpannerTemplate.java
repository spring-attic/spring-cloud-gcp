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
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
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

	private static final Log LOGGER = LogFactory.getLog(SpannerTemplate.class);

	private final DatabaseClient databaseClient;

	private final SpannerMappingContext mappingContext;

	private final SpannerEntityProcessor spannerEntityProcessor;

	private final SpannerMutationFactory mutationFactory;

	public SpannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext, SpannerEntityProcessor spannerEntityProcessor,
			SpannerMutationFactory spannerMutationFactory) {
		Assert.notNull(databaseClient,
				"A valid database client for Spanner is required.");
		Assert.notNull(mappingContext,
				"A valid mapping context for Spanner is required.");
		Assert.notNull(spannerEntityProcessor,
				"A valid entity processor for Spanner is required.");
		Assert.notNull(spannerMutationFactory,
				"A valid Spanner mutation factory is required.");
		this.databaseClient = databaseClient;
		this.mappingContext = mappingContext;
		this.spannerEntityProcessor = spannerEntityProcessor;
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

	public SpannerEntityProcessor getSpannerEntityProcessor() {
		return this.spannerEntityProcessor;
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
		return this.spannerEntityProcessor.mapToList(executeRead(persistentEntity.tableName(),
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
		return this.spannerEntityProcessor.mapToList(
				executeQuery(SpannerStatementQueryExecutor
						.buildStatementFromSqlWithArgs(finalSql, tags, params), options),
				entityClass, Optional.empty(),
				allowPartialRead);
	}

	@Override
	public <T> List<T> query(Class<T> entityClass, Statement statement) {
		return this.spannerEntityProcessor.mapToList(executeQuery(statement, null), entityClass,
				Optional.empty(), true);
	}

	@Override
	public <T> List<T> readAll(Class<T> entityClass, SpannerReadOptions options) {
		return read(entityClass, KeySet.all(), options);
	}

	@Override
	public <T> List<T> readAll(Class<T> entityClass) {
		return readAll(entityClass, null);
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
		StringBuilder sb = applySort(options.getSort(), wrapAsSubSelect(sql));
			if (options.hasLimit()) {
				sb.append(" LIMIT ").append(options.getLimit());
			}
			if (options.hasOffset()) {
				sb.append(" OFFSET ").append(options.getOffset());
			}
		return sb.toString();
	}

	private StringBuilder applySort(Sort sort, StringBuilder sql) {
		if (sort == null || sort.isUnsorted()) {
			return sql;
		}
		sql.append(" ORDER BY ");
		StringJoiner sj = new StringJoiner(" , ");
		sort.iterator().forEachRemaining(
				o -> sj.add(o.getProperty() + (o.isAscending() ? " ASC" : " DESC")));
		return sql.append(sj);
	}

	private StringBuilder wrapAsSubSelect(String sql) {
		return new StringBuilder("SELECT * FROM (").append(sql).append(")");
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
				"SELECT COUNT(*) FROM %s", persistentEntity.tableName()));
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
					public T run(TransactionContext transaction) { // @formatter:off
						ReadWriteTransactionSpannerTemplate transactionSpannerTemplate =
										new ReadWriteTransactionSpannerTemplate(
										// @formatter:on
										SpannerTemplate.this.databaseClient,
										SpannerTemplate.this.mappingContext,
										SpannerTemplate.this.spannerEntityProcessor,
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
					SpannerTemplate.this.spannerEntityProcessor,
					SpannerTemplate.this.mutationFactory, readOnlyTransaction));
		}
	}

	private ResultSet executeRead(String tableName, KeySet keys, Iterable<String> columns,
			SpannerReadOptions options) {

		if (LOGGER.isDebugEnabled()) {
			StringBuilder logs = logColumns(tableName, keys, columns);
			logReadOptions(options, logs);
			LOGGER.debug(logs.toString());
		}

		if (options == null) {
			return getReadContext().read(tableName, keys, columns);
		}

		ReadContext readContext = options.hasTimestamp()
				? getReadContext(options.getTimestamp())
				: getReadContext();

		if (options.hasIndex()) {
			return readContext.readUsingIndex(tableName, options.getIndex(), keys,
					columns, options.getReadOptions());
		}

		return readContext.read(tableName, keys, columns,
				options.getReadOptions());
	}

	private void logReadOptions(SpannerReadOptions options, StringBuilder logs) {
		if (options == null) {
			return;
		}
		if (options.hasTimestamp()) {
			logs.append(" at timestamp " + options.getTimestamp());
		}
		for (ReadOption readOption : options.getReadOptions()) {
			logs.append(" with option: " + readOption);
		}
		if (options.hasIndex()) {
			logs.append(" secondary index: " + options.getIndex());
		}
	}

	private StringBuilder logColumns(String tableName, KeySet keys, Iterable<String> columns) {
		StringBuilder logSb = new StringBuilder("Executing read on table "
				+ tableName
				+ " with keys: "
				+ keys
				+ " and columns: ");
		StringJoiner sj = new StringJoiner(",");
		columns.forEach(col -> sj.add(col));
		logSb.append(sj.toString());
		return logSb;
	}

	@VisibleForTesting
	public ResultSet executeQuery(Statement statement, SpannerQueryOptions options) {
		ResultSet resultSet;
		if (options == null) {
			resultSet = getReadContext().executeQuery(statement);
		}
		else {
			resultSet = (options.hasTimestamp() ? getReadContext(options.getTimestamp())
					: getReadContext()).executeQuery(statement,
							options.getQueryOptions());
		}
		if (LOGGER.isDebugEnabled()) {
			String message;
			if (options == null) {
				message = "Executing query without additional options: " + statement;
			}
			else {
				StringBuilder logSb = new StringBuilder(
						"Executing query").append(options.hasTimestamp()
								? " at timestamp" + options.getTimestamp()
								: "");
				for (QueryOption queryOption : options.getQueryOptions()) {
					logSb.append(" with option: " + queryOption);
				}
				logSb.append(" : ").append(statement);
				message = logSb.toString();
			}
			LOGGER.debug(message);
		}
		return resultSet;
	}

	protected <T, U> void applyMutationTwoArgs(BiFunction<T, U, Mutation> function,
			T arg1,
			U arg2) {
		Mutation mutation = function.apply(arg1, arg2);
		LOGGER.debug("Applying Mutation: " + mutation);
		this.databaseClient.write(Arrays.asList(mutation));
	}

	private <T> void applyMutationUsingEntity(Function<T, Mutation> function, T arg) {
		applyMutationTwoArgs((T t, Object unused) -> function.apply(t), arg, null);
	}
}
