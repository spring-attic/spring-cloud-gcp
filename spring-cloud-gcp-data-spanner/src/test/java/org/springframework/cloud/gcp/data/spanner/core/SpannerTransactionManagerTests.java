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

import com.google.cloud.spanner.AbortedException;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.ErrorCode;
import com.google.cloud.spanner.SpannerException;
import com.google.cloud.spanner.SpannerExceptionFactory;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;
import com.google.cloud.spanner.TransactionManager.TransactionState;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Spanner transaction manager.
 *
 * @author Alexander Khimich
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 */
public class SpannerTransactionManagerTests {

	/**
	 * checks the exception for messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Mock
	DatabaseClient databaseClient;

	@Mock
	TransactionContext transactionContext;

	@Mock
	TransactionManager transactionManager;

	@Mock
	DefaultTransactionStatus status;

	SpannerTransactionManager.Tx tx;

	SpannerTransactionManager manager;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);

		this.manager = new SpannerTransactionManager(() -> this.databaseClient);

		this.tx = new SpannerTransactionManager.Tx(databaseClient);

		when(status.getTransaction()).thenReturn(tx);

		when(databaseClient.transactionManager()).thenReturn(this.transactionManager);

		TransactionSynchronizationManager.bindResource(this.databaseClient, tx);
	}

	@Test
	public void testDoGetTransactionStarted() {
		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		tx.transactionManager = transactionManager;
		tx.transactionContext = transactionContext;

		Assert.assertEquals(manager.doGetTransaction(), tx);

		verify(this.databaseClient, never()).transactionManager();
	}

	@Test
	public void testDoGetTransactionStartedReadOnly() {
		tx.transactionManager = transactionManager;
		tx.transactionContext = transactionContext;
		tx.isReadOnly = true;

		Assert.assertEquals(manager.doGetTransaction(), tx);

		verify(this.databaseClient, never()).transactionManager();
	}


	@Test
	public void testDoGetTransactionAborted() {
		TransactionManager transactionManagerAborted = mock(TransactionManager.class);
		when(transactionManagerAborted.getState()).thenReturn(TransactionState.ABORTED);

		tx.transactionManager = transactionManager;

		TransactionManager transactionManagerNew = mock(TransactionManager.class);
		when(transactionManagerNew.getState()).thenReturn(TransactionState.STARTED);

		when(this.databaseClient.transactionManager()).thenReturn(transactionManagerNew);

		Assert.assertNotEquals(
				"expected a new transaction but got the same one", tx, manager.doGetTransaction());
	}

	@Test
	public void testDoBegin() {
		when(transactionManager.begin()).thenReturn(transactionContext);

		TransactionSynchronizationManager.unbindResource(this.databaseClient);

		TransactionDefinition definition = new DefaultTransactionDefinition();

		manager.doBegin(tx, definition);

		Assert.assertEquals(tx.getTransactionManager(), transactionManager);
		Assert.assertEquals(tx.getTransactionContext(), transactionContext);
		Assert.assertFalse(tx.isReadOnly());

		verify(transactionManager, times(1)).begin();
	}

	@Test
	public void testDoBeginReadOnly() {
		when(transactionManager.begin()).thenReturn(transactionContext);

		DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
		definition.setReadOnly(true);

		TransactionSynchronizationManager.unbindResource(this.databaseClient);

		manager.doBegin(tx, definition);

		Assert.assertNull(tx.getTransactionManager());
		Assert.assertNotNull(tx.getTransactionContext());
		Assert.assertNotEquals(tx.getTransactionContext(), transactionContext);
		Assert.assertTrue(tx.isReadOnly());

		verify(transactionManager, times(0)).begin();
		verify(transactionManager, times(0)).getState();
	}

	@Test
	public void testDoCommit() {
		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		when(transactionManager.begin()).thenReturn(this.transactionContext);
		doNothing().when(transactionManager).commit();

		tx.transactionManager = transactionManager;
		tx.transactionContext = transactionContext;

		manager.doCommit(status);

		verify(transactionManager, times(1)).commit();
	}

	@Test
	public void testDoCommitNotStarted() {
		tx.transactionManager = transactionManager;
		tx.transactionContext = transactionContext;

		manager.doCommit(status);

		verify(transactionManager, never()).commit();
		verify(this.transactionContext, never()).close();
	}

	@Test
	public void testDoCommitRollbackExceptions() {

		this.expectedEx.expect(UnexpectedRollbackException.class);
		this.expectedEx.expectMessage("Transaction Got Rolled Back; " +
				"nested exception is com.google.cloud.spanner.AbortedException");

		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doThrow(AbortedException.class).when(transactionManager).commit();

		tx.transactionManager = transactionManager;

		manager.doCommit(status);
	}

	@Test
	public void testDoCommitDupeException() {

		this.expectedEx.expect(DuplicateKeyException.class);
		this.expectedEx.expectMessage("ALREADY_EXISTS; nested exception is " +
				"com.google.cloud.spanner.SpannerException: ALREADY_EXISTS: this is from a test");

		SpannerException exception = SpannerExceptionFactory.newSpannerException(
				ErrorCode.ALREADY_EXISTS, "this is from a test");

		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doThrow(exception).when(transactionManager).commit();

		tx.transactionManager = transactionManager;

		manager.doCommit(status);
	}

	@Test
	public void testDoRollback() {
		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		when(transactionManager.begin()).thenReturn(this.transactionContext);
		doNothing().when(transactionManager).rollback();

		tx.transactionContext = transactionContext;
		tx.transactionManager = transactionManager;

		manager.doRollback(status);

		verify(transactionManager, times(1)).rollback();
	}

	@Test
	public void testDoRollbackNotStarted() {
		tx.transactionManager = transactionManager;

		manager.doRollback(status);

		verify(transactionManager, never()).rollback();
	}
}
