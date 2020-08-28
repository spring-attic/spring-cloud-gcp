/*
 * Copyright 2017-2018 the original author or authors.
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

import java.util.function.Supplier;

import javax.annotation.Nullable;

import com.google.api.core.ApiFuture;
import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.AsyncResultSet;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Spanner transaction manager.
 *
 * @author Alexander Khimich
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
public class SpannerTransactionManager extends AbstractPlatformTransactionManager {
	private final Supplier<DatabaseClient> databaseClientProvider;

	public SpannerTransactionManager(final Supplier databaseClientProvider) {
		this.databaseClientProvider = databaseClientProvider;
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		Tx tx = (Tx) TransactionSynchronizationManager.getResource(databaseClientProvider.get());
		if (tx != null && tx.getTransactionContext() != null
				&& (tx.getTransactionManager() != null
						&& tx.getTransactionManager().getState() == TransactionManager.TransactionState.STARTED ||
						tx.isReadOnly())) {
			return tx;
		}
		return new Tx(databaseClientProvider.get());
	}

	@Override
	protected void doBegin(Object transactionObject, TransactionDefinition transactionDefinition)
			throws TransactionException {
		if (transactionDefinition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new IllegalStateException(
					"SpannerTransactionManager supports only isolation level TransactionDefinition.ISOLATION_DEFAULT");
		}
		if (transactionDefinition.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRED) {
			throw new IllegalStateException(
					"SpannerTransactionManager supports only propagation behavior " +
							"TransactionDefinition.PROPAGATION_REQUIRED");
		}
		Tx tx = (Tx) transactionObject;
		if (transactionDefinition.isReadOnly()) {
			final ReadContext targetTransactionContext = this.databaseClientProvider.get()
					.readOnlyTransaction();
			tx.isReadOnly = true;
			tx.transactionManager = null;
			tx.transactionContext = new TransactionContext() {
				@Override
				public void buffer(Mutation mutation) {
					throw new IllegalStateException("Spanner transaction cannot apply" +
							" mutation because it is in readonly mode");
				}

				@Override
				public void buffer(Iterable<Mutation> iterable) {
					throw new IllegalStateException("Spanner transaction cannot apply" +
							" mutations because it is in readonly mode");
				}

				@Override
				public long executeUpdate(Statement statement) {
					throw new IllegalStateException("Spanner transaction cannot execute DML " +
							"because it is in readonly mode");
				}

				@Override
				public ApiFuture<Long> executeUpdateAsync(Statement statement) {
					throw new IllegalStateException("Spanner transaction cannot execute DML " +
							"because it is in readonly mode");
				}

				@Override
				public long[] batchUpdate(Iterable<Statement> iterable) {
					throw new IllegalStateException("Spanner transaction cannot execute DML " +
							"because it is in readonly mode");
				}

				@Override
				public ApiFuture<long[]> batchUpdateAsync(Iterable<Statement> iterable) {
					throw new IllegalStateException("Spanner transaction cannot execute DML " +
							"because it is in readonly mode");
				}

				@Override
				public ResultSet read(
						String s,
						KeySet keySet,
						Iterable<String> iterable,
						Options.ReadOption... readOptions) {
					return targetTransactionContext.read(s, keySet, iterable, readOptions);
				}

				@Override
				public AsyncResultSet readAsync(String s, KeySet keySet, Iterable<String> iterable, ReadOption... readOptions) {
					return targetTransactionContext.readAsync(s, keySet, iterable, readOptions);
				}

				@Override
				public ResultSet readUsingIndex(
						String s,
						String s1,
						KeySet keySet,
						Iterable<String> iterable,
						Options.ReadOption... readOptions) {
					return targetTransactionContext.readUsingIndex(s, s1, keySet, iterable, readOptions);
				}

				@Override
				public AsyncResultSet readUsingIndexAsync(
						String s,
						String s1,
						KeySet keySet,
						Iterable<String> iterable,
						Options.ReadOption... readOptions) {
					return targetTransactionContext.readUsingIndexAsync(s, s1, keySet, iterable, readOptions);
				}

				@Nullable
				@Override
				public Struct readRow(String s, Key key, Iterable<String> iterable) {
					return targetTransactionContext.readRow(s, key, iterable);
				}

				@Override
				public ApiFuture<Struct> readRowAsync(String s, Key key, Iterable<String> iterable) {
					return targetTransactionContext.readRowAsync(s, key, iterable);
				}

				@Nullable
				@Override
				public Struct readRowUsingIndex(
						String s, String s1, Key key, Iterable<String> iterable) {
					return targetTransactionContext.readRowUsingIndex(s, s1, key, iterable);
				}

				@Override
				public ApiFuture<Struct> readRowUsingIndexAsync(String s, String s1, Key key, Iterable<String> iterable) {
					return targetTransactionContext.readRowUsingIndexAsync(s, s1, key, iterable);
				}

				@Override
				public ResultSet executeQuery(
						Statement statement, Options.QueryOption... queryOptions) {
					return targetTransactionContext.executeQuery(statement, queryOptions);
				}

				@Override
				public AsyncResultSet executeQueryAsync(Statement statement, QueryOption... queryOptions) {
					return targetTransactionContext.executeQueryAsync(statement, queryOptions);
				}

				@Override
				public ResultSet analyzeQuery(Statement statement, QueryAnalyzeMode queryAnalyzeMode) {
					return targetTransactionContext.analyzeQuery(statement, queryAnalyzeMode);
				}

				@Override
				public void close() {
					targetTransactionContext.close();
				}
			};
		}
		else {
			tx.transactionManager = tx.databaseClient.transactionManager();
			tx.transactionContext = tx.getTransactionManager().begin();
			tx.isReadOnly = false;
		}

		TransactionSynchronizationManager.bindResource(tx.getDatabaseClient(), tx);
	}

	@Override
	protected void doCommit(DefaultTransactionStatus defaultTransactionStatus)
			throws TransactionException {
		Tx tx = (Tx) defaultTransactionStatus.getTransaction();
		try {
			if (tx.getTransactionManager() != null &&
					tx.getTransactionManager().getState() == TransactionManager.TransactionState.STARTED) {

				tx.getTransactionManager().commit();
			}
			if (tx.isReadOnly()) {
				tx.getTransactionContext().close();
			}
		}
		catch (AbortedException ex) {
			throw new UnexpectedRollbackException("Transaction Got Rolled Back", ex);
		}
		catch (SpannerException ex) {
			throw makeDataIntegrityViolationException(ex);
		}
	}

	private RuntimeException makeDataIntegrityViolationException(SpannerException e) {
		switch (e.getErrorCode()) {
		case ALREADY_EXISTS:
			return new DuplicateKeyException(e.getErrorCode().toString(), e);
		}
		return e;
	}

	@Override
	protected void doRollback(DefaultTransactionStatus defaultTransactionStatus)
			throws TransactionException {
		Tx tx = (Tx) defaultTransactionStatus.getTransaction();
		if (tx.getTransactionManager() != null
				&& tx.getTransactionManager().getState() == TransactionManager.TransactionState.STARTED) {
			tx.getTransactionManager().rollback();
		}
		if (tx.isReadOnly()) {
			tx.getTransactionContext().close();
		}
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return ((Tx) transaction).getTransactionContext() != null;
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		Tx tx = (Tx) transaction;
		TransactionSynchronizationManager.unbindResource(tx.getDatabaseClient());
		tx.transactionManager = null;
		tx.transactionContext = null;
		tx.isReadOnly = false;
	}

	/**
	 * A transaction object that holds the transaction context.
	 */
	public static class Tx {
		TransactionManager transactionManager;

		TransactionContext transactionContext;

		boolean isReadOnly;

		DatabaseClient databaseClient;

		public Tx(DatabaseClient databaseClient) {
			this.databaseClient = databaseClient;
		}

		public TransactionContext getTransactionContext() {
			return this.transactionContext;
		}

		public TransactionManager getTransactionManager() {
			return this.transactionManager;
		}

		public boolean isReadOnly() {
			return this.isReadOnly;
		};

		public DatabaseClient getDatabaseClient() {
			return databaseClient;
		}
	}
}
