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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.google.cloud.ByteArray;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;

/** @author Alexander Khimich */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class SpannerTemplateTransactionManagerTests {
	static final List<Mutation> INSERT_MUTATION = Arrays.asList(Mutation.newInsertBuilder("custom_test_table").build());

	static final List<Mutation> UPSERT_MUTATION = Arrays
			.asList(Mutation.newInsertOrUpdateBuilder("custom_test_table").build());

	static final Mutation DELETE_MUTATION = Mutation.delete("custom_test_table", Key.of("1"));

	final AtomicReference<TransactionManager.TransactionState> transactionState = new AtomicReference<>();

	@MockBean
	DatabaseClient databaseClient;

	@MockBean
	ReadContext readContext;

	@MockBean
	TransactionContext transactionContext;

	@Autowired
	TransactionalService transactionalService;

	TransactionManager transactionManager;

	@Before
	public void setUp() {
		Mockito.when(this.databaseClient.singleUse()).thenReturn(this.readContext);
		this.transactionManager = Mockito.spy(TransactionManager.class);
		Mockito.doAnswer(
				invocation -> {
					this.transactionState.set(TransactionManager.TransactionState.STARTED);
					return this.transactionContext;
				})
				.when(this.transactionManager)
				.begin();

		Mockito.doAnswer(
				invocation -> {
					this.transactionState.set(
							TransactionManager.TransactionState.ROLLED_BACK);
					return null;
				})
				.when(this.transactionManager)
				.rollback();
		Mockito.doAnswer(
				invocation -> {
					this.transactionState.set(
							TransactionManager.TransactionState.COMMITTED);
					return null;
				})
				.when(this.transactionManager)
				.commit();
		Mockito.doAnswer(invocation -> this.transactionState.get())
				.when(this.transactionManager)
				.getState();

		Mockito.when(this.databaseClient.transactionManager()).thenReturn(this.transactionManager);
		Mockito.when(this.transactionManager.begin()).thenReturn(this.transactionContext);
	}

	@Test
	public void greenPathTransaction() {
		TestEntity entity1 = new TestEntity();
		TestEntity entity2 = new TestEntity();
		this.transactionalService.doInTransaction(entity1, entity2);
		Mockito.verify(this.transactionManager, Mockito.times(1)).begin();
		Mockito.verify(this.transactionManager, Mockito.times(1)).commit();
		Mockito.verify(this.transactionManager, Mockito.times(0)).rollback();

		Mockito.verify(this.databaseClient, Mockito.times(1)).transactionManager(); // only 1 transaction

		Mockito.verify(this.transactionContext, Mockito.times(2)).buffer(INSERT_MUTATION);
		Mockito.verify(this.transactionContext, Mockito.times(1))
				.read(
						eq("custom_test_table"),
						eq(KeySet.singleKey(Key.of("abc"))),
						Mockito.any(Iterable.class),
						Mockito.any());
		Mockito.verify(this.transactionContext, Mockito.times(1)).buffer(Arrays.asList(DELETE_MUTATION));
		Mockito.verify(this.transactionContext, Mockito.times(1)).buffer(UPSERT_MUTATION);
	}

	@Test
	public void rollBackTransaction() {
		TestEntity entity1 = new TestEntity();
		TestEntity entity2 = new TestEntity();
		Exception exception = null;
		try {
			this.transactionalService.doInTransactionWithException(entity1, entity2);
		}
		catch (Exception e) {
			exception = e;
		}
		assertNotNull(exception);

		Mockito.verify(this.transactionManager, Mockito.times(1)).begin();
		Mockito.verify(this.transactionManager, Mockito.times(0)).commit();
		Mockito.verify(this.transactionManager, Mockito.times(1)).rollback();
		Mockito.verify(this.databaseClient, Mockito.times(1)).transactionManager(); // only 1 transaction

		Mockito.verify(this.transactionContext, Mockito.times(2)).buffer(INSERT_MUTATION);
		Mockito.verify(this.transactionContext, Mockito.times(1))
				.read(
						eq("custom_test_table"),
						eq(KeySet.singleKey(Key.of("abc"))),
						Mockito.any(Iterable.class),
						Mockito.any());
		Mockito.verify(this.transactionContext, Mockito.times(1)).buffer(Arrays.asList(DELETE_MUTATION));
		Mockito.verify(this.transactionContext, Mockito.times(1)).buffer(UPSERT_MUTATION);
	}

	@Test
	public void doWithoutTransaction() {
		TestEntity entity1 = new TestEntity();
		TestEntity entity2 = new TestEntity();
		this.transactionalService.doWithoutTransaction(entity1, entity2);

		Mockito.verify(this.transactionManager, Mockito.never()).begin();
		Mockito.verify(this.transactionManager, Mockito.never()).commit();
		Mockito.verify(this.transactionManager, Mockito.never()).rollback();
		Mockito.verify(this.databaseClient, Mockito.never()).transactionManager(); // only 1 transaction

		Mockito.verify(this.transactionContext, Mockito.never()).buffer(Mockito.any(List.class));
		Mockito.verify(this.transactionContext, Mockito.never())
				.read(Mockito.anyString(), Mockito.any(KeySet.class), Mockito.any(Iterable.class), Mockito.any());
	}

	@Configuration
	@EnableTransactionManagement
	static class Config {

		@Bean
		public SpannerTemplate spannerTemplate(DatabaseClient databaseClient) {
			SpannerMappingContext mappingContext = new SpannerMappingContext();
			SpannerEntityProcessor objectMapper = Mockito.mock(SpannerEntityProcessor.class);
			SpannerMutationFactory mutationFactory = Mockito.mock(SpannerMutationFactory.class);
			Mockito.when(mutationFactory.insert(Mockito.any(TestEntity.class))).thenReturn(INSERT_MUTATION);
			Mockito.when(mutationFactory.upsert(Mockito.any(TestEntity.class), Mockito.any()))
					.thenReturn(UPSERT_MUTATION);
			Mockito.when(mutationFactory.delete(Mockito.any(TestEntity.class))).thenReturn(DELETE_MUTATION);
			SpannerSchemaUtils schemaUtils = new SpannerSchemaUtils(mappingContext, objectMapper, true);

			return new SpannerTemplate(
					databaseClient, mappingContext, objectMapper, mutationFactory, schemaUtils);
		}

		@Bean
		public SpannerTransactionManager spannerTransactionManager(DatabaseClient databaseClient) {
			return new SpannerTransactionManager(databaseClient);
		}

		@Bean
		TransactionalService transactionalService() {
			return new TransactionalService();
		}
	}

	public static class TransactionalService {
		@Autowired
		SpannerTemplate spannerTemplate;

		@Transactional
		public void doInTransaction(TestEntity entity1, TestEntity entity2) {
			this.spannerTemplate.read(TestEntity.class, Key.of("abc"));
			this.spannerTemplate.insert(entity1);
			this.spannerTemplate.insert(entity2);
			this.spannerTemplate.delete(entity1);
			this.spannerTemplate.upsert(entity2);
		}

		@Transactional
		public void doInTransactionWithException(TestEntity entity1, TestEntity entity2) {
			this.spannerTemplate.read(TestEntity.class, Key.of("abc"));
			this.spannerTemplate.insert(entity1);
			this.spannerTemplate.insert(entity2);
			this.spannerTemplate.delete(entity1);
			this.spannerTemplate.upsert(entity2);
			throw new RuntimeException("oops");
		}

		public void doWithoutTransaction(TestEntity entity1, TestEntity entity2) {
			this.spannerTemplate.read(TestEntity.class, Key.of("abc"));
			this.spannerTemplate.insert(entity1);
			this.spannerTemplate.insert(entity2);
			this.spannerTemplate.delete(entity1);
			this.spannerTemplate.upsert(entity2);
		}
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		long id2;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		ByteArray bytes;

		List<ByteArray> bytesList;

		List<Integer> integerList;

		double[] doubles;
	}
}
