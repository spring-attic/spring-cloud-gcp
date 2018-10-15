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
import java.util.LinkedList;
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
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
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
import static org.mockito.Mockito.mock;
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

	// A fake entity query used for testing.
	private final Query testEntityQuery = GqlQuery
			.newGqlQueryBuilder(ResultType.PROJECTION_ENTITY, "fake query").build();

	// This is the query that is expected to be constructed by the template.
	private final Query findAllTestEntityQuery = Query.newEntityQueryBuilder()
			.setKind("custom_test_kind").build();

	// The keys, entities, and objects below are constructed for all tests below. the
	// number of each
	// object here corresponds to the same thing across keys, entities, objects.
	private final Key key1 = createFakeKey("key1");
	private final Key key2 = createFakeKey("key2");
	private final Key badKey = createFakeKey("badkey");
	private final Entity e1 = Entity.newBuilder(this.key1).build();
	private final Entity e2 = Entity.newBuilder(this.key2).build();
	private TestEntity ob1;
	private TestEntity ob2;
	private ChildEntity childEntity1;

	@Before
	public void setup() {
		this.datastoreTemplate = new DatastoreTemplate(this.datastore,
				this.datastoreEntityConverter, new DatastoreMappingContext(),
				this.objectToKeyFactory);

		ReadWriteConversions readWriteConversions = mock(ReadWriteConversions.class);
		when(this.datastoreEntityConverter.getConversions())
				.thenReturn(readWriteConversions);

		// The readWriteConversions are only mocked for purposes of collection-conversion
		// for
		// descendants. no other conversions take place in the template.
		doAnswer(invocation -> {
			LinkedList linkedList = new LinkedList();
			for (Object object : (List) invocation.getArgument(0)) {
				linkedList.add(object);
			}
			return linkedList;
		}).when(readWriteConversions).convertOnRead(any(), any(), any());

		this.ob1 = new TestEntity();
		this.ob2 = new TestEntity();

		this.ob1.id = "value1";
		this.ob2.id = "value2";

		Entity ce1 = Entity.newBuilder(createFakeKey("key3")).build();

		Query childTestEntityQuery = Query.newEntityQueryBuilder().setKind("child_entity")
				.setFilter(PropertyFilter.hasAncestor(key1)).build();

		this.childEntity1 = new ChildEntity();
		this.childEntity1.id = "child_id";

		// mocked query results for entities and child entities.
		QueryResults childTestEntityQueryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(ce1).iterator().forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(childTestEntityQueryResults).forEachRemaining(any());

		QueryResults testEntityQueryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(e1, e2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(testEntityQueryResults).forEachRemaining(any());

		// mocking the converter to return the final objects corresponding to their
		// specific entities.
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), eq(this.e1)))
				.thenReturn(this.ob1);
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), eq(this.e2)))
				.thenReturn(this.ob2);
		when(this.datastoreEntityConverter.read(eq(ChildEntity.class), same(ce1)))
				.thenReturn(childEntity1);

		when(this.datastore.run(eq(this.testEntityQuery)))
				.thenReturn(testEntityQueryResults);
		when(this.datastore.run(eq(this.findAllTestEntityQuery)))
				.thenReturn(testEntityQueryResults);
		when(this.datastore.run(eq(childTestEntityQuery)))
				.thenReturn(childTestEntityQueryResults);

		// Because get() takes varags, there is difficulty in matching the single param
		// case using just thenReturn.
		doAnswer(invocation -> {
			Object key = invocation.getArgument(0);
			return key instanceof Key && key == this.key1
					? ImmutableList.of(this.e1).iterator()
					: null;
		}).when(this.datastore).get((Key[]) any());

		when(this.objectToKeyFactory.getKeyFromId(eq(this.key1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromId(eq(this.key2), any()))
				.thenReturn(this.key2);
		when(this.objectToKeyFactory.getKeyFromId(eq(this.badKey), any()))
				.thenReturn(this.badKey);

		when(this.objectToKeyFactory.getKeyFromObject(eq(this.ob1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromObject(eq(this.ob2), any()))
				.thenReturn(this.key2);
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
		assertThat(result.childEntities, contains(this.childEntity1));
	}

	@Test
	public void findByIdNotFoundTest() {
		when(this.datastore.get(ArgumentMatchers.<Key[]> any())).thenReturn(null);
		assertNull(
				this.datastoreTemplate.findById(createFakeKey("key0"), TestEntity.class));
	}

	@Test
	public void findAllByIdTest() {
		when(this.datastore.get(eq(this.key1), eq(this.key2)))
				.thenReturn(ImmutableList.of(this.e1, this.e2).iterator());
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
		assertTrue(this.datastoreTemplate.existsById(this.key1, TestEntity.class));
		assertFalse(this.datastoreTemplate.existsById(this.badKey, TestEntity.class));
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
		LinkedList<ChildEntity> childEntities;

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
