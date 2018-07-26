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

import java.util.List;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.annotation.Id;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

	private Key createFakeKey() {
		return new KeyFactory("project").setKind("custom_test_kind").newKey("key");
	}

	@Before
	public void setup() {
		this.datastoreTemplate = new DatastoreTemplate(this.datastore,
				this.datastoreEntityConverter, new DatastoreMappingContext(),
				this.objectToKeyFactory);
	}

	@Test
	public void findByIdTest() {
		Key key1 = createFakeKey();
		TestEntity ob1 = new TestEntity();
		Entity e1 = Entity.newBuilder(createFakeKey()).build();
		when(this.datastore.get(ArgumentMatchers.<Key>any())).thenReturn(e1);
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), any())).thenReturn(ob1);

		assertEquals(ob1, this.datastoreTemplate.findById(key1, TestEntity.class));
	}

	@Test
	public void findAllByIdTest() {
		Key key1 = createFakeKey();
		Key key2 = createFakeKey();
		List<Key> keys = ImmutableList.of(key1, key2);
		TestEntity ob1 = new TestEntity();
		TestEntity ob2 = new TestEntity();
		Entity e1 = Entity.newBuilder(createFakeKey()).build();
		Entity e2 = Entity.newBuilder(createFakeKey()).build();
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), any()))
				.thenAnswer(invocation -> {
					Object ret;
					if (invocation.getArgument(1) == e1) {
						ret = ob1;
					}
					else {
						ret = ob2;
					}
					return ret;
				});

		when(this.datastore.get(ArgumentMatchers.<Key[]>any()))
				.thenReturn(ImmutableList.of(e1, e2).iterator());
		assertThat(this.datastoreTemplate.findAllById(keys, TestEntity.class),
				contains(ob1, ob2));
	}

	@Test
	public void saveTest() {
		TestEntity object = new TestEntity();
		Entity entity = Entity.newBuilder(createFakeKey()).build();
		Key key = createFakeKey();
		object.id = "value";
		when(this.objectToKeyFactory.getKeyFromObject(same(object), any()))
				.thenReturn(key);
		when(this.datastore.put((FullEntity<?>) any())).thenReturn(entity);
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), same(entity)))
				.thenReturn(object);
		assertTrue(this.datastoreTemplate.save(object) instanceof TestEntity);
		verify(this.datastore, times(1)).put(eq(entity));
		verify(this.datastoreEntityConverter, times(1)).write(same(object), notNull());
	}

	@Test
	public void saveAndAllocateIdTest() {
		TestEntity object = new TestEntity();
		Entity entity = Entity.newBuilder(createFakeKey()).build();
		Key key = createFakeKey();
		when(this.objectToKeyFactory.allocateKeyForObject(same(object), any()))
				.thenReturn(key);
		when(this.datastore.put((FullEntity<?>) any())).thenReturn(entity);
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), same(entity)))
				.thenReturn(object);
		assertTrue(this.datastoreTemplate.save(object) instanceof TestEntity);
		verify(this.datastore, times(1)).put(eq(entity));
		verify(this.datastoreEntityConverter, times(1)).write(same(object), notNull());
	}

	@Test
	public void findAllTest() {
		Object ob1 = new Object();
		Object ob2 = new Object();
		Entity e1 = Entity.newBuilder(createFakeKey()).build();
		Entity e2 = Entity.newBuilder(createFakeKey()).build();
		this.datastoreTemplate.findAll(TestEntity.class);
		when(this.datastoreEntityConverter.read(eq(TestEntity.class), any()))
				.thenAnswer(invocation -> {
					Object ret;
					if (invocation.getArgument(1) == e1) {
						ret = ob1;
					}
					else {
						ret = ob2;
					}
					return ret;
				});

		QueryResults queryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(e1, e2).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(queryResults).forEachRemaining(any());
		when(this.datastore.run(
				eq(Query.newEntityQueryBuilder().setKind("custom_test_kind").build())))
						.thenReturn(queryResults);

		assertThat(this.datastoreTemplate.findAll(TestEntity.class), contains(ob1, ob2));
	}

	@Test
	public void countTest() {
		Key key = createFakeKey();
		QueryResults<Key> queryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(key, key).iterator()
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
		Key key1 = createFakeKey();
		Key key2 = createFakeKey();
		doReturn(new Object()).when(spy).findById(same(key1), eq(Object.class));
		doReturn(null).when(spy).findById(same(key2), eq(Object.class));
		assertTrue(spy.existsById(key1, Object.class));
		assertFalse(spy.existsById(key2, Object.class));
	}

	@Test
	public void deleteByIdTest() {
		Key key1 = createFakeKey();
		when(this.objectToKeyFactory.getKeyFromId(same(key1), any())).thenReturn(key1);
		this.datastoreTemplate.deleteById(key1, TestEntity.class);
		verify(this.datastore, times(1)).delete(same(key1));
	}

	@Test
	public void deleteAllByIdTest() {
		Key key1 = createFakeKey();
		Key key2 = createFakeKey();
		when(this.objectToKeyFactory.getKeyFromId(same(key1), any())).thenReturn(key1);
		when(this.objectToKeyFactory.getKeyFromId(same(key2), any())).thenReturn(key2);
		this.datastoreTemplate.deleteAllById(ImmutableList.of(key1, key2),
				TestEntity.class);
		verify(this.datastore, times(1)).delete(same(key1), same(key2));
	}

	@Test
	public void deleteObjectTest() {
		TestEntity object = new TestEntity();
		Key key = createFakeKey();
		when(this.objectToKeyFactory.getKeyFromObject(same(object), any()))
				.thenReturn(key);

		this.datastoreTemplate.delete(object);
		verify(this.datastore, times(1)).delete(same(key));
	}

	@Test
	public void deleteAllTest() {
		TestEntity object = new TestEntity();
		Key key = createFakeKey();
		when(this.objectToKeyFactory.getKeyFromObject(same(object), any()))
				.thenReturn(key);
		QueryResults<Key> queryResults = mock(QueryResults.class);
		doAnswer(invocation -> {
			ImmutableList.of(key, key).iterator()
					.forEachRemaining(invocation.getArgument(0));
			return null;
		}).when(queryResults).forEachRemaining(any());
		when(this.datastore
				.run(eq(Query.newKeyQueryBuilder().setKind("custom_test_kind").build())))
						.thenReturn(queryResults);
		assertEquals(2, this.datastoreTemplate.deleteAll(TestEntity.class));
		verify(this.datastore, times(1)).delete(same(key), same(key));
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;
	}
}
