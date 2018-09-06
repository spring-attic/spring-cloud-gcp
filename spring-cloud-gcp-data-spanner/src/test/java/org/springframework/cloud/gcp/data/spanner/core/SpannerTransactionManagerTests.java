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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/** @author Alexander Khimich */
@RunWith(PowerMockRunner.class)
@PrepareForTest(TransactionAspectSupport.class)
public class SpannerTransactionManagerTests {

	SpannerTransactionManager manager;

	@Mock
	DatabaseClient databaseClient;

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
		this.manager = new SpannerTransactionManager(this.databaseClient);
	}

	@Test
	public void testGetCurrentTX() {
		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		PowerMockito.mockStatic(TransactionAspectSupport.class);
		PowerMockito.when(TransactionAspectSupport.currentTransactionStatus()).thenReturn(status);

		SpannerTransactionManager.Tx actual = this.manager.getCurrentTX();
		Assert.assertEquals(tx, actual);

		Mockito.verify(status, times(1)).getTransaction();
	}

	@Test
	public void testGetCurrentTXNull() {
		Assert.assertNull(this.manager.getCurrentTX());
	}

	@Test
	public void testDoGetTransactionStarted() {
		TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
		Mockito.when(transactionManager.getState()).thenReturn(TransactionState.STARTED);

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		PowerMockito.mockStatic(TransactionAspectSupport.class);
		PowerMockito.when(TransactionAspectSupport.currentTransactionStatus()).thenReturn(status);

		Assert.assertEquals(this.manager.doGetTransaction(), tx);

		Mockito.verify(this.databaseClient, never()).transactionManager();
	}

	@Test
	public void testDoGetTransactionAborted() {
		TransactionManager transactionManagerAborted = Mockito.mock(TransactionManager.class);
		Mockito.when(transactionManagerAborted.getState()).thenReturn(TransactionState.ABORTED);

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManagerAborted);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		PowerMockito.mockStatic(TransactionAspectSupport.class);
		PowerMockito.when(TransactionAspectSupport.currentTransactionStatus()).thenReturn(status);

		TransactionManager transactionManagerNew = Mockito.mock(TransactionManager.class);
		Mockito.when(transactionManagerNew.getState()).thenReturn(TransactionState.STARTED);

		Mockito.when(this.databaseClient.transactionManager()).thenReturn(transactionManagerNew);

		Assert.assertNotEquals(
				"expected a new transaction but got the same one", tx, this.manager.doGetTransaction());

		Mockito.verify(this.databaseClient, times(1)).transactionManager();
	}

	@Test
	public void testDoGetTransactionNew() {
		TransactionManager transactionManagerNew = Mockito.mock(TransactionManager.class);
		Mockito.when(transactionManagerNew.getState()).thenReturn(TransactionState.STARTED);

		Mockito.when(this.databaseClient.transactionManager()).thenReturn(transactionManagerNew);

		SpannerTransactionManager.Tx actual = (SpannerTransactionManager.Tx) this.manager.doGetTransaction();
		Assert.assertNotNull(actual);
		Assert.assertEquals(
				ReflectionTestUtils.getField(actual, "transactionManager"), transactionManagerNew);

		Mockito.verify(this.databaseClient, times(1)).transactionManager();
	}

	@Test
	public void testDoBegin() {
		TransactionContext transactionContext = Mockito.mock(TransactionContext.class);

		TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
		Mockito.when(transactionManager.begin()).thenReturn(transactionContext);
		Mockito.when(transactionManager.getState()).thenReturn(TransactionState.STARTED);

		TransactionDefinition definition = Mockito.mock(TransactionDefinition.class);
		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		this.manager.doBegin(tx, definition);

		// need to use ReflectionTestUtils because tx is mocked and accessor won't work
		Assert.assertEquals(ReflectionTestUtils.getField(tx, "transactionContext"), transactionContext);

		Mockito.verify(transactionManager, times(1)).begin();
		Mockito.verify(transactionManager, times(1)).getState();
	}

	@Test
	public void testDoCommit() {
		TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
		Mockito.when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doNothing().when(transactionManager).commit();

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		this.manager.doCommit(status);

		Mockito.verify(transactionManager, times(1)).commit();
	}

	@Test
	public void testDoCommitNotStarted() {
		TransactionManager transactionManager = Mockito.mock(TransactionManager.class);

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		this.manager.doCommit(status);

		Mockito.verify(transactionManager, never()).commit();
	}

	@Test(expected = UnexpectedRollbackException.class)
	public void testDoCommitRollbackExceptions() {

		TransactionManager abortedTxManager = Mockito.mock(TransactionManager.class);
		Mockito.when(abortedTxManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doThrow(AbortedException.class).when(abortedTxManager).commit();

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", abortedTxManager);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		this.manager.doCommit(status);
	}

	@Test(expected = DuplicateKeyException.class)
	public void testDoCommitDupeException() {

		SpannerException exception = SpannerExceptionFactory.newSpannerException(
				ErrorCode.ALREADY_EXISTS, "this is from a test");

		TransactionManager dupeTxManager = Mockito.mock(TransactionManager.class);
		Mockito.when(dupeTxManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doThrow(exception).when(dupeTxManager).commit();

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", dupeTxManager);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		this.manager.doCommit(status);
	}

	@Test
	public void testDoRollback() {
		TransactionManager transactionManager = Mockito.mock(TransactionManager.class);
		Mockito.when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doNothing().when(transactionManager).rollback();

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		this.manager.doRollback(status);

		Mockito.verify(transactionManager, times(1)).rollback();
	}

	@Test
	public void testDoRollbackNotStarted() {
		TransactionManager transactionManager = Mockito.mock(TransactionManager.class);

		SpannerTransactionManager.Tx tx = Mockito.mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = Mockito.mock(DefaultTransactionStatus.class);
		Mockito.when(status.getTransaction()).thenReturn(tx);

		this.manager.doRollback(status);

		Mockito.verify(transactionManager, never()).rollback();
	}
}
