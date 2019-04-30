/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery;
import com.google.datastore.v1.QueryResultBatch;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.cloud.gcp.core.util.MapBuilder;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.AfterDeleteEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.AfterFindByKeyEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.AfterQueryEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.AfterSaveEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.BeforeDeleteEvent;
import org.springframework.cloud.gcp.data.datastore.core.mapping.event.BeforeSaveEvent;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Reactive Datastore Template.
 *
 * @author Chengyuan Zhao
 */
public class ReactiveDatastoreTemplateTests extends AbstractDatastoreTemplateTests {

	private ReactiveDatastoreTemplate reactiveDatastoreTemplate;

	@Before
	public void setup() {
		super.setup();
		this.reactiveDatastoreTemplate = new ReactiveDatastoreTemplate(this.datastoreTemplate);
	}

	public void setupGenericResultSets() {
		when(this.datastore.run(any())).thenReturn(this.testEntityQueryResults);
		when(this.datastore.get((Key[]) any())).thenReturn(Arrays.asList(this.e1, this.e2).iterator());
		Iterator<Key> keys = Arrays.asList(this.key1, this.key2).iterator();

		QueryResults<Key> queryResults = new QueryResults<Key>() {
			@Override
			public Class<?> getResultClass() {
				return Key.class;
			}

			@Override
			public Cursor getCursorAfter() {
				return null;
			}

			@Override
			public int getSkippedResults() {
				return 0;
			}

			@Override
			public QueryResultBatch.MoreResultsType getMoreResults() {
				return null;
			}

			@Override
			public boolean hasNext() {
				return keys.hasNext();
			}

			@Override
			public Key next() {
				return keys.next();
			}
		};
		when(this.datastore
				.run(eq(Query.newKeyQueryBuilder().setKind("custom_test_kind").build())))
						.thenReturn(queryResults);
	}

	@Test
	public void findAllByIdTestNotNull() {
		when(this.datastore.get((Key[]) any())).thenReturn(Collections.emptyIterator());
		this.reactiveDatastoreTemplate
				.findAllById(Collections.singletonList(this.badKey), AbstractDatastoreTemplateTests.TestEntity.class)
				.collectList()
				.subscribe(x -> assertThat(x).isEmpty());
	}

	@Test
	public void findByIdTest() {
		when(this.datastore.get((Key[]) any())).thenReturn(Arrays.asList(this.e1).iterator());
		this.reactiveDatastoreTemplate.findById(this.key1, AbstractDatastoreTemplateTests.TestEntity.class)
				.subscribe(result -> {
					assertThat(result).isEqualTo(this.ob1);
					assertThat(result.childEntities).contains(this.childEntity1);
					assertThat(this.childEntity1).isEqualTo(result.singularReference);
					assertThat(result.multipleReference).contains(this.childEntity1);
				});
	}

	@Test
	public void findByIdNotFoundTest() {
		when(this.datastore.fetch(ArgumentMatchers.<Key[]>any())).thenReturn(Collections.singletonList(null));
		verifyBeforeAndAfterEvents(null, new AfterFindByKeyEvent(Collections.emptyList(), Collections.singleton(null)),
				() -> assertThat(this.datastoreTemplate.findById(createFakeKey("key0"),
						AbstractDatastoreTemplateTests.TestEntity.class)).isNull(),
				x -> {
				});
	}

	@Test
	public void findAllByIdTest() {
		when(this.datastore.fetch(eq(this.key2), eq(this.key1)))
				.thenReturn(Arrays.asList(this.e1, this.e2));
		List<Key> keys = Arrays.asList(this.key1, this.key2);

		verifyBeforeAndAfterEvents(null,
				new AfterFindByKeyEvent(Arrays.asList(this.ob1, this.ob2), new HashSet<>(keys)),
				() -> assertThat(
						this.datastoreTemplate.findAllById(keys, AbstractDatastoreTemplateTests.TestEntity.class))
								.containsExactly(this.ob1,
										this.ob2),
				x -> {
				});
	}

