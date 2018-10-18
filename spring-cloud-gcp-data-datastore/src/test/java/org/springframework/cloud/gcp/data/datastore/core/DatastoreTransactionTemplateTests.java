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
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Entity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class tests that {@link DatastoreTemplate} is using the transction-specific
 * read-write when inside transactions.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class DatastoreTransactionTemplateTests {

	private final Key key = Key.newBuilder("a", "b", "c").build();

	@MockBean
	Datastore datastore;

	@MockBean
	Transaction transaction;

	@Autowired
	TransactionalService transactionalService;

	@MockBean
	ObjectToKeyFactory objectToKeyFactory;

	@Before
	public void setUp() {
		when(this.datastore.newTransaction()).thenReturn(this.transaction);
		when(this.transaction.isActive()).thenReturn(true);

		// This test class does not verify the integrity of key/object/entity
		// relationships.
		// DatastoreTemplateTests verifies the integrity of those relationships.
		when(this.objectToKeyFactory.getKeyFromId(any(), any())).thenReturn(this.key);
		when(this.objectToKeyFactory.getKeyFromObject(any(), any())).thenReturn(this.key);
		when(this.objectToKeyFactory.allocateKeyForObject(any(), any()))
				.thenReturn(this.key);
	}

	@Test
	public void newTransaction() {
		this.transactionalService.doInTransaction(new TestEntity(), new TestEntity());
		verify(this.datastore, times(1)).newTransaction();
		verify(this.transaction, times(1)).commit();
		verify(this.transaction, times(0)).rollback();
		verify(this.transaction, times(3)).put((FullEntity<?>) any());
		verify(this.transaction, times(1)).get((Key[]) any());
		verify(this.transaction, times(1)).delete(any());
	}

	@Test
	public void rollBackTransaction() {
		Exception exception = null;
		try {
			this.transactionalService.doInTransactionWithException(new TestEntity(),
					new TestEntity());
		}
		catch (Exception e) {
			exception = e;
		}
		assertNotNull(exception);
		verify(this.transaction, times(0)).commit();
		verify(this.transaction, times(1)).rollback();
		verify(this.datastore, times(1)).newTransaction();
	}

	@Test
	public void doWithoutTransactionTest() {
		this.transactionalService.doWithoutTransaction(new TestEntity(),
				new TestEntity());
		verify(this.transaction, never()).commit();
		verify(this.transaction, never()).rollback();
		verify(this.transaction, never()).put((FullEntity<?>) any());
		verify(this.transaction, never()).get((Key[]) any());
		verify(this.transaction, never()).delete(any());
		verify(this.datastore, never()).newTransaction();
	}

	@Test(expected = IllegalStateException.class)
	public void unsupportedIsolationTest() {
		this.transactionalService.doNothingUnsupportedIsolation();
	}

	@Test(expected = IllegalStateException.class)
	public void unsupportedPropagationTest() {
		this.transactionalService.doNothingUnsupportedPropagation();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readOnlySaveTest() {
		this.transactionalService.writingInReadOnly();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readOnlyDeleteTest() {
		this.transactionalService.deleteInReadOnly();
	}

	@Configuration
	@EnableTransactionManagement
	static class Config {

		@Bean
		DatastoreTemplate datastoreTemplateTemplate(Datastore datastore,
				ObjectToKeyFactory objectToKeyFactory) {
			return new DatastoreTemplate(datastore, mock(DatastoreEntityConverter.class),
					new DatastoreMappingContext(), objectToKeyFactory);
		}

		@Bean
		Datastore datastore() {
			return mock(Datastore.class);
		}

		@Bean
		ObjectToKeyFactory objectToKeyFactory() {
			return mock(ObjectToKeyFactory.class);
		}

		@Bean
		TransactionalService transactionalService() {
			return new TransactionalService();
		}

		@Bean
		DatastoreTransactionManager datastoreTransactionManager(Datastore datastore) {
			return new DatastoreTransactionManager(datastore);
		}
	}

	public static class TransactionalService {
		@Autowired
		DatastoreTemplate datastoreTemplate;

		@Transactional
		public void doInTransaction(TestEntity entity1, TestEntity entity2) {
			this.datastoreTemplate.findById("abc", TestEntity.class);
			this.datastoreTemplate.save(entity1);
			this.datastoreTemplate.save(entity2);
			this.datastoreTemplate.delete(entity1);
			this.datastoreTemplate.save(entity2);
		}

		@Transactional
		public void doInTransactionWithException(TestEntity entity1, TestEntity entity2) {
			this.datastoreTemplate.findById("abc", TestEntity.class);
			this.datastoreTemplate.save(entity1);
			this.datastoreTemplate.save(entity2);
			this.datastoreTemplate.delete(entity1);
			this.datastoreTemplate.save(entity2);
			throw new RuntimeException("oops");
		}

		public void doWithoutTransaction(TestEntity entity1, TestEntity entity2) {
			this.datastoreTemplate.findById("abc", TestEntity.class);
			this.datastoreTemplate.save(entity1);
			this.datastoreTemplate.save(entity2);
			this.datastoreTemplate.delete(entity1);
			this.datastoreTemplate.save(entity2);
		}

		@Transactional(isolation = Isolation.READ_UNCOMMITTED)
		public void doNothingUnsupportedIsolation() {
			// This method does nothing, but should fail anyway because of the unsupported
			// isolation.
		}

		@Transactional(propagation = Propagation.NESTED)
		public void doNothingUnsupportedPropagation() {
			// This method does nothing, but should fail anyway because of the unsupported
			// propagation.
		}

		@Transactional(readOnly = true)
		public void writingInReadOnly() {
			this.datastoreTemplate.save(new TestEntity());
		}

		@Transactional(readOnly = true)
		public void deleteInReadOnly() {
			this.datastoreTemplate.delete(new TestEntity());
		}
	}

	@Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;

		long val;
	}
}
