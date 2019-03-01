/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

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
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Query.ResultType;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.core.util.MapBuilder;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Descendants;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorField;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorValue;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.AfterDeleteEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.AfterSaveEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.BeforeDeleteEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.BeforeSaveEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.ClassTypeInformation;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Tests for the Datastore Template.
 *
 * @author Chengyuan Zhao
 */
public class DatastoreTemplateTests {

	/**
	 * used to check exception messages and types.
	 */
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

	private SimpleTestEntity simpleTestEntity = new SimpleTestEntity();
	private SimpleTestEntity simpleTestEntityNullVallues = new SimpleTestEntity();

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
		doAnswer((invocation) -> {
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

		this.childEntity1 = createChildEntity();

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
		doAnswer((invocation) -> {
			Arrays.asList(ce1).iterator().forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(childTestEntityQueryResults).forEachRemaining(any());

		QueryResults testEntityQueryResults = mock(QueryResults.class);
		doAnswer((invocation) -> {
			Arrays.asList(this.e1, this.e2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(testEntityQueryResults).forEachRemaining(any());
		setUpConverters(ce1, childTestEntityQuery, childTestEntityQueryResults, testEntityQueryResults);
	}

	private ChildEntity createChildEntity() {
		ChildEntity e = new ChildEntity();
		e.id = createFakeKey("key3");
		return e;
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
				.thenAnswer(invocationOnMock -> createChildEntity());

		doAnswer((invocation) -> {
			FullEntity.Builder builder = invocation.getArgument(1);
			builder.set("id", "simple_test_entity");
			builder.set("int_field", 1);
			return null;
		}).when(this.datastoreEntityConverter).write(same(this.simpleTestEntity), any());

		doAnswer((invocation) -> {
			FullEntity.Builder builder = invocation.getArgument(1);
			builder.set("id", NullValue.of());
			builder.set("int_field", NullValue.of());
			return null;
		}).when(this.datastoreEntityConverter).write(same(this.simpleTestEntityNullVallues), any());

		when(this.datastore.run(eq(this.testEntityQuery)))
				.thenReturn(testEntityQueryResults);
		when(this.datastore.run(eq(this.findAllTestEntityQuery)))
				.thenReturn(testEntityQueryResults);
		when(this.datastore.run(eq(childTestEntityQuery)))
				.thenReturn(childTestEntityQueryResults);

		// Because get() takes varags, there is difficulty in matching the single param
		// case using just thenReturn.
		doAnswer((invocation) -> {
			Object key = invocation.getArgument(0);
			List<Entity> result = new ArrayList<>();
			if (key instanceof Key) {
				if (key == this.key1) {
					result.add(this.e1);
				}
				else if (key == this.keyChild1) {
					result.add(ce1);
				}
				else {
					result.add(null);
				}
			}
			return result;
		}).when(this.datastore).fetch((Key[]) any());

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

		when(this.datastore.runInTransaction(any())).thenAnswer((invocation) -> {
			TransactionCallable<String> callable = invocation.getArgument(0);
			return callable.run(transactionContext);
		});

		List<Entity> e1 = Collections
				.singletonList(this.e1);
		when(transactionContext.fetch(ArgumentMatchers.<Key[]>any())).thenReturn(e1);

		String finalResult = this.datastoreTemplate
				.performTransaction((datastoreOperations) -> {
					datastoreOperations.save(this.ob2);
					datastoreOperations.findById("ignored", TestEntity.class);
					return "all done";
				});

		assertThat(finalResult).isEqualTo("all done");
		verify(transactionContext, times(1)).put(ArgumentMatchers.<FullEntity[]>any());
		verify(transactionContext, times(2)).fetch((Key[]) any());
	}

	@Test
	public void findAllByIdTestNotNull() {
		assertThat(
				this.datastoreTemplate.findAllById(Collections.singletonList(this.badKey), TestEntity.class)).isEmpty();
	}

	@Test
	public void findByIdTest() {
		TestEntity result = this.datastoreTemplate.findById(this.key1, TestEntity.class);
		assertThat(result).isEqualTo(this.ob1);
		assertThat(result.childEntities).contains(this.childEntity1);
		assertThat(this.childEntity1).isEqualTo(result.singularReference);
		assertThat(result.multipleReference).contains(this.childEntity1);
	}

	@Test
	public void findByIdNotFoundTest() {
		when(this.datastore.fetch(ArgumentMatchers.<Key[]>any())).thenReturn(Collections.singletonList(null));
		assertThat(this.datastoreTemplate.findById(createFakeKey("key0"), TestEntity.class)).isNull();
	}

	@Test
	public void findAllByIdTest() {
		when(this.datastore.fetch(eq(this.key1), eq(this.key2)))
				.thenReturn(Arrays.asList(this.e1, this.e2));
		List<Key> keys = Arrays.asList(this.key1, this.key2);
		assertThat(this.datastoreTemplate.findAllById(keys, TestEntity.class)).containsExactly(this.ob1, this.ob2);
	}

	@Test
	public void findAllByIdReferenceConsistencyTest() {
		when(this.objectToKeyFactory.getKeyFromObject(eq(this.childEntity1), any())).thenReturn(this.childEntity1.id);

		when(this.datastore.fetch(eq(this.key1)))
				.thenReturn(Arrays.asList(this.e1));

		TestEntity parentEntity1 = this.datastoreTemplate.findById(this.key1, TestEntity.class);
		assertThat(parentEntity1).isSameAs(this.ob1);
		ChildEntity singularReference1 = parentEntity1.singularReference;
		ChildEntity childEntity1 = parentEntity1.childEntities.get(0);
		assertThat(singularReference1).isSameAs(childEntity1);

		TestEntity parentEntity2 = this.datastoreTemplate.findById(this.key1, TestEntity.class);
		assertThat(parentEntity2).isSameAs(this.ob1);
		ChildEntity singularReference2 = parentEntity2.singularReference;
		ChildEntity childEntity2 = parentEntity2.childEntities.get(0);
		assertThat(singularReference2).isSameAs(childEntity2);

		assertThat(childEntity1).isNotSameAs(childEntity2);
	}

	@Test
	public void findAllReferenceLoopTest() {


		Entity referenceTestDatastoreEntity = Entity.newBuilder(this.key1)
				.set("sibling", this.key1)
				.build();

		when(this.datastore.fetch(eq(this.key1)))
				.thenReturn(Arrays.asList(referenceTestDatastoreEntity));

		when(this.datastoreEntityConverter.read(eq(ReferenceTestEntity.class), same(referenceTestDatastoreEntity)))
				.thenAnswer(invocationOnMock -> new ReferenceTestEntity());

		ReferenceTestEntity readReferenceTestEntity = this.datastoreTemplate.findById(this.key1, ReferenceTestEntity.class);

		assertThat(readReferenceTestEntity.sibling).isSameAs(readReferenceTestEntity);
	}

	@Test
	public void saveReferenceLoopTest() {
		ReferenceTestEntity referenceTestEntity = new ReferenceTestEntity();
		referenceTestEntity.id = 1L;
		referenceTestEntity.sibling = referenceTestEntity;
		when(this.objectToKeyFactory.getKeyFromObject(eq(referenceTestEntity), any())).thenReturn(this.key1);

		List<Object[]> callsArgs = gatherVarArgCallsArgs(this.datastore.put(ArgumentMatchers.<FullEntity[]>any()),
				Collections.singletonList(this.e1));

		assertThat(this.datastoreTemplate.save(referenceTestEntity) instanceof ReferenceTestEntity).isTrue();

		Entity writtenEntity = Entity.newBuilder(this.key1)
				.set("sibling", this.key1)
				.build();

		assertArgs(callsArgs, new MapBuilder<List, Integer>()
				.put(Arrays.asList(writtenEntity), 1)
				.buildModifiable());
	}

	@Test
	public void saveTest() {
		Entity writtenEntity = Entity.newBuilder(this.key1)
				.set("singularReference", this.childKey4)
				.set("multipleReference", Arrays.asList(KeyValue.of(this.childKey5), KeyValue.of(this.childKey6)))
				.build();

		Entity writtenChildEntity2 = Entity.newBuilder(this.childKey2).build();
		Entity writtenChildEntity3 = Entity.newBuilder(this.childKey3).build();
		Entity writtenChildEntity4 = Entity.newBuilder(this.childKey4).build();
		Entity writtenChildEntity5 = Entity.newBuilder(this.childKey5).build();
		Entity writtenChildEntity6 = Entity.newBuilder(this.childKey6).build();

		doAnswer(invocation -> {
			assertThat(invocation.getArguments()).containsExactlyInAnyOrder(writtenChildEntity2, writtenChildEntity3,
					writtenChildEntity4, writtenChildEntity5, writtenChildEntity6, writtenEntity);
			return null;
		}).when(this.datastore).put(ArgumentMatchers.<FullEntity[]>any());

		assertThat(this.datastoreTemplate.save(this.ob1) instanceof TestEntity).isTrue();
		verify(this.datastore, times(1)).put(ArgumentMatchers.<FullEntity[]>any());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity2), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity3), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity4), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity5), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity6), notNull());
	}

