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

import javax.annotation.Nullable;

import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Spanner transaction manager
 *
 * @author Alexander Khimich
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerTransactionManager extends AbstractPlatformTransactionManager {
	private final DatabaseClient databaseClient;

	public SpannerTransactionManager(final DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	protected Tx getCurrentTX() {
		try {
			return (SpannerTransactionManager.Tx) ((DefaultTransactionStatus) TransactionAspectSupport
					.currentTransactionStatus())
							.getTransaction();
		}
		catch (NoTransactionException e) {
			return null;
		}
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		Tx tx = getCurrentTX();
		if (tx != null
				&& tx.transactionManager.getState() == TransactionManager.TransactionState.STARTED) {
			logger.debug(tx + " reuse; state = " + tx.transactionManager.getState());
			return tx;
		}
		// create a new one if current is not there
		tx = new Tx();
		tx.transactionManager = this.databaseClient.transactionManager();
		logger.debug(tx + " create; state = " + tx.transactionManager.getState());
		return tx;
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
			final ReadContext targetTransactionContext = this.databaseClient
					.readOnlyTransaction();

			tx.transactionContext = new TransactionContext() {
				@Override
				public void buffer(Mutation mutation) {
					throw new IllegalStateException("Spanner transaction is in readonly mode");
				}

				@Override
				public void buffer(Iterable<Mutation> iterable) {
					throw new IllegalStateException("Spanner transaction is in readonly mode");
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
				public ResultSet readUsingIndex(
						String s,
						String s1,
						KeySet keySet,
						Iterable<String> iterable,
						Options.ReadOption... readOptions) {
					return targetTransactionContext.readUsingIndex(s, s1, keySet, iterable, readOptions);
				}

				@Nullable
				@Override
				public Struct readRow(String s, Key key, Iterable<String> iterable) {
					return targetTransactionContext.readRow(s, key, iterable);
				}

				@Nullable
				@Override
				public Struct readRowUsingIndex(
						String s, String s1, Key key, Iterable<String> iterable) {
					return targetTransactionContext.readRowUsingIndex(s, s1, key, iterable);
				}

				@Override
				public ResultSet executeQuery(
						Statement statement, Options.QueryOption... queryOptions) {
					return targetTransactionContext.executeQuery(statement, queryOptions);
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
			tx.transactionContext = tx.transactionManager.begin();
		}

		logger.debug(tx + " begin; state = " + tx.transactionManager.getState());
	}

	@Override
	protected void doCommit(DefaultTransactionStatus defaultTransactionStatus)
			throws TransactionException {
		Tx tx = (Tx) defaultTransactionStatus.getTransaction();
		try {
			logger.debug(tx + " beforeCommit; state = " + tx.transactionManager.getState());
			if (tx.transactionManager.getState() == TransactionManager.TransactionState.STARTED) {

				tx.transactionManager.commit();
				logger.debug(tx + " afterCommit; state = " + tx.transactionManager.getState());
			}
		}
		catch (AbortedException e) {
			throw new UnexpectedRollbackException("Transaction Got Rolled Back", e);
		}
		catch (SpannerException e) {
			throw makeDataIntegrityViolationException(e);
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
		logger.debug(tx + " beforeRollback; state = " + tx.transactionManager.getState());
		if (tx.transactionManager.getState() == TransactionManager.TransactionState.STARTED) {
			tx.transactionManager.rollback();
		}
		logger.debug(tx + " afterRollback; state = " + tx.transactionManager.getState());
	}

	protected boolean isExistingTransaction(Object transaction) {
		logger.debug("existing transaction " + transaction + "=" + getCurrentTX());
		return transaction == getCurrentTX();
	}

	public static class Tx {
		private TransactionManager transactionManager;

		private TransactionContext transactionContext;

		public TransactionContext getTransactionContext() {
			return this.transactionContext;
		}
	}
}