	@Test
	public void findAllByIdReferenceConsistencyTest() {
		when(this.objectToKeyFactory.getKeyFromObject(eq(this.childEntity1), any())).thenReturn(this.childEntity1.id);

		when(this.datastore.fetch(eq(this.key1)))
				.thenReturn(Collections.singletonList(this.e1));

		verifyBeforeAndAfterEvents(null,
				new AfterFindByKeyEvent(Collections.singletonList(this.ob1), Collections.singleton(this.key1)),
				() -> {
					AbstractDatastoreTemplateTests.TestEntity parentEntity1 = this.datastoreTemplate.findById(this.key1,
							AbstractDatastoreTemplateTests.TestEntity.class);
					assertThat(parentEntity1).isSameAs(this.ob1);
					AbstractDatastoreTemplateTests.ChildEntity singularReference1 = parentEntity1.singularReference;
					AbstractDatastoreTemplateTests.ChildEntity childEntity1 = parentEntity1.childEntities.get(0);
					assertThat(singularReference1).isSameAs(childEntity1);

					AbstractDatastoreTemplateTests.TestEntity parentEntity2 = this.datastoreTemplate.findById(this.key1,
							AbstractDatastoreTemplateTests.TestEntity.class);
					assertThat(parentEntity2).isSameAs(this.ob1);
					AbstractDatastoreTemplateTests.ChildEntity singularReference2 = parentEntity2.singularReference;
					AbstractDatastoreTemplateTests.ChildEntity childEntity2 = parentEntity2.childEntities.get(0);
					assertThat(singularReference2).isSameAs(childEntity2);

					assertThat(childEntity1).isNotSameAs(childEntity2);
				}, x -> {
				});
	}

	@Test
	public void findAllReferenceLoopTest() {

		Entity referenceTestDatastoreEntity = Entity.newBuilder(this.key1)
				.set("sibling", this.key1)
				.build();

		when(this.datastore.fetch(eq(this.key1)))
				.thenReturn(Collections.singletonList(referenceTestDatastoreEntity));

		AbstractDatastoreTemplateTests.ReferenceTestEntity referenceTestEntity = new AbstractDatastoreTemplateTests.ReferenceTestEntity();

		when(this.datastoreEntityConverter.read(eq(AbstractDatastoreTemplateTests.ReferenceTestEntity.class),
				same(referenceTestDatastoreEntity)))
						.thenAnswer(invocationOnMock -> referenceTestEntity);

		verifyBeforeAndAfterEvents(null,
				new AfterFindByKeyEvent(Collections.singletonList(referenceTestEntity),
						Collections.singleton(this.key1)),
				() -> {
					AbstractDatastoreTemplateTests.ReferenceTestEntity readReferenceTestEntity = this.datastoreTemplate
							.findById(this.key1,
									AbstractDatastoreTemplateTests.ReferenceTestEntity.class);

					assertThat(readReferenceTestEntity.sibling).isSameAs(readReferenceTestEntity);
				}, x -> {
				});
	}

