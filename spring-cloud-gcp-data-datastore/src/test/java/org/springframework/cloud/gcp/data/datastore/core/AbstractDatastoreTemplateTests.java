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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.GqlQuery;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Query.ResultType;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.InOrder;
import org.mockito.Mockito;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Descendants;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorField;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorValue;
import org.springframework.cloud.gcp.data.datastore.core.mapping.Field;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Setup steps for template tests.
 *
 * @author Chengyuan Zhao
 */
public class AbstractDatastoreTemplateTests {

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	protected final Datastore datastore = mock(Datastore.class);

	protected final DatastoreEntityConverter datastoreEntityConverter = mock(
			DatastoreEntityConverter.class);

	protected final ObjectToKeyFactory objectToKeyFactory = mock(ObjectToKeyFactory.class);

	protected DatastoreTemplate datastoreTemplate;

	protected ChildEntity childEntity2;

	protected ChildEntity childEntity3;

	protected ChildEntity childEntity4;

	protected ChildEntity childEntity5;

	protected ChildEntity childEntity6;

	protected Key childKey2;

	protected Key childKey3;

	protected Key childKey4;

	protected Key childKey5;

	protected Key childKey6;

	protected SimpleTestEntity simpleTestEntity = new SimpleTestEntity();

	protected SimpleTestEntity simpleTestEntityNullVallues = new SimpleTestEntity();

	protected final ReadWriteConversions readWriteConversions = mock(ReadWriteConversions.class);

	protected Key createFakeKey(String val) {
		return new KeyFactory("project").setKind("custom_test_kind").newKey(val);
	}

	// A fake entity query used for testing.
	protected final Query testEntityQuery = GqlQuery
			.newGqlQueryBuilder(ResultType.PROJECTION_ENTITY, "fake query").build();

	// This is the query that is expected to be constructed by the template.
	protected final Query findAllTestEntityQuery = Query.newEntityQueryBuilder()
			.setKind("custom_test_kind").build();

	// The keys, entities, and objects below are constructed for all tests below. the
	// number of each
	// object here corresponds to the same thing across keys, entities, objects.
	protected final Key key1 = createFakeKey("key1");

	protected final Key key2 = createFakeKey("key2");

	protected final Key keyChild1 = createFakeKey("key3");

	protected final Key badKey = createFakeKey("badkey");

	protected final Entity e1 = Entity.newBuilder(this.key1)
			.set("singularReference", this.keyChild1)
			.set("multipleReference", Collections.singletonList(KeyValue.of(this.keyChild1)))
			.build();

	protected final Entity e2 = Entity.newBuilder(this.key2)
			.set("singularReference", this.keyChild1)
			.set("multipleReference", Collections.singletonList(KeyValue.of(this.keyChild1)))
			.build();

	protected TestEntity ob1;

	protected TestEntity ob2;

	protected ChildEntity childEntity1;

	protected QueryResults testEntityQueryResults = mock(QueryResults.class);

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
		doAnswer((invocation) -> new LinkedList<>(invocation.getArgument(0)))
				.when(this.readWriteConversions).convertOnRead(any(), any(), (Class) any());

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

	protected List<Object[]> gatherVarArgCallsArgs(Object methodCall, List<Entity> returnVal) {
		List<Object[]> callsArgs = new ArrayList<>();
		when(methodCall).thenAnswer(invocationOnMock -> {
			callsArgs.add(invocationOnMock.getArguments());
			return returnVal;
		});
		return callsArgs;
	}

	protected void assertArgs(List<Object[]> callsArgs, Map<List, Integer> expected) {
		for (Object[] args : callsArgs) {
			List<Object> key = Arrays.asList(args);
			expected.put(key, expected.computeIfAbsent(key, k -> 0) - 1);
		}
		expected.forEach((key, value) -> assertThat(value).as("Extra calls with argument " + key).isEqualTo(0));
	}

	protected void verifyBeforeAndAfterEvents(ApplicationEvent expectedBefore,
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

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	protected static class TestEntity {
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
	protected static class ChildEntity {
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
	protected static class SimpleTestEntity {
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
	protected static class SimpleDiscriminationTestEntity {
		@Id
		String id;

		@Field(name = "int_field")
		int intField;
	}

}
