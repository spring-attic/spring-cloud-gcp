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

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Datastore.TransactionCallable;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Query.ResultType;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Descendants;
import org.springframework.data.annotation.Id;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class DatastoreTemplateTests {

	private final Datastore datastore = mock(Datastore.class);

	private final DatastoreEntityConverter datastoreEntityConverter = mock(
			DatastoreEntityConverter.class);

	private final ObjectToKeyFactory objectToKeyFactory = mock(ObjectToKeyFactory.class);

	private DatastoreTemplate datastoreTemplate;

	private Key createFakeKey(String val) {
		return new KeyFactory("project").setKind("custom_test_kind").newKey(val);
	}

	private final Query testEntityQuery = GqlQuery
			.newGqlQueryBuilder(ResultType.PROJECTION_ENTITY, "fake query").build();

	private final Query findAllTestEntityQuery = Query.newEntityQueryBuilder()
			.setKind("custom_test_kind").build();
	private final Key key1 = createFakeKey("key1");
	private final Key key2 = createFakeKey("key2");
	private final Entity e1 = Entity.newBuilder(this.key1).build();
	private final Entity e2 = Entity.newBuilder(this.key2).build();
	private TestEntity ob1;
	private TestEntity ob2;
	private ChildEntity childEntity;

	@Before
	public void setup() {
		this.datastoreTemplate = new DatastoreTemplate(this.datastore,
				this.datastoreEntityConverter, new DatastoreMappingContext(),
				this.objectToKeyFactory);

		this.ob1 = new TestEntity();
		this.ob2 = new TestEntity();

		this.ob1.id = "value";
		this.ob2.id = "value";

		Entity ce1 = Entity.newBuilder(createFakeKey("key3")).build();

		Query childTestEntityQuery = Query.newEntityQueryBuilder().setKind("child_entity")
				.setFilter(PropertyFilter.hasAncestor(key1)).build();

		this.childEntity = new ChildEntity();
		this.childEntity.id = "child_id";

		QueryResults childTestEntityQueryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(ce1).iterator().forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(childTestEntityQueryResults).forEachRemaining(any());

		when(this.datastoreEntityConverter.read(eq(TestEntity.class), any()))
				.thenAnswer(invocation -> {
					Object ret;
					if (invocation.getArgument(1) == e1) {
						ret = this.ob1;
					}
					else {
						ret = this.ob2;
					}
					return ret;
				});

		when(this.datastoreEntityConverter.read(eq(ChildEntity.class), same(ce1)))
				.thenReturn(childEntity);

		QueryResults testEntityQueryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(e1, e2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(testEntityQueryResults).forEachRemaining(any());

		when(this.datastore.run(any())).thenAnswer(invocation -> {
			Query query = invocation.getArgument(0);
			Object result = null;
			if (this.testEntityQuery.equals(query)
					|| this.findAllTestEntityQuery.equals(query)) {
				result = testEntityQueryResults;
			}
			else if (childTestEntityQuery.equals(query)) {
				result = childTestEntityQueryResults;
			}
			return result;
		});

		when(this.datastore.get(ArgumentMatchers.<Key[]> any()))
				.thenReturn(ImmutableList.of(this.e1, this.e2).iterator());

		when(this.objectToKeyFactory.getKeyFromObject(any(), any()))
				.thenAnswer(invocation -> {
					Object ret;
					if (invocation.getArgument(0) == this.ob1) {
						ret = this.key1;
					}
					else {
						ret = this.key2;
					}
					return ret;
				});
	}

	@Test
	public void performTransactionTest() {

		DatastoreReaderWriter transactionContext = mock(DatastoreReaderWriter.class);

		when(this.datastore.runInTransaction(any())).thenAnswer(invocation -> {
			TransactionCallable<String> callable = invocation.getArgument(0);
			return callable.run(transactionContext);
		});

		Iterator<Entity> e1 = Collections
				.singletonList(Entity.newBuilder(this.key1).build())
				.iterator();
		when(transactionContext.get(ArgumentMatchers.<Key[]> any())).thenReturn(e1);

		String finalResult = this.datastoreTemplate
				.performTransaction(datastoreOperations -> {
					datastoreOperations.save(this.ob1);
					datastoreOperations.findById("ignored", TestEntity.class);
					return "all done";
				});

		assertEquals("all done", finalResult);
		verify(transactionContext, times(1)).put((FullEntity<?>) any());
		verify(transactionContext, times(1)).get((Key[]) any());
	}

	@Test
	public void findByIdTest() {
		TestEntity result = this.datastoreTemplate.findById(this.key1, TestEntity.class);
		assertEquals(this.ob1, result);
		assertThat(result.childEntities, contains(this.childEntity));
	}

	@Test
	public void findByIdNotFoundTest() {
		when(this.datastore.get(ArgumentMatchers.<Key[]> any())).thenReturn(null);
		assertNull(
				this.datastoreTemplate.findById(createFakeKey("key0"), TestEntity.class));
	}

	@Test
	public void findAllByIdTest() {
		List<Key> keys = ImmutableList.of(this.key1, this.key2);
		assertThat(this.datastoreTemplate.findAllById(keys, TestEntity.class),
				contains(this.ob1, this.ob2));
	}

	@Test
	public void saveTest() {
		when(this.datastore.put((FullEntity<?>) any())).thenReturn(this.e1);
		assertTrue(this.datastoreTemplate.save(this.ob1) instanceof TestEntity);
		verify(this.datastore, times(1)).put(eq(this.e1));
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
	}

	@Test
	public void saveAndAllocateIdTest() {
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.ob1), any()))
				.thenReturn(this.key1);
		when(this.datastore.put((FullEntity<?>) any())).thenReturn(this.e1);
		assertTrue(this.datastoreTemplate.save(this.ob1) instanceof TestEntity);
		verify(this.datastore, times(1)).put(eq(this.e1));
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
	}

	@Test
	public void saveAllTest() {
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.ob1), any()))
				.thenReturn(key1);
		when(this.objectToKeyFactory.getKeyFromObject(same(this.ob2), any()))
				.thenReturn(key2);

		when(this.datastore.put(any(), any()))
				.thenReturn(ImmutableList.of(this.e1, this.e2));

		this.datastoreTemplate.saveAll(ImmutableList.of(this.ob1, this.ob2));
		verify(this.datastore, times(1)).put(eq(this.e1), eq(this.e2));
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob2), notNull());
	}

	@Test
	public void findAllTest() {
		this.datastoreTemplate.findAll(TestEntity.class);
		assertThat(this.datastoreTemplate.findAll(TestEntity.class),
				contains(this.ob1, this.ob2));
	}

	@Test
	public void queryTest() {
		assertThat(this.datastoreTemplate.query((Query<Entity>) this.testEntityQuery,
				TestEntity.class), contains(this.ob1, this.ob2));
	}

	@Test
	public void queryKeysTest() {
		KeyQuery keyQuery = GqlQuery.newKeyQueryBuilder().build();
		this.datastoreTemplate.queryKeys(keyQuery).iterator();
		verify(this.datastore, times(1)).run(keyQuery);
	}

	@Test
	public void countTest() {
		QueryResults<Key> queryResults = mock(QueryResults.class);
		when(queryResults.getResultClass()).thenReturn((Class)Key.class);
		doAnswer(invocation -> {
			ImmutableList.of(this.key1, this.key2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(queryResults).forEachRemaining(any());
		when(this.datastore
				.run(eq(Query.newKeyQueryBuilder().setKind("custom_test_kind").build())))
						.thenReturn(queryResults);
		assertEquals(2, this.datastoreTemplate.count(TestEntity.class));
	}

	@Test
	public void existsByIdTest() {
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		doReturn(new Object()).when(spy).findById(same(this.key1), eq(Object.class));
		doReturn(null).when(spy).findById(same(this.key2), eq(Object.class));
		assertTrue(spy.existsById(this.key1, Object.class));
		assertFalse(spy.existsById(this.key2, Object.class));
	}

	@Test
	public void deleteByIdTest() {
		when(this.objectToKeyFactory.getKeyFromId(same(this.key1), any()))
				.thenReturn(this.key1);
		this.datastoreTemplate.deleteById(this.key1, TestEntity.class);
		verify(this.datastore, times(1)).delete(same(this.key1));
	}

	@Test
	public void deleteAllByIdTest() {
		when(this.objectToKeyFactory.getKeyFromId(same(this.key1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromId(same(this.key2), any()))
				.thenReturn(this.key2);
		this.datastoreTemplate.deleteAllById(ImmutableList.of(this.key1, this.key2),
				TestEntity.class);
		verify(this.datastore, times(1)).delete(same(this.key1), same(this.key2));
	}

	@Test
	public void deleteObjectTest() {
		this.datastoreTemplate.delete(this.ob1);
		verify(this.datastore, times(1)).delete(same(this.key1));
	}

	@Test
	public void deleteMultipleObjectsTest() {
		this.datastoreTemplate.deleteAll(ImmutableList.of(this.ob1, this.ob2));
		verify(this.datastore, times(1)).delete(eq(key1), eq(key2));
	}

	@Test
	public void deleteAllTest() {
		QueryResults<Key> queryResults = mock(QueryResults.class);
		when(queryResults.getResultClass()).thenReturn((Class)Key.class);
		doAnswer(invocation -> {
			ImmutableList.of(this.key1, this.key2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(queryResults).forEachRemaining(any());
		when(this.datastore
				.run(eq(Query.newKeyQueryBuilder().setKind("custom_test_kind").build())))
						.thenReturn(queryResults);
		assertEquals(2, this.datastoreTemplate.deleteAll(TestEntity.class));
		verify(this.datastore, times(1)).delete(same(this.key1), same(this.key2));
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;

		@Descendants
		List<ChildEntity> childEntities;

		@Override
		public boolean equals(Object other) {
			TestEntity o = (TestEntity) other;
			return this.id.equals(o.id);
		}
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "child_entity")
	private static class ChildEntity {
		@Id
		String id;

		@Override
		public boolean equals(Object other) {
			ChildEntity o = (ChildEntity) other;
			return this.id.equals(o.id);
		}
	}

}
