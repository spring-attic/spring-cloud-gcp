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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

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

	@Before
	public void initMocks() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testDoGetTransactionStarted() {
		TransactionManager transactionManager = mock(TransactionManager.class);
		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};

		Assert.assertEquals(manager.doGetTransaction(), tx);

		verify(this.databaseClient, never()).transactionManager();
	}

	@Test
	public void testDoGetTransactionAborted() {
		TransactionManager transactionManagerAborted = mock(TransactionManager.class);
		when(transactionManagerAborted.getState()).thenReturn(TransactionState.ABORTED);

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManagerAborted);

		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};

		TransactionManager transactionManagerNew = mock(TransactionManager.class);
		when(transactionManagerNew.getState()).thenReturn(TransactionState.STARTED);

		when(this.databaseClient.transactionManager()).thenReturn(transactionManagerNew);

		Assert.assertNotEquals(
				"expected a new transaction but got the same one", tx, manager.doGetTransaction());

		verify(this.databaseClient, times(1)).transactionManager();
	}

	@Test
	public void testDoGetTransactionNew() {
		TransactionManager transactionManagerNew = mock(TransactionManager.class);
		when(transactionManagerNew.getState()).thenReturn(TransactionState.STARTED);
		when(this.databaseClient.transactionManager()).thenReturn(transactionManagerNew);
		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return null;
			}
		};

		SpannerTransactionManager.Tx actual = (SpannerTransactionManager.Tx) manager.doGetTransaction();
		Assert.assertNotNull(actual);
		Assert.assertEquals(
				ReflectionTestUtils.getField(actual, "transactionManager"), transactionManagerNew);

		verify(this.databaseClient, times(1)).transactionManager();
	}

	@Test
	public void testDoBegin() {
		TransactionContext transactionContext = mock(TransactionContext.class);

		TransactionManager transactionManager = mock(TransactionManager.class);
		when(transactionManager.begin()).thenReturn(transactionContext);
		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);

		TransactionDefinition definition = new DefaultTransactionDefinition();
		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);
		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};

		manager.doBegin(tx, definition);

		// need to use ReflectionTestUtils because tx is mocked and accessor won't work
		Assert.assertEquals(ReflectionTestUtils.getField(tx, "transactionContext"), transactionContext);

		verify(transactionManager, times(1)).begin();
		verify(transactionManager, times(1)).getState();
	}

	@Test
	public void testDoCommit() {
		TransactionManager transactionManager = mock(TransactionManager.class);
		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		when(transactionManager.begin()).thenReturn(this.transactionContext);
		doNothing().when(transactionManager).commit();

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		when(tx.getTransactionContext()).thenReturn(this.transactionContext);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = mock(DefaultTransactionStatus.class);
		when(status.getTransaction()).thenReturn(tx);
		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};

		manager.doCommit(status);

		verify(transactionManager, times(1)).commit();
	}

	@Test
	public void testDoCommitNotStarted() {
		TransactionManager transactionManager = mock(TransactionManager.class);

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);
		when(tx.getTransactionContext()).thenReturn(this.transactionContext);

		DefaultTransactionStatus status = mock(DefaultTransactionStatus.class);
		when(status.getTransaction()).thenReturn(tx);
		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};
		manager.doCommit(status);

		verify(transactionManager, never()).commit();
		verify(this.transactionContext, never()).close();
	}

	@Test
	public void testDoCommitRollbackExceptions() {

		this.expectedEx.expect(UnexpectedRollbackException.class);
		this.expectedEx.expectMessage("Transaction Got Rolled Back; " +
				"nested exception is com.google.cloud.spanner.AbortedException");

		TransactionManager abortedTxManager = mock(TransactionManager.class);
		when(abortedTxManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doThrow(AbortedException.class).when(abortedTxManager).commit();

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", abortedTxManager);

		DefaultTransactionStatus status = mock(DefaultTransactionStatus.class);
		when(status.getTransaction()).thenReturn(tx);
		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};
		manager.doCommit(status);
	}

	@Test
	public void testDoCommitDupeException() {

		this.expectedEx.expect(DuplicateKeyException.class);
		this.expectedEx.expectMessage("ALREADY_EXISTS; nested exception is " +
				"com.google.cloud.spanner.SpannerException: ALREADY_EXISTS: this is from a test");

		SpannerException exception = SpannerExceptionFactory.newSpannerException(
				ErrorCode.ALREADY_EXISTS, "this is from a test");

		TransactionManager dupeTxManager = mock(TransactionManager.class);
		when(dupeTxManager.getState()).thenReturn(TransactionState.STARTED);
		Mockito.doThrow(exception).when(dupeTxManager).commit();

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", dupeTxManager);

		DefaultTransactionStatus status = mock(DefaultTransactionStatus.class);
		when(status.getTransaction()).thenReturn(tx);
		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};
		manager.doCommit(status);
	}

	@Test
	public void testDoRollback() {
		TransactionManager transactionManager = mock(TransactionManager.class);
		when(transactionManager.getState()).thenReturn(TransactionState.STARTED);
		when(transactionManager.begin()).thenReturn(this.transactionContext);
		doNothing().when(transactionManager).rollback();

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		when(tx.getTransactionContext()).thenReturn(this.transactionContext);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = mock(DefaultTransactionStatus.class);
		when(status.getTransaction()).thenReturn(tx);

		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};

		manager.doRollback(status);

		verify(transactionManager, times(1)).rollback();
	}

	@Test
	public void testDoRollbackNotStarted() {
		TransactionManager transactionManager = mock(TransactionManager.class);

		SpannerTransactionManager.Tx tx = mock(SpannerTransactionManager.Tx.class);
		ReflectionTestUtils.setField(tx, "transactionManager", transactionManager);

		DefaultTransactionStatus status = mock(DefaultTransactionStatus.class);
		when(status.getTransaction()).thenReturn(tx);
		SpannerTransactionManager manager = new SpannerTransactionManager(() -> this.databaseClient) {
			@Override
			protected Tx getCurrentTX() {
				return tx;
			}
		};

		manager.doRollback(status);

		verify(transactionManager, never()).rollback();
	}
}
