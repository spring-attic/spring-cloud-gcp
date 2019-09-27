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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.function.Supplier;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Transaction;
import com.google.datastore.v1.TransactionOptions;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Cloud Datastore transaction manager.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class DatastoreTransactionManager extends AbstractPlatformTransactionManager {
	private final static TransactionOptions READ_ONLY_OPTIONS = TransactionOptions.newBuilder()
			.setReadOnly(TransactionOptions.ReadOnly.newBuilder().build())
			.build();

	private final Supplier<Datastore> datastore;

	public DatastoreTransactionManager(final Supplier<Datastore> datastore) {
		this.datastore = datastore;
	}

	@Override
	protected Object doGetTransaction() throws TransactionException {
		Tx tx = (Tx) TransactionSynchronizationManager.getResource(datastore.get());
		if (tx != null && tx.transaction != null && tx.transaction.isActive()) {
			return tx;
		}
		tx = new Tx(datastore.get());
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
		if (transactionDefinition.isReadOnly()) {
			tx.transaction = tx.datastore.newTransaction(READ_ONLY_OPTIONS);
		}
		else {
			tx.transaction = tx.datastore.newTransaction();
		}

		TransactionSynchronizationManager.bindResource(tx.datastore, tx);
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
		catch (DatastoreException ex) {
			throw new TransactionSystemException(
					"Cloud Datastore transaction failed to commit.", ex);
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
		catch (DatastoreException ex) {
			throw new TransactionSystemException(
					"Cloud Datastore transaction failed to rollback.", ex);
		}
	}

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		return ((Tx) transaction).transaction != null;
	}

	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		Tx tx = (Tx) transaction;
		TransactionSynchronizationManager.unbindResource(tx.datastore);
	}

	/**
	 * A class to contain the transaction context.
	 */
	public static class Tx {
		private Transaction transaction;
		private Datastore datastore;

		public Tx(Datastore datastore) {
			this.datastore = datastore;
		}

		public Transaction getTransaction() {
			return this.transaction;
		}

		public void setTransaction(Transaction transaction) {
			this.transaction = transaction;
		}

		public Datastore getDatastore() {
			return datastore;
		}

	}

}