	private List<Object[]> gatherVarArgCallsArgs(Object methodCall, List<Entity> returnVal) {
		List<Object[]> callsArgs = new ArrayList<>();
		when(methodCall).thenAnswer(invocationOnMock -> {
			callsArgs.add(invocationOnMock.getArguments());
			return returnVal;
		});
		return callsArgs;
	}

	private void assertArgs(List<Object[]> callsArgs, Map<List, Integer> expected) {
		for (Object[] args : callsArgs) {
			List<Object> key = Arrays.asList(args);
			expected.put(key, expected.computeIfAbsent(key, k -> 0) - 1);
		}
		expected.forEach((key, value) -> assertThat(value).as("Extra calls with argument " + key).isEqualTo(0));
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
		assertThat(this.ob2.childEntities).isNull();
		assertThat(this.ob2.singularReference).isNull();
		assertThat(this.ob2.multipleReference).isNull();


		List<Object[]> callsArgs = gatherVarArgCallsArgs(this.datastore.put(ArgumentMatchers.<FullEntity[]>any()),
				Collections.singletonList(this.e1));

		this.datastoreTemplate.save(this.ob2);

		assertArgs(callsArgs, new MapBuilder<List, Integer>()
				.put(Arrays.asList(Entity.newBuilder(this.key2).build()), 1).buildModifiable());

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
		List<Object[]> callsArgs = gatherVarArgCallsArgs(this.datastore.put(ArgumentMatchers.<FullEntity[]>any()), Collections.singletonList(this.e1));

		this.datastoreTemplate.save(childEntity, key0);

		Entity writtenChildEntity = Entity.newBuilder(keyA).build();

		assertArgs(callsArgs, new MapBuilder<List, Integer>()
				.put(Arrays.asList(writtenChildEntity), 1)
				.buildModifiable());
	}

