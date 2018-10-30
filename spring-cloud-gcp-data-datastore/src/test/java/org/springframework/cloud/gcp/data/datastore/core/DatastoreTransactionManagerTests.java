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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager.Tx;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class DatastoreTransactionManagerTests {

	@Mock
	Datastore datastore;

	@Mock
	Transaction transaction;

	private Tx tx = new Tx();

	private DatastoreTransactionManager manager;

	private DefaultTransactionStatus status = mock(DefaultTransactionStatus.class);

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
		when(this.datastore.newTransaction()).thenReturn(this.transaction);
		when(this.status.getTransaction()).thenReturn(this.tx);
		this.manager = new DatastoreTransactionManager(this.datastore) {
			@Override
			protected Tx getCurrentTX() {
				return DatastoreTransactionManagerTests.this.tx;
			}
		};
	}

	@Test
	public void testDoGetTransactionActive() {
		when(this.transaction.isActive()).thenReturn(true);
		this.tx.setTransaction(this.transaction);
		assertSame(this.manager.doGetTransaction(), this.tx);
	}

	@Test
	public void testDoGetTransactionNotActive() {
		when(this.transaction.isActive()).thenReturn(false);
		this.tx.setTransaction(this.transaction);
		assertNotSame(this.tx, this.manager.doGetTransaction());
	}

	@Test
	public void testDoGetTransactionNoTransaction() {
		assertNotSame(this.tx, this.manager.doGetTransaction());
	}

	@Test
	public void testDoBegin() {
		TransactionDefinition definition = new DefaultTransactionDefinition();
		this.manager.doBegin(this.tx, definition);
		verify(this.datastore, times(1)).newTransaction();
	}

	@Test
	public void testDoCommit() {
		when(this.transaction.isActive()).thenReturn(true);
		this.tx.setTransaction(this.transaction);
		this.manager.doCommit(this.status);
		verify(this.transaction, times(1)).commit();
	}

	@Test(expected = TransactionSystemException.class)
	public void testDoCommitFailure() {
		when(this.transaction.isActive()).thenReturn(true);
		this.tx.setTransaction(this.transaction);
		when(this.transaction.commit()).thenThrow(new DatastoreException(0, "", ""));
		this.manager.doCommit(this.status);
	}

	@Test
	public void testDoCommitNotActive() {
		when(this.transaction.isActive()).thenReturn(false);
		this.tx.setTransaction(this.transaction);
		this.manager.doCommit(this.status);
		verify(this.transaction, never()).commit();
	}

	@Test
	public void testDoRollback() {
		when(this.transaction.isActive()).thenReturn(true);
		this.tx.setTransaction(this.transaction);
		this.manager.doRollback(this.status);
		verify(this.transaction, times(1)).rollback();
	}

	@Test(expected = TransactionSystemException.class)
	public void testDoRollbackFailure() {
		when(this.transaction.isActive()).thenReturn(true);
		this.tx.setTransaction(this.transaction);
		doThrow(new DatastoreException(0, "", "")).when(this.transaction).rollback();
		this.manager.doRollback(this.status);
	}

	@Test
	public void testDoRollbackNotActive() {
		when(this.transaction.isActive()).thenReturn(false);
		this.tx.setTransaction(this.transaction);
		this.manager.doRollback(this.status);
		verify(this.transaction, never()).rollback();
	}
}
