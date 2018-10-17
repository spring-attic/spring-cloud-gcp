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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.Iterator;
import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cloud Datastore transaction manager
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreTransactionManager extends AbstractPlatformTransactionManager {
	private final Datastore datastore;

	public DatastoreTransactionManager(final Datastore datastore) {
		this.datastore = datastore;
	}

	@VisibleForTesting
	Tx getCurrentTX() {
		return TransactionSynchronizationManager.isActualTransactionActive()
				? (Tx) ((DefaultTransactionStatus) TransactionAspectSupport
						.currentTransactionStatus()).getTransaction()
				: null;
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		Tx tx = getCurrentTX();
		if (tx != null && tx.transaction != null && tx.transaction.isActive()) {
			return tx;
		}
		tx = new Tx();
		return tx;
	}

	@Override
	protected void doBegin(Object transactionObject,
			TransactionDefinition transactionDefinition) throws TransactionException {
		if (transactionDefinition
				.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT
				&& transactionDefinition
						.getIsolationLevel() != TransactionDefinition.ISOLATION_SERIALIZABLE) {
			throw new IllegalStateException(
					"DatastoreTransactionManager supports only isolation level "
							+ "TransactionDefinition.ISOLATION_DEFAULT or ISOLATION_SERIALIZABLE");
		}
		if (transactionDefinition
				.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRED) {
			throw new IllegalStateException(
					"DatastoreTransactionManager supports only propagation behavior "
							+ "TransactionDefinition.PROPAGATION_REQUIRED");
		}
		Tx tx = (Tx) transactionObject;
		Transaction transaction = this.datastore.newTransaction();
		if (transactionDefinition.isReadOnly()) {
			tx.transaction = new ReadOnlyTransaction(transaction);
		}
		else {
			tx.transaction = transaction;
		}
	}

	@Override
	protected void doCommit(DefaultTransactionStatus defaultTransactionStatus)
			throws TransactionException {
		Tx tx = (Tx) defaultTransactionStatus.getTransaction();
		try {
			if (tx.transaction.isActive()) {
				tx.transaction.commit();
			}
			else {
				this.logger.debug(
						"Transaction was not committed because it is no longer active.");
			}
		}
		catch (DatastoreException e) {
			throw new TransactionSystemException(
					"Cloud Datastore transaction failed to commit.", e);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus defaultTransactionStatus)
			throws TransactionException {
		Tx tx = (Tx) defaultTransactionStatus.getTransaction();
		try {
			if (tx.transaction.isActive()) {
				tx.transaction.rollback();
			}
			else {
				this.logger.debug(
						"Transaction was not rolled back because it is no longer active.");
			}
		}
		catch (DatastoreException e) {
			throw new TransactionSystemException(
					"Cloud Datastore transaction failed to rollback.", e);
		}
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return transaction == getCurrentTX();
	}

	public static class Tx {
		private Transaction transaction;

		public Transaction getTransaction() {
			return this.transaction;
		}

		@VisibleForTesting
		void setTransaction(Transaction transaction) {
			this.transaction = transaction;
		}
	}

	private static final class ReadOnlyTransaction implements Transaction {

		private final Transaction transaction;

		private ReadOnlyTransaction(Transaction transaction) {
			this.transaction = transaction;
		}

		@Override
		public Entity get(Key key) {
			return this.transaction.get(key);
		}

		@Override
		public Iterator<Entity> get(Key... key) {
			return this.transaction.get(key);
		}

		@Override
		public List<Entity> fetch(Key... keys) {
			return this.transaction.fetch(keys);
		}

		@Override
		public <T> QueryResults<T> run(Query<T> query) {
			return this.transaction.run(query);
		}

		@Override
		public void addWithDeferredIdAllocation(FullEntity<?>... entities) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public Entity add(FullEntity<?> entity) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public List<Entity> add(FullEntity<?>... entities) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public void update(Entity... entities) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public void delete(Key... keys) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public void putWithDeferredIdAllocation(FullEntity<?>... entities) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public Entity put(FullEntity<?> entity) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public List<Entity> put(FullEntity<?>... entities) {
			throw new UnsupportedOperationException(
					"The Cloud Datastore transaction is in read-only mode.");
		}

		@Override
		public Response commit() {
			return this.transaction.commit();
		}

		@Override
		public void rollback() {
			this.transaction.rollback();
		}

		@Override
		public boolean isActive() {
			return this.transaction.isActive();
		}

		@Override
		public Datastore getDatastore() {
			return this.transaction.getDatastore();
		}

		@Override
		public ByteString getTransactionId() {
			return this.transaction.getTransactionId();
		}
	}
}