	@Test
	public void saveAndAllocateIdTest() {
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.ob1), any()))
				.thenReturn(this.key1);
		Entity writtenEntity1 = Entity.newBuilder(this.key1)
				.set("singularReference", this.childKey4)
				.set("multipleReference", Arrays.asList(KeyValue.of(this.childKey5), KeyValue.of(this.childKey6)))
				.build();
		Entity writtenChildEntity2 = Entity.newBuilder(this.childKey2).build();
		Entity writtenChildEntity3 = Entity.newBuilder(this.childKey3).build();
		Entity writtenChildEntity4 = Entity.newBuilder(this.childKey4).build();
		Entity writtenChildEntity5 = Entity.newBuilder(this.childKey5).build();
		Entity writtenChildEntity6 = Entity.newBuilder(this.childKey6).build();
		doAnswer(invocation -> {
			assertThat(invocation.getArguments()).containsExactlyInAnyOrder(writtenChildEntity2, writtenChildEntity3,
					writtenChildEntity4, writtenChildEntity5, writtenChildEntity6, writtenEntity1);
			return null;
		}).when(this.datastore).put(ArgumentMatchers.<FullEntity[]>any());

		assertThat(this.datastoreTemplate.save(this.ob1) instanceof TestEntity).isTrue();

		verify(this.datastore, times(1)).put(ArgumentMatchers.<FullEntity[]>any());

		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
	}

	@Test
	public void saveAllTest() {
		when(this.objectToKeyFactory.allocateKeyForObject(same(this.ob1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromObject(same(this.ob2), any()))
				.thenReturn(this.key2);
		Entity writtenEntity1 = Entity.newBuilder(this.key1)
				.set("singularReference", this.childKey4)
				.set("multipleReference", Arrays.asList(KeyValue.of(this.childKey5), KeyValue.of(this.childKey6)))
				.build();
		Entity writtenEntity2 = Entity.newBuilder(this.key2).build();
		Entity writtenChildEntity2 = Entity.newBuilder(this.childKey2).build();
		Entity writtenChildEntity3 = Entity.newBuilder(this.childKey3).build();
		Entity writtenChildEntity4 = Entity.newBuilder(this.childKey4).build();
		Entity writtenChildEntity5 = Entity.newBuilder(this.childKey5).build();
		Entity writtenChildEntity6 = Entity.newBuilder(this.childKey6).build();
		doAnswer(invocation -> {
			assertThat(invocation.getArguments()).containsExactlyInAnyOrder(writtenChildEntity2, writtenChildEntity3,
					writtenChildEntity4, writtenChildEntity5, writtenChildEntity6, writtenEntity1, writtenEntity2);
			return null;
		}).when(this.datastore).put(ArgumentMatchers.<FullEntity[]>any());

		List<Entity> expected = Arrays.asList(writtenChildEntity2, writtenChildEntity3,
				writtenChildEntity4, writtenChildEntity5, writtenChildEntity6, writtenEntity1, writtenEntity2);
		List javaExpected = Arrays.asList(this.ob1, this.ob2);

		verifyBeforeAndAfterEvents(new BeforeSaveEvent(expected, javaExpected),
				new AfterSaveEvent(expected, javaExpected),
				() -> this.datastoreTemplate.saveAll(Arrays.asList(this.ob1, this.ob2)),
				x -> {
				});

		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob1), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.ob2), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity2), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity3), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity4), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity5), notNull());
		verify(this.datastoreEntityConverter, times(1)).write(same(this.childEntity6), notNull());
		verify(this.datastore, times(1)).put(ArgumentMatchers.<FullEntity[]>any());
	}

	@Test
	public void findAllTest() {
		this.datastoreTemplate.findAll(TestEntity.class);
		assertThat(this.datastoreTemplate.findAll(TestEntity.class)).contains(this.ob1, this.ob2);
	}

	@Test
	public void queryTest() {
		assertThat(this.datastoreTemplate.query((Query<Entity>) this.testEntityQuery, TestEntity.class))
				.contains(this.ob1, this.ob2);
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
		doAnswer((invocation) -> {
			Arrays.asList(this.key1, this.key2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(queryResults).forEachRemaining(any());
		when(this.datastore
				.run(eq(Query.newKeyQueryBuilder().setKind("custom_test_kind").build())))
						.thenReturn(queryResults);
		assertThat(this.datastoreTemplate.count(TestEntity.class)).isEqualTo(2);
	}

	@Test
	public void existsByIdTest() {
		assertThat(this.datastoreTemplate.existsById(this.key1, TestEntity.class)).isTrue();
		assertThat(this.datastoreTemplate.existsById(this.badKey, TestEntity.class)).isFalse();
	}

	@Test
	public void deleteByIdTest() {
		when(this.objectToKeyFactory.getKeyFromId(same(this.key1), any()))
				.thenReturn(this.key1);

		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1 }, TestEntity.class, Collections.singletonList(this.key1),
						null),
				new AfterDeleteEvent(new Key[] { this.key1 }, TestEntity.class, Collections.singletonList(this.key1),
						null),
				() -> this.datastoreTemplate.deleteById(this.key1, TestEntity.class),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key1)));
	}

	@Test
	public void deleteAllByIdTest() {
		when(this.objectToKeyFactory.getKeyFromId(same(this.key1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromId(same(this.key2), any()))
				.thenReturn(this.key2);

		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1, this.key2 }, TestEntity.class,
						Arrays.asList(this.key1, this.key2), null),
				new AfterDeleteEvent(new Key[] { this.key1, this.key2 }, TestEntity.class,
						Arrays.asList(this.key1, this.key2), null),
				() -> this.datastoreTemplate.deleteAllById(Arrays.asList(this.key1, this.key2),
						TestEntity.class),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key1), same(this.key2)));
	}

	@Test
	public void deleteObjectTest() {
		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1 }, TestEntity.class, null, Arrays.asList(this.ob1)),
				new AfterDeleteEvent(new Key[] { this.key1 }, TestEntity.class, null, Arrays.asList(this.ob1)),
				() -> this.datastoreTemplate.delete(this.ob1),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key1)));
	}

	@Test
	public void deleteMultipleObjectsTest() {
		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1, this.key2 }, null, null,
						Arrays.asList(this.ob1, this.ob2)),
				new AfterDeleteEvent(new Key[] { this.key1, this.key2 }, null, null, Arrays.asList(this.ob1, this.ob2)),
				() -> this.datastoreTemplate.deleteAll(Arrays.asList(this.ob1, this.ob2)),
				x -> x.verify(this.datastore, times(1)).delete(eq(this.key1), eq(this.key2)));
	}

	@Test
	public void deleteAllTest() {
		QueryResults<Key> queryResults = mock(QueryResults.class);
		when(queryResults.getResultClass()).thenReturn((Class) Key.class);
		doAnswer((invocation) -> {
			Arrays.asList(this.key1, this.key2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(queryResults).forEachRemaining(any());
		when(this.datastore
				.run(eq(Query.newKeyQueryBuilder().setKind("custom_test_kind").build())))
						.thenReturn(queryResults);

		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1, this.key2 }, TestEntity.class, null, null),
				new AfterDeleteEvent(new Key[] { this.key1, this.key2 }, TestEntity.class, null, null),
				() -> assertThat(this.datastoreTemplate.deleteAll(TestEntity.class)).isEqualTo(2),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key1), same(this.key2)));
	}

	private void verifyBeforeAndAfterEvents(ApplicationEvent expectedBefore,
			ApplicationEvent expectedAfter, Runnable operation, Consumer<InOrder> verifyOperation) {
		ApplicationEventPublisher mockPublisher = mock(ApplicationEventPublisher.class);
		ApplicationEventPublisher mockBeforePublisher = mock(ApplicationEventPublisher.class);
		ApplicationEventPublisher mockAfterPublisher = mock(ApplicationEventPublisher.class);

		InOrder inOrder = Mockito.inOrder(mockBeforePublisher, this.datastore, mockAfterPublisher);

		doAnswer((invocationOnMock) -> {
			ApplicationEvent event = invocationOnMock.getArgument(0);
			if (expectedBefore != null && event.getClass().equals(expectedBefore.getClass())) {
				mockBeforePublisher.publishEvent(event);
			}
			else if (expectedAfter != null && event.getClass().equals(expectedAfter.getClass())) {
				mockAfterPublisher.publishEvent(event);
			}
			return null;
		}).when(mockPublisher).publishEvent(any());

		this.datastoreTemplate.setApplicationEventPublisher(mockPublisher);

		operation.run();
		if (expectedBefore != null) {
			inOrder.verify(mockBeforePublisher, times(1)).publishEvent(eq(expectedBefore));
		}
		verifyOperation.accept(inOrder);
		if (expectedAfter != null) {
			inOrder.verify(mockAfterPublisher, times(1)).publishEvent(eq(expectedAfter));
		}
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
	public void findAllDiscrimination() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind");

		this.datastoreTemplate.findAll(SimpleDiscriminationTestEntity.class);
		verify(this.datastore, times(1)).run(builder.setFilter(PropertyFilter.eq("discrimination_field", "A")).build());
	}

	@Test
	public void combineFiltersDiscrimination() {
		PropertyFilter propertyFilter = PropertyFilter.eq("field", "some value");
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind")
				.setFilter(propertyFilter);
		DatastoreTemplate.applyQueryOptions(builder, new DatastoreQueryOptions(1, 2, null),
				new DatastoreMappingContext().getPersistentEntity(SimpleDiscriminationTestEntity.class));

		assertThat(builder.build().getFilter()).isEqualTo(
				StructuredQuery.CompositeFilter.and(propertyFilter, PropertyFilter.eq("discrimination_field", "A")));

		assertThat(builder.build().getLimit()).isEqualTo(1);
		assertThat(builder.build().getOffset()).isEqualTo(2);
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
	public void queryByExampleSimpleEntityTest() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind");
		this.datastoreTemplate.queryByExample(Example.of(this.simpleTestEntity), null);

		StructuredQuery.CompositeFilter filter = StructuredQuery.CompositeFilter
				.and(PropertyFilter.eq("id", "simple_test_entity"),
						PropertyFilter.eq("int_field", 1));
		verify(this.datastore, times(1)).run(builder.setFilter(filter).build());
	}

	@Test
	public void queryByExampleIgnoreFieldTest() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind");
		this.datastoreTemplate.queryByExample(
				Example.of(this.simpleTestEntity, ExampleMatcher.matching().withIgnorePaths("intField")), null);

		StructuredQuery.CompositeFilter filter = StructuredQuery.CompositeFilter
				.and(PropertyFilter.eq("id", "simple_test_entity"));
		verify(this.datastore, times(1)).run(builder.setFilter(filter).build());
	}

	@Test
	public void queryByExampleDeepPathTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Ignored paths deeper than 1 are not supported");

		this.datastoreTemplate.queryByExample(
				Example.of(new SimpleTestEntity(), ExampleMatcher.matching().withIgnorePaths("intField.a")), null);
	}

	@Test
	public void queryByExampleIncludeNullValuesTest() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind");
		this.datastoreTemplate.queryByExample(
				Example.of(this.simpleTestEntityNullVallues, ExampleMatcher.matching().withIncludeNullValues()), null);

		StructuredQuery.CompositeFilter filter = StructuredQuery.CompositeFilter
				.and(PropertyFilter.eq("id", NullValue.of()),
						PropertyFilter.eq("int_field", NullValue.of()));
		verify(this.datastore, times(1)).run(builder.setFilter(filter).build());
	}

	@Test
	public void queryByExampleNoNullValuesTest() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind");
		this.datastoreTemplate.queryByExample(
				Example.of(this.simpleTestEntityNullVallues), null);

		verify(this.datastore, times(1)).run(builder.build());
	}
	@Test
	public void queryByExampleExactMatchTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Unsupported StringMatcher. Only EXACT and DEFAULT are supported");

		this.datastoreTemplate.queryByExample(
				Example.of(new SimpleTestEntity(), ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.REGEX)), null);
	}

	@Test
	public void queryByExampleIgnoreCaseTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Ignore case matching is not supported");

		this.datastoreTemplate.queryByExample(
				Example.of(new SimpleTestEntity(), ExampleMatcher.matching().withIgnoreCase()), null);
	}


	@Test
	public void queryByExampleAllMatchTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Unsupported MatchMode. Only MatchMode.ALL is supported");

		this.datastoreTemplate.queryByExample(
				Example.of(new SimpleTestEntity(), ExampleMatcher.matchingAny()), null);
	}


	@Test
	public void queryByExamplePropertyMatchersTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Property matchers are not supported");

		this.datastoreTemplate.queryByExample(
				Example.of(new SimpleTestEntity(), ExampleMatcher.matching().withMatcher("id",
						ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.REGEX))),
				null);
	}

	@Test
	public void queryByExampleCaseSensitiveTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Property matchers are not supported");

		this.datastoreTemplate.queryByExample(
				Example.of(new SimpleTestEntity(), ExampleMatcher.matching().withMatcher("id",
						ExampleMatcher.GenericPropertyMatcher::caseSensitive)),
				null);
	}

	@Test
	public void queryByExampleNullTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null example is expected");

		this.datastoreTemplate.queryByExample(null, null);
	}

	@Test
	public void queryByExampleOptions() {
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind");
		this.datastoreTemplate.queryByExample(Example.of(this.simpleTestEntity),
				new DatastoreQueryOptions(10, 1, Sort.by("intField")));

		StructuredQuery.CompositeFilter filter = StructuredQuery.CompositeFilter
				.and(PropertyFilter.eq("id", "simple_test_entity"),
						PropertyFilter.eq("int_field", 1));
		verify(this.datastore, times(1)).run(builder.setFilter(filter)
				.addOrderBy(StructuredQuery.OrderBy.asc("int_field")).setLimit(10).setOffset(1).build());
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

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "test_kind")
	private static class SimpleTestEntity {
		@Id
		String id;

		@Field(name = "int_field")
		int intField;
	}

	class ReferenceTestEntity {
		@Id
		Long id;

		@Reference
		ReferenceTestEntity sibling;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "test_kind")
	@DiscriminatorField(field = "discrimination_field")
	@DiscriminatorValue("A")
	private static class SimpleDiscriminationTestEntity {
		@Id
		String id;

		@Field(name = "int_field")
		int intField;
	}

}
