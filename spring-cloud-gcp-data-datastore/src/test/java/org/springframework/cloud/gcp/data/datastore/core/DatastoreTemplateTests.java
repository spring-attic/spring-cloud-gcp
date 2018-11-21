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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Datastore.TransactionCallable;
import com.google.cloud.datastore.DatastoreReaderWriter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Query.ResultType;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Descendants;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Reference;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.ClassTypeInformation;

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

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private final Datastore datastore = mock(Datastore.class);

	private final DatastoreEntityConverter datastoreEntityConverter = mock(
			DatastoreEntityConverter.class);

	private final ObjectToKeyFactory objectToKeyFactory = mock(ObjectToKeyFactory.class);

	private DatastoreTemplate datastoreTemplate;

	private ChildEntity childEntity2;

	private ChildEntity childEntity3;

	private ChildEntity childEntity4;

	private ChildEntity childEntity5;

	private ChildEntity childEntity6;

	private Key childKey2;

	private Key childKey3;

	private Key childKey4;

	private Key childKey5;

	private Key childKey6;

	private final ReadWriteConversions readWriteConversions = mock(ReadWriteConversions.class);

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

	private final Key keyChild1 = createFakeKey("key3");

	private final Key badKey = createFakeKey("badkey");

	private final Entity e1 = Entity.newBuilder(this.key1)
			.set("singularReference", this.keyChild1)
			.set("multipleReference", Collections.singletonList(KeyValue.of(this.keyChild1)))
			.build();

	private final Entity e2 = Entity.newBuilder(this.key2)
			.set("singularReference", this.keyChild1)
			.set("multipleReference", Collections.singletonList(KeyValue.of(this.keyChild1)))
			.build();

	private TestEntity ob1;

	private TestEntity ob2;

	private ChildEntity childEntity1;

	@Before
	public void setup() {
		this.datastoreTemplate = new DatastoreTemplate(this.datastore,
				this.datastoreEntityConverter, new DatastoreMappingContext(),
				this.objectToKeyFactory);

		when(this.datastoreEntityConverter.getConversions())
				.thenReturn(this.readWriteConversions);

		// The readWriteConversions are only mocked for purposes of collection-conversion
		// for
		// descendants. no other conversions take place in the template.
		doAnswer(invocation -> {
			LinkedList linkedList = new LinkedList();
			for (Object object : (List) invocation.getArgument(0)) {
				linkedList.add(object);
			}
			return linkedList;
		}).when(this.readWriteConversions).convertOnRead(any(), any(), (Class) any());

		this.ob1 = new TestEntity();
		this.ob2 = new TestEntity();

		this.ob1.id = "value1";
		this.ob2.id = "value2";

		Entity ce1 = Entity.newBuilder(this.keyChild1).build();

		Query childTestEntityQuery = Query.newEntityQueryBuilder().setKind("child_entity")
				.setFilter(PropertyFilter.hasAncestor(this.key1)).build();

		this.childEntity1 = new ChildEntity();
		this.childEntity1.id = createFakeKey("child_id");

		this.ob1.childEntities = new LinkedList<>();
		this.childEntity2 = new ChildEntity();
		this.ob1.childEntities.add(this.childEntity2);

		this.childEntity3 = new ChildEntity();
		this.ob1.childEntities.add(this.childEntity3);

		this.childEntity4 = new ChildEntity();
		this.ob1.singularReference = this.childEntity4;

		this.ob1.multipleReference = new LinkedList<>();
		this.childEntity5 = new ChildEntity();
		this.ob1.multipleReference.add(this.childEntity5);

		this.childEntity6 = new ChildEntity();
		this.ob1.multipleReference.add(this.childEntity6);


		// mocked query results for entities and child entities.
		QueryResults childTestEntityQueryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(ce1).iterator().forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(childTestEntityQueryResults).forEachRemaining(any());

		QueryResults testEntityQueryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(this.e1, this.e2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(testEntityQueryResults).forEachRemaining(any());
		setUpConverters(ce1, childTestEntityQuery, childTestEntityQueryResults, testEntityQueryResults);


	}

	private void setUpConverters(Entity ce1, Query childTestEntityQuery,
			QueryResults childTestEntityQueryResults, QueryResults testEntityQueryResults) {
		// mocking the converter to return the final objects corresponding to their
		// specific entities.
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), eq(this.e1)))
				.thenReturn(this.ob1);
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), eq(this.e2)))
				.thenReturn(this.ob2);
		when(this.datastoreEntityConverter.read(eq(ChildEntity.class), same(ce1)))
				.thenReturn(this.childEntity1);

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
			Iterator<Entity> result = null;
			if (key instanceof Key) {
				if (key == this.key1) {
					result = ImmutableList.of(this.e1).iterator();
				}
				else if (key == this.keyChild1) {
					result = ImmutableList.of(ce1).iterator();
				}
			}
			return result;
		}).when(this.datastore).get((Key[]) any());

		when(this.objectToKeyFactory.getKeyFromId(eq(this.key1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromId(eq(this.key2), any()))
				.thenReturn(this.key2);
		when(this.objectToKeyFactory.getKeyFromId(eq(this.keyChild1), any()))
				.thenReturn(this.keyChild1);
		when(this.objectToKeyFactory.getKeyFromId(eq(this.badKey), any()))
				.thenReturn(this.badKey);

		when(this.objectToKeyFactory.getKeyFromObject(eq(this.ob1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromObject(eq(this.ob2), any()))
				.thenReturn(this.key2);
		this.childKey2 = createFakeKey("child_id2");
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.childEntity2), any(), eq(this.key1)))
				.thenReturn(this.childKey2);
		this.childKey3 = createFakeKey("child_id3");
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.childEntity3), any(), eq(this.key1)))
				.thenReturn(this.childKey3);
		this.childKey4 = createFakeKey("child_id4");
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.childEntity4), any(), any()))
				.thenReturn(this.childKey4);
		when(this.objectToKeyFactory.getKeyFromObject(same(this.childEntity4), any()))
				.thenReturn(this.childKey4);
		this.childKey5 = createFakeKey("child_id5");
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.childEntity5), any(), any()))
				.thenReturn(this.childKey5);
		when(this.objectToKeyFactory.getKeyFromObject(same(this.childEntity5), any()))
				.thenReturn(this.childKey5);
		this.childKey6 = createFakeKey("child_id6");
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.childEntity6), any(), any()))
				.thenReturn(this.childKey6);
		when(this.objectToKeyFactory.getKeyFromObject(same(this.childEntity6), any()))
				.thenReturn(this.childKey6);
	}

	@Test
	public void performTransactionTest() {

		DatastoreReaderWriter transactionContext = mock(DatastoreReaderWriter.class);

		when(this.datastore.runInTransaction(any())).thenAnswer(invocation -> {
			TransactionCallable<String> callable = invocation.getArgument(0);
			return callable.run(transactionContext);
		});

		Iterator<Entity> e1 = Collections
				.singletonList(this.e1)
				.iterator();
		when(transactionContext.get(ArgumentMatchers.<Key[]>any())).thenReturn(e1);

		String finalResult = this.datastoreTemplate
				.performTransaction(datastoreOperations -> {
					datastoreOperations.save(this.ob2);
					datastoreOperations.findById("ignored", TestEntity.class);
					return "all done";
				});

		assertEquals("all done", finalResult);
		verify(transactionContext, times(1)).put((FullEntity<?>) any());
		verify(transactionContext, times(3)).get((Key[]) any());
	}

	@Test
	public void findAllByIdTestNotNull() {
		assertTrue(this.datastoreTemplate
				.findAllById(Collections.singletonList(this.badKey), TestEntity.class)
				.isEmpty());
	}

	@Test
	public void findByIdTest() {
		TestEntity result = this.datastoreTemplate.findById(this.key1, TestEntity.class);
		assertEquals(this.ob1, result);
		assertThat(result.childEntities, contains(this.childEntity1));
		assertEquals(result.singularReference, this.childEntity1);
		assertThat(result.multipleReference, contains(this.childEntity1));
	}

	@Test
	public void findByIdNotFoundTest() {
		when(this.datastore.get(ArgumentMatchers.<Key[]>any())).thenReturn(null);
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

		Entity writtenEntity = Entity.newBuilder(this.key1)
				.set("singularReference", this.childKey4)
				.set("multipleReference", Arrays.asList(KeyValue.of(this.childKey5), KeyValue.of(this.childKey6)))
				.build();
		verify(this.datastore, times(1)).put(eq(writtenEntity));

		Entity writtenChildEntity4 = Entity.newBuilder(this.childKey4).build();
		verify(this.datastore, times(1)).put(eq(writtenChildEntity4));

		Entity writtenChildEntity2 = Entity.newBuilder(this.childKey2).build();
		Entity writtenChildEntity3 = Entity.newBuilder(this.childKey3).build();
		Entity writtenChildEntity5 = Entity.newBuilder(this.childKey5).build();
		Entity writtenChildEntity6 = Entity.newBuilder(this.childKey6).build();
		verify(this.datastore, times(1)).put(eq(writtenChildEntity2), eq(writtenChildEntity3));
		verify(this.datastore, times(1)).put(eq(writtenChildEntity5), eq(writtenChildEntity6));

		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity2), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity3), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity4), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity5), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity6), notNull());
	}


	@Test
	public void saveTestNonKeyId() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Only Key types are allowed for descendants id");

		this.datastoreTemplate.save(this.ob1, createFakeKey("key0"));
	}

	@Test
	public void saveTestNullDescendantsAndReferences() {
		//making sure save works when descendants are null
		assertNull(this.ob2.childEntities);
		assertNull(this.ob2.singularReference);
		assertNull(this.ob2.multipleReference);

		this.datastoreTemplate.save(this.ob2);
	}

	@Test
	public void saveTestKeyNoAncestor() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Descendant object has a key without current ancestor");

		when(this.objectToKeyFactory.getKeyFromObject(eq(this.childEntity1), any())).thenReturn(this.childEntity1.id);
		this.datastoreTemplate.save(this.childEntity1, createFakeKey("key0"));
	}

	@Test
	public void saveTestKeyWithAncestor() {
		Key key0 = createFakeKey("key0");
		Key keyA = Key.newBuilder(key0)
				.addAncestor(PathElement.of(key0.getKind(), key0.getName())).setName("keyA").build();
		ChildEntity childEntity = new ChildEntity();
		childEntity.id = keyA;
		when(this.objectToKeyFactory.getKeyFromObject(eq(childEntity), any())).thenReturn(keyA);
		this.datastoreTemplate.save(childEntity, key0);

		Entity writtenChildEntity = Entity.newBuilder(keyA).build();
		verify(this.datastore, times(1)).put(eq(writtenChildEntity));
	}

	@Test
	public void saveAndAllocateIdTest() {
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.ob1), any()))
				.thenReturn(this.key1);
		when(this.datastore.put((FullEntity<?>) any())).thenReturn(this.e1);
		assertTrue(this.datastoreTemplate.save(this.ob1) instanceof TestEntity);
		Entity writtenEntity1 = Entity.newBuilder(this.key1)
				.set("singularReference", this.childKey4)
				.set("multipleReference", Arrays.asList(KeyValue.of(this.childKey5), KeyValue.of(this.childKey6)))
				.build();
		verify(this.datastore, times(1)).put(eq(writtenEntity1));
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
	}

	@Test
	public void saveAllTest() {
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.ob1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromObject(same(this.ob2), any()))
				.thenReturn(this.key2);

		when(this.datastore.put(any(), any()))
				.thenReturn(ImmutableList.of(this.e1, this.e2));

		this.datastoreTemplate.saveAll(ImmutableList.of(this.ob1, this.ob2));
		Entity writtenEntity1 = Entity.newBuilder(this.key1)
				.set("singularReference", this.childKey4)
				.set("multipleReference", Arrays.asList(KeyValue.of(this.childKey5), KeyValue.of(this.childKey6)))
				.build();
		Entity writtenEntity2 = Entity.newBuilder(this.key2).build();
		verify(this.datastore, times(1)).put(eq(writtenEntity1), eq(writtenEntity2));
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob2), notNull());

		Entity writtenChildEntity2 = Entity.newBuilder(this.childKey2).build();
		Entity writtenChildEntity3 = Entity.newBuilder(this.childKey3).build();
		verify(this.datastore, times(1)).put(eq(writtenChildEntity2), eq(writtenChildEntity3));

		Entity writtenChildEntity4 = Entity.newBuilder(this.childKey4).build();
		verify(this.datastore, times(1)).put(eq(writtenChildEntity4));


		Entity writtenChildEntity5 = Entity.newBuilder(this.childKey5).build();
		Entity writtenChildEntity6 = Entity.newBuilder(this.childKey6).build();
		verify(this.datastore, times(1)).put(eq(writtenChildEntity2), eq(writtenChildEntity3));
		verify(this.datastore, times(1)).put(eq(writtenChildEntity5), eq(writtenChildEntity6));

		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity2), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity3), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity4), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity5), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity6), notNull());
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
		when(queryResults.getResultClass()).thenReturn((Class) Key.class);
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
		verify(this.datastore, times(1)).delete(eq(this.key1), eq(this.key2));
	}

	@Test
	public void deleteAllTest() {
		QueryResults<Key> queryResults = mock(QueryResults.class);
		when(queryResults.getResultClass()).thenReturn((Class) Key.class);
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


	@Test
	public void findAllTestLimitOffset() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("custom_test_kind");

		this.datastoreTemplate.findAll(TestEntity.class,
				new DatastoreQueryOptions(1, 5, null));
		verify(this.datastore, times(1)).run(builder.setLimit(1).setOffset(5).build());

		this.datastoreTemplate.findAll(TestEntity.class,
				new DatastoreQueryOptions(null, null, null));
		verify(this.datastore, times(1)).run(builder.build());
	}

	@Test
	public void findAllTestSort() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("custom_test_kind");

		this.datastoreTemplate.findAll(TestEntity.class,
				new DatastoreQueryOptions(null, null, new Sort(Sort.Direction.ASC, "sortProperty")));
		verify(this.datastore, times(1)).run(
				builder.setOrderBy(
						new StructuredQuery.OrderBy("prop", StructuredQuery.OrderBy.Direction.ASCENDING)).build());

		this.datastoreTemplate.findAll(TestEntity.class,
				new DatastoreQueryOptions(null, null, new Sort(Sort.Direction.DESC, "sortProperty")));
		verify(this.datastore, times(1)).run(
				builder.setOrderBy(
						new StructuredQuery.OrderBy("prop", StructuredQuery.OrderBy.Direction.DESCENDING)).build());
	}

	@Test
	public void findAllTestSortLimitOffset() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("custom_test_kind");

		this.datastoreTemplate.findAll(TestEntity.class,
				new DatastoreQueryOptions(2, 3, new Sort(Sort.Direction.ASC, "sortProperty")));
		verify(this.datastore, times(1)).run(
				builder.setLimit(2).setOffset(3)
						.setOrderBy(
						new StructuredQuery.OrderBy("prop", StructuredQuery.OrderBy.Direction.ASCENDING)).build());
	}

	@Test
	public void writeMapTest() {
		Map<String, Long> map = new HashMap<>();
		map.put("field1", 1L);
		Key keyForMap = createFakeKey("map1");

		when(this.readWriteConversions.convertOnWriteSingle(eq(1L))).thenReturn(LongValue.of(1L));

		this.datastoreTemplate.writeMap(keyForMap, map);

		Entity datastoreEntity = Entity.newBuilder(keyForMap).set("field1", 1L).build();
		verify(this.datastore, times(1)).put(eq(datastoreEntity));
	}

	@Test
	public void findByIdAsMapTest() {
		Key keyForMap = createFakeKey("map1");

		Entity datastoreEntity = Entity.newBuilder(keyForMap).set("field1", 1L).build();
		when(this.datastore.get(eq(keyForMap))).thenReturn(datastoreEntity);

		this.datastoreTemplate.findByIdAsMap(keyForMap, Long.class);
		verify(this.datastoreEntityConverter, times(1))
				.readAsMap(eq(String.class), eq(ClassTypeInformation.from(Long.class)), eq(datastoreEntity));
	}

	@Test
	public void createKeyTest() {
		this.datastoreTemplate.createKey("kind1", 1L);
		verify(this.objectToKeyFactory, times(1)).getKeyFromId(eq(1L), eq("kind1"));
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;

		@Field(name = "prop")
		String sortProperty;

		@Descendants
		LinkedList<ChildEntity> childEntities;

		@Reference
		ChildEntity singularReference;

		@Reference
		LinkedList<ChildEntity> multipleReference;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			TestEntity that = (TestEntity) o;
			return Objects.equals(this.id, that.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.id);
		}
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "child_entity")
	private static class ChildEntity {
		@Id
		Key id;

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ChildEntity that = (ChildEntity) o;
			return Objects.equals(this.id, that.id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.id);
		}
	}
}