	@Test
	public void saveReferenceLoopTest() {
		AbstractDatastoreTemplateTests.ReferenceTestEntity referenceTestEntity = new AbstractDatastoreTemplateTests.ReferenceTestEntity();
		referenceTestEntity.id = 1L;
		referenceTestEntity.sibling = referenceTestEntity;
		when(this.objectToKeyFactory.getKeyFromObject(eq(referenceTestEntity), any())).thenReturn(this.key1);

		List<Object[]> callsArgs = gatherVarArgCallsArgs(this.datastore.put(ArgumentMatchers.<FullEntity[]>any()),
				Collections.singletonList(this.e1));

		assertThat(this.datastoreTemplate.save(referenceTestEntity))
				.isInstanceOf(AbstractDatastoreTemplateTests.ReferenceTestEntity.class);

		Entity writtenEntity = Entity.newBuilder(this.key1)
				.set("sibling", this.key1)
				.build();

		assertArgs(callsArgs, new MapBuilder<List, Integer>()
				.put(Collections.singletonList(writtenEntity), 1)
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

		assertThat(this.datastoreTemplate.save(this.ob1)).isInstanceOf(AbstractDatastoreTemplateTests.TestEntity.class);
		verify(this.datastore, times(1)).put(ArgumentMatchers.<FullEntity[]>any());
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
		// making sure save works when descendants are null
		assertThat(this.ob2.childEntities).isNull();
		assertThat(this.ob2.singularReference).isNull();
		assertThat(this.ob2.multipleReference).isNull();

		List<Object[]> callsArgs = gatherVarArgCallsArgs(this.datastore.put(ArgumentMatchers.<FullEntity[]>any()),
				Collections.singletonList(this.e1));

		this.datastoreTemplate.save(this.ob2);

		assertArgs(callsArgs, new MapBuilder<List, Integer>()
				.put(Collections.singletonList(Entity.newBuilder(this.key2).build()), 1).buildModifiable());

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
		AbstractDatastoreTemplateTests.ChildEntity childEntity = new AbstractDatastoreTemplateTests.ChildEntity();
		childEntity.id = keyA;
		when(this.objectToKeyFactory.getKeyFromObject(eq(childEntity), any())).thenReturn(keyA);
		List<Object[]> callsArgs = gatherVarArgCallsArgs(this.datastore.put(ArgumentMatchers.<FullEntity[]>any()),
				Collections.singletonList(this.e1));

		this.datastoreTemplate.save(childEntity, key0);

		Entity writtenChildEntity = Entity.newBuilder(keyA).build();

		assertArgs(callsArgs, new MapBuilder<List, Integer>()
				.put(Collections.singletonList(writtenChildEntity), 1)
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

		assertThat(this.datastoreTemplate.save(this.ob1)).isInstanceOf(AbstractDatastoreTemplateTests.TestEntity.class);

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

		verifyBeforeAndAfterEvents(new BeforeSaveEvent(javaExpected),
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
		verifyBeforeAndAfterEvents(null,
				new AfterQueryEvent(Arrays.asList(this.ob1, this.ob2), this.findAllTestEntityQuery),
				() -> assertThat(this.datastoreTemplate.findAll(AbstractDatastoreTemplateTests.TestEntity.class))
						.contains(this.ob1, this.ob2),
				x -> {
				});
	}

	@Test
	public void queryTest() {
		verifyBeforeAndAfterEvents(null,
				new AfterQueryEvent(Arrays.asList(this.ob1, this.ob2), this.testEntityQuery),
				() -> assertThat(this.datastoreTemplate.query((Query<Entity>) this.testEntityQuery,
						AbstractDatastoreTemplateTests.TestEntity.class))
								.contains(this.ob1, this.ob2),
				x -> {
				});
	}

	@Test
	public void queryKeysTest() {
		KeyQuery keyQuery = GqlQuery.newKeyQueryBuilder().build();
		this.datastoreTemplate.queryKeys(keyQuery).iterator();
		verify(this.datastore, times(1)).run(keyQuery);
	}

	@Test
	public void countTest() {
		setupGenericResultSets();
		this.reactiveDatastoreTemplate.count(AbstractDatastoreTemplateTests.TestEntity.class)
				.subscribe(x -> assertThat(x).isEqualTo(2));
	}

	@Test
	public void existsByIdTest() {
		setupGenericResultSets();
		this.reactiveDatastoreTemplate.existsById(this.key1, AbstractDatastoreTemplateTests.TestEntity.class)
				.subscribe(x -> assertThat(x).isTrue());
	}

	@Test
	public void deleteByIdTest() {
		when(this.objectToKeyFactory.getKeyFromId(same(this.key1), any()))
				.thenReturn(this.key1);

		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1 }, AbstractDatastoreTemplateTests.TestEntity.class,
						Collections.singletonList(this.key1),
						null),
				new AfterDeleteEvent(new Key[] { this.key1 }, AbstractDatastoreTemplateTests.TestEntity.class,
						Collections.singletonList(this.key1),
						null),
				() -> this.reactiveDatastoreTemplate
						.deleteById(this.key1, AbstractDatastoreTemplateTests.TestEntity.class).subscribe(),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key1)));
	}

	@Test
	public void deleteAllByIdTest() {
		when(this.objectToKeyFactory.getKeyFromId(same(this.key1), any()))
				.thenReturn(this.key1);
		when(this.objectToKeyFactory.getKeyFromId(same(this.key2), any()))
				.thenReturn(this.key2);

		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key2, this.key1 },
						AbstractDatastoreTemplateTests.TestEntity.class,
						Arrays.asList(this.key1, this.key2), null),
				new AfterDeleteEvent(new Key[] { this.key2, this.key1 },
						AbstractDatastoreTemplateTests.TestEntity.class,
						Arrays.asList(this.key1, this.key2), null),
				() -> this.reactiveDatastoreTemplate.deleteAllById(Arrays.asList(this.key1, this.key2),
						AbstractDatastoreTemplateTests.TestEntity.class).subscribe(),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key2), same(this.key1)));
	}

	@Test
	public void deleteObjectTest() {
		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1 }, AbstractDatastoreTemplateTests.TestEntity.class, null,
						Collections.singletonList(this.ob1)),
				new AfterDeleteEvent(new Key[] { this.key1 }, AbstractDatastoreTemplateTests.TestEntity.class, null,
						Collections.singletonList(this.ob1)),
				() -> this.reactiveDatastoreTemplate.delete(this.ob1).subscribe(),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key1)));
	}

	@Test
	public void deleteMultipleObjectsTest() {
		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1, this.key2 }, null, null,
						Arrays.asList(this.ob1, this.ob2)),
				new AfterDeleteEvent(new Key[] { this.key1, this.key2 }, null, null, Arrays.asList(this.ob1, this.ob2)),
				() -> this.reactiveDatastoreTemplate.deleteAll(Arrays.asList(this.ob1, this.ob2)).subscribe(),
				x -> x.verify(this.datastore, times(1)).delete(eq(this.key1), eq(this.key2)));
	}

	@Test
	public void deleteAllTest() {
		setupGenericResultSets();
		verifyBeforeAndAfterEvents(
				new BeforeDeleteEvent(new Key[] { this.key1, this.key2 },
						AbstractDatastoreTemplateTests.TestEntity.class, null, null),
				new AfterDeleteEvent(new Key[] { this.key1, this.key2 },
						AbstractDatastoreTemplateTests.TestEntity.class, null, null),
				() -> this.reactiveDatastoreTemplate.deleteAll(AbstractDatastoreTemplateTests.TestEntity.class)
						.subscribe(),
				x -> x.verify(this.datastore, times(1)).delete(same(this.key1), same(this.key2)));
	}

	@Test
	public void findAllTestSortLimitOffset() {
		setupGenericResultSets();
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("custom_test_kind");

		this.reactiveDatastoreTemplate.findAll(AbstractDatastoreTemplateTests.TestEntity.class,
				new DatastoreQueryOptions.Builder().setLimit(2).setOffset(3)
						.setSort(Sort.by("sortProperty")).build())
				.collectList().subscribe(x -> {
					verify(this.datastore, times(1)).run(
							builder.setLimit(2).setOffset(3)
									.setOrderBy(
											new StructuredQuery.OrderBy("prop",
													StructuredQuery.OrderBy.Direction.ASCENDING))
									.build());
				});
	}

	@Test
	public void queryByExampleOptions() {
		setupGenericResultSets();
		EntityQuery.Builder builder = Query.newEntityQueryBuilder().setKind("test_kind");
		this.reactiveDatastoreTemplate.queryByExample(Example.of(this.simpleTestEntity),
				new DatastoreQueryOptions.Builder().setLimit(10).setOffset(1).setSort(Sort.by("intField"))
						.build())
				.collectList().subscribe(x -> {
					StructuredQuery.CompositeFilter filter = StructuredQuery.CompositeFilter
							.and(StructuredQuery.PropertyFilter.eq("id", "simple_test_entity"),
									StructuredQuery.PropertyFilter.eq("int_field", 1));
					verify(this.datastore, times(1)).run(builder.setFilter(filter)
							.addOrderBy(StructuredQuery.OrderBy.asc("int_field")).setLimit(10).setOffset(1).build());
				});
	}
}
