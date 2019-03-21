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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConversionUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.AfterDeleteEvent;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.AfterExecuteDmlEvent;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.AfterQueryEvent;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.AfterReadEvent;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.AfterSaveEvent;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.BeforeDeleteEvent;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.BeforeExecuteDmlEvent;
import org.springframework.cloud.gcp.data.spanner.core.mapping.event.BeforeSaveEvent;
import org.springframework.cloud.gcp.data.spanner.repository.query.SpannerStatementQueryExecutor;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * An implementation of {@link SpannerOperations}.
 *
 * @author Chengyuan Zhao
 * @author Ray Tsang
 * @author Mike Eltsufin
 * @since 1.1
 */
public class SpannerTemplate
		implements SpannerOperations, ApplicationEventPublisherAware {

	private static final Log LOGGER = LogFactory.getLog(SpannerTemplate.class);

	private final DatabaseClient databaseClient;

	private final SpannerMappingContext mappingContext;

	private final SpannerEntityProcessor spannerEntityProcessor;

	private final SpannerMutationFactory mutationFactory;

	private final SpannerSchemaUtils spannerSchemaUtils;

	private @Nullable ApplicationEventPublisher eventPublisher;

	public SpannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext,
			SpannerEntityProcessor spannerEntityProcessor,
			SpannerMutationFactory spannerMutationFactory,
			SpannerSchemaUtils spannerSchemaUtils) {
		Assert.notNull(databaseClient,
				"A valid database client for Spanner is required.");
		Assert.notNull(mappingContext,
				"A valid mapping context for Spanner is required.");
		Assert.notNull(spannerEntityProcessor,
				"A valid entity processor for Spanner is required.");
		Assert.notNull(spannerMutationFactory,
				"A valid Spanner mutation factory is required.");
		Assert.notNull(spannerSchemaUtils, "A valid Spanner schema utils is required.");
		this.databaseClient = databaseClient;
		this.mappingContext = mappingContext;
		this.spannerEntityProcessor = spannerEntityProcessor;
		this.mutationFactory = spannerMutationFactory;
		this.spannerSchemaUtils = spannerSchemaUtils;
	}

	@Override
	public void setApplicationEventPublisher(
			ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	protected ReadContext getReadContext() {
		return doWithOrWithoutTransactionContext((x) -> x,
				this.databaseClient::singleUse);
	}

	protected ReadContext getReadContext(Timestamp timestamp) {
		return doWithOrWithoutTransactionContext((x) -> x, () -> this.databaseClient
				.singleUse(TimestampBound.ofReadTimestamp(timestamp)));
	}

	public SpannerMappingContext getMappingContext() {
		return this.mappingContext;
	}

	public SpannerEntityProcessor getSpannerEntityProcessor() {
		return this.spannerEntityProcessor;
	}

	@Override
	public long executeDmlStatement(Statement statement) {
		Assert.notNull(statement, "A non-null statement is required.");
		maybeEmitEvent(new BeforeExecuteDmlEvent(statement));
		long rowsAffected = doWithOrWithoutTransactionContext(
				(x) -> x.executeUpdate(statement),
				() -> this.databaseClient.executePartitionedUpdate(statement));
		maybeEmitEvent(new AfterExecuteDmlEvent(statement, rowsAffected));
		return rowsAffected;
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
		List<T> entities = mapToListAndResolveChildren(
				executeRead(persistentEntity.tableName(), keys,
						persistentEntity.columns(), options),
				entityClass, (options != null) ? options.getIncludeProperties() : null,
				options != null && options.isAllowPartialRead());
		maybeEmitEvent(new AfterReadEvent(entities, keys, options));
		return entities;
	}

	@Override
	public <A> List<A> query(Function<Struct, A> rowFunc, Statement statement,
			SpannerQueryOptions options) {
		ArrayList<A> result = new ArrayList<>();
		try (ResultSet resultSet = executeQuery(statement, options)) {
			while (resultSet.next()) {
				result.add(rowFunc.apply(resultSet.getCurrentRowAsStruct()));
			}
		}
		maybeEmitEvent(new AfterQueryEvent(result, statement, options));
		return result;
	}

	@Override
	public <T> List<T> query(Class<T> entityClass, Statement statement,
			SpannerQueryOptions options) {
		List<T> entitites = queryAndResolveChildren(entityClass, statement, options);
		maybeEmitEvent(new AfterQueryEvent(entitites, statement, options));
		return entitites;
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
	public <T> List<T> queryAll(Class<T> entityClass,
			SpannerPageableQueryOptions options) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		String sql = "SELECT " + SpannerStatementQueryExecutor.getColumnsStringForSelect(
				persistentEntity) + " FROM " + persistentEntity.tableName();
		return query(entityClass,
				SpannerStatementQueryExecutor.buildStatementFromSqlWithArgs(
						SpannerStatementQueryExecutor.applySortingPagingQueryOptions(
								entityClass, options, sql, this.mappingContext),
						null, null, null, null),
				options);
	}

	@Override
	public void insert(Object object) {
		applySaveMutations(() -> this.mutationFactory.insert(object),
				Collections.singletonList(object), null);
	}

	@Override
	public void insertAll(Iterable objects) {
		applySaveMutations(() -> getMutationsForMultipleObjects(objects,
				this.mutationFactory::insert), objects, null);
	}

	@Override
	public void update(Object object) {
		applySaveMutations(() -> this.mutationFactory.update(object, null),
				Collections.singletonList(object), null);
	}

	@Override
	public void updateAll(Iterable objects) {
		applySaveMutations(() -> getMutationsForMultipleObjects(objects,
				(x) -> this.mutationFactory.update(x, null)), objects, null);
	}

	@Override
	public void update(Object object, String... includeProperties) {
		Set<String> incl = (includeProperties.length == 0) ? null
				: new HashSet<>(Arrays.asList(includeProperties));
		applySaveMutations(() -> this.mutationFactory.update(object, incl),
				Collections.singletonList(object), incl);
	}

	@Override
	public void update(Object object, Set<String> includeProperties) {
		applySaveMutations(() -> this.mutationFactory.update(object, includeProperties),
				Collections.singletonList(object), includeProperties);
	}

	@Override
	public void upsert(Object object) {
		applySaveMutations(() -> this.mutationFactory.upsert(object, null),
				Collections.singletonList(object), null);
	}

	@Override
	public void upsertAll(Iterable objects) {
		applySaveMutations(() -> getMutationsForMultipleObjects(objects,
				(x) -> this.mutationFactory.upsert(x, null)), objects, null);
	}

	@Override
	public void upsert(Object object, String... includeProperties) {
		Set<String> incl = (includeProperties.length == 0) ? null
				: new HashSet<>(Arrays.asList(includeProperties));
		applySaveMutations(() -> this.mutationFactory.upsert(object, incl),
				Collections.singletonList(object), incl);
	}

	@Override
	public void upsert(Object object, Set<String> includeProperties) {
		applySaveMutations(() -> this.mutationFactory.upsert(object, includeProperties),
				Collections.singletonList(object), includeProperties);
	}

	private void applySaveMutations(Supplier<List<Mutation>> mutationsSupplier,
			Iterable entities, Set<String> includeProperties) {
		maybeEmitEvent(new BeforeSaveEvent(entities, includeProperties));
		List<Mutation> mutations = mutationsSupplier.get();
		applyMutations(mutations);
		maybeEmitEvent(new AfterSaveEvent(mutations, entities, includeProperties));
	}

	@Override
	public void delete(Object entity) {
		applyDeleteMutations(Collections.singletonList(entity),
				Collections.singletonList(this.mutationFactory.delete(entity)));
	}

	@Override
	public void deleteAll(Iterable objects) {
		applyDeleteMutations(objects,
				(List<Mutation>) StreamSupport.stream(objects.spliterator(), false)
						.map(this.mutationFactory::delete).collect(Collectors.toList()));
	}

	private void applyDeleteMutations(Iterable objects, List<Mutation> mutations) {
		maybeEmitEvent(new BeforeDeleteEvent(mutations, objects, null, null));
		applyMutations(mutations);
		maybeEmitEvent(new AfterDeleteEvent(mutations, objects, null, null));
	}

	@Override
	public void delete(Class entityClass, Key key) {
		applyDeleteMutations(entityClass, KeySet.newBuilder().addKey(key).build(),
				Collections.singletonList(this.mutationFactory.delete(entityClass, key)));
	}

	@Override
	public void delete(Class entityClass, KeySet keys) {
		applyDeleteMutations(entityClass, keys, Collections
				.singletonList(this.mutationFactory.delete(entityClass, keys)));
	}

	private void applyDeleteMutations(Class entityClass, KeySet keys,
			List<Mutation> mutations) {
		maybeEmitEvent(new BeforeDeleteEvent(mutations, null, keys, entityClass));
		applyMutations(mutations);
		maybeEmitEvent(new AfterDeleteEvent(mutations, null, keys, entityClass));
	}

	@Override
	public long count(Class entityClass) {
		SpannerPersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(entityClass);
		Statement statement = Statement.of(
				String.format("SELECT COUNT(*) FROM %s", persistentEntity.tableName()));
		try (ResultSet resultSet = executeQuery(statement, null)) {
			resultSet.next();
			return resultSet.getLong(0);
		}
	}

	@Override
	public <T> T performReadWriteTransaction(Function<SpannerTemplate, T> operations) {
		return doWithOrWithoutTransactionContext((x) -> {
			throw new IllegalStateException(
					"There is already declarative transaction open. "
							+ "Spanner does not support nested transactions");
		}, () -> this.databaseClient.readWriteTransaction()
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
										SpannerTemplate.this.mutationFactory,
										SpannerTemplate.this.spannerSchemaUtils,
										transaction);
						return operations.apply(transactionSpannerTemplate);
					}
				}));
	}

	@Override
	public <T> T performReadOnlyTransaction(Function<SpannerTemplate, T> operations,
			SpannerReadOptions readOptions) {
		return doWithOrWithoutTransactionContext((x) -> {
			throw new IllegalStateException(
					"There is already declarative transaction open. "
							+ "Spanner does not support nested transactions");
		}, () -> {

			SpannerReadOptions options = (readOptions != null) ? readOptions
					: new SpannerReadOptions();
			try (ReadOnlyTransaction readOnlyTransaction = (options
					.getTimestamp() != null) ? this.databaseClient.readOnlyTransaction(
							TimestampBound.ofReadTimestamp(options.getTimestamp()))
							: this.databaseClient.readOnlyTransaction()) {
				return operations.apply(new ReadOnlyTransactionSpannerTemplate(
						SpannerTemplate.this.databaseClient,
						SpannerTemplate.this.mappingContext,
						SpannerTemplate.this.spannerEntityProcessor,
						SpannerTemplate.this.mutationFactory,
						SpannerTemplate.this.spannerSchemaUtils, readOnlyTransaction));
			}
		});
	}

	public ResultSet executeQuery(Statement statement, SpannerQueryOptions options) {

		long startTime = LOGGER.isDebugEnabled() ? System.currentTimeMillis() : 0;

		ResultSet resultSet = performQuery(statement, options);
		if (LOGGER.isDebugEnabled()) {
			String message;
			if (options == null) {
				message = "Executing query without additional options: " + statement;
			}
			else {
				message = getQueryLogMessageWithOptions(statement, options);
			}
			LOGGER.debug(message);
			LOGGER.debug("Query elapsed milliseconds: "
					+ (System.currentTimeMillis() - startTime));
		}
		return resultSet;
	}

	private String getQueryLogMessageWithOptions(Statement statement,
			SpannerQueryOptions options) {
		String message;
		StringBuilder logSb = new StringBuilder("Executing query")
				.append((options.getTimestamp() != null)
						? " at timestamp" + options.getTimestamp() : "");
		for (QueryOption queryOption : options.getQueryOptions()) {
			logSb.append(" with option: " + queryOption);
		}
		logSb.append(" : ").append(statement);
		message = logSb.toString();
		return message;
	}

	private ResultSet performQuery(Statement statement, SpannerQueryOptions options) {
		ResultSet resultSet;
		if (options == null) {
			resultSet = getReadContext().executeQuery(statement);
		}
		else {
			resultSet = ((options.getTimestamp() != null)
					? getReadContext(options.getTimestamp()) : getReadContext())
							.executeQuery(statement, options.getQueryOptions());
		}
		return resultSet;
	}

	private ResultSet executeRead(String tableName, KeySet keys, Iterable<String> columns,
			SpannerReadOptions options) {

		long startTime = LOGGER.isDebugEnabled() ? System.currentTimeMillis() : 0;

		ResultSet resultSet;

		ReadContext readContext = (options != null && options.getTimestamp() != null)
				? getReadContext(options.getTimestamp()) : getReadContext();

		if (options == null) {
			resultSet = readContext.read(tableName, keys, columns);
		}
		else if (options.getIndex() != null) {
			resultSet = readContext.readUsingIndex(tableName, options.getIndex(), keys,
					columns, options.getReadOptions());
		}
		else {
			resultSet = readContext.read(tableName, keys, columns,
					options.getReadOptions());
		}

		if (LOGGER.isDebugEnabled()) {
			StringBuilder logs = logColumns(tableName, keys, columns);
			logReadOptions(options, logs);
			LOGGER.debug(logs.toString());

			LOGGER.debug("Read elapsed milliseconds: "
					+ (System.currentTimeMillis() - startTime));
		}

		return resultSet;
	}

	private void logReadOptions(SpannerReadOptions options, StringBuilder logs) {
		if (options == null) {
			return;
		}
		if (options.getTimestamp() != null) {
			logs.append(" at timestamp " + options.getTimestamp());
		}
		for (ReadOption readOption : options.getReadOptions()) {
			logs.append(" with option: " + readOption);
		}
		if (options.getIndex() != null) {
			logs.append(" secondary index: " + options.getIndex());
		}
	}

	private StringBuilder logColumns(String tableName, KeySet keys,
			Iterable<String> columns) {
		StringBuilder logSb = new StringBuilder("Executing read on table " + tableName
				+ " with keys: " + keys + " and columns: ");
		StringJoiner sj = new StringJoiner(",");
		columns.forEach((col) -> sj.add(col));
		logSb.append(sj.toString());
		return logSb;
	}

	protected void applyMutations(Collection<Mutation> mutations) {
		LOGGER.debug("Applying Mutation: " + mutations);
		doWithOrWithoutTransactionContext((x) -> {
			x.buffer(mutations);
			return null;
		}, () -> {
			this.databaseClient.write(mutations);
			return null;
		});
	}

	private <T> List<T> queryAndResolveChildren(Class<T> entityClass, Statement statement,
			SpannerQueryOptions options) {
		return mapToListAndResolveChildren(executeQuery(statement, options), entityClass,
				(options != null) ? options.getIncludeProperties() : null,
				options != null && options.isAllowPartialRead());
	}

	private <T> List<T> mapToListAndResolveChildren(ResultSet resultSet,
			Class<T> entityClass, Set<String> includeProperties,
			boolean allowMissingColumns) {
		return resolveChildEntities(this.spannerEntityProcessor.mapToList(resultSet,
				entityClass, includeProperties, allowMissingColumns), includeProperties);
	}

	private <T> List<T> resolveChildEntities(List<T> entities,
			Set<String> includeProperties) {
		for (Object entity : entities) {
			resolveChildEntity(entity, includeProperties);
		}
		return entities;
	}

	private void resolveChildEntity(Object entity, Set<String> includeProperties) {
		SpannerPersistentEntity spannerPersistentEntity = this.mappingContext
				.getPersistentEntity(entity.getClass());
		PersistentPropertyAccessor accessor = spannerPersistentEntity
				.getPropertyAccessor(entity);
		spannerPersistentEntity.doWithInterleavedProperties(
				(PropertyHandler<SpannerPersistentProperty>) (
						spannerPersistentProperty) -> {
					if (includeProperties != null && !includeProperties
							.contains(spannerPersistentEntity.getName())) {
						return;
					}
					Class childType = spannerPersistentProperty.getColumnInnerType();
					SpannerPersistentEntity childPersistentEntity = this.mappingContext
							.getPersistentEntity(childType);

					Supplier<List> getChildrenEntitiesFunc = () -> queryAndResolveChildren(
							childType,
							SpannerStatementQueryExecutor.getChildrenRowsQuery(
									this.spannerSchemaUtils.getKey(entity),
									childPersistentEntity),
							null);

					accessor.setProperty(spannerPersistentProperty,
							spannerPersistentProperty.isLazyInterleaved()
									? ConversionUtils.wrapSimpleLazyProxy(
											getChildrenEntitiesFunc, List.class)
									: getChildrenEntitiesFunc.get());
				});
	}

	private List<Mutation> getMutationsForMultipleObjects(Iterable it,
			Function<Object, Collection<Mutation>> individualEntityMutationFunc) {
		return (List<Mutation>) StreamSupport.stream(it.spliterator(), false)
				.flatMap((x) -> individualEntityMutationFunc.apply(x).stream())
				.collect(Collectors.toList());
	}

	private TransactionContext getTransactionContext() {
		return TransactionSynchronizationManager.isActualTransactionActive()
				? ((SpannerTransactionManager.Tx) ((DefaultTransactionStatus) TransactionAspectSupport
						.currentTransactionStatus()).getTransaction())
								.getTransactionContext()
				: null;
	}

	private <A> A doWithOrWithoutTransactionContext(
			Function<TransactionContext, A> funcWithTransactionContext,
			Supplier<A> funcWithoutTransactionContext) {
		TransactionContext txContext = getTransactionContext();
		return (txContext != null) ? funcWithTransactionContext.apply(txContext)
				: funcWithoutTransactionContext.get();
	}

	private void maybeEmitEvent(ApplicationEvent event) {
		if (this.eventPublisher != null) {
			this.eventPublisher.publishEvent(event);
		}
	}

}
