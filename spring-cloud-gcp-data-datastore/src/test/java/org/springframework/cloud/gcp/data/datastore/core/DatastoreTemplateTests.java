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
import java.util.function.Consumer;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.ReadOption;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
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

	private DatastoreTemplate datastoreTemplate;

	private Key createFakeKey() {
		return Key.newBuilder("test", "test").addAncestor(PathElement.of("test", "test"))
				.build().getParent();
	}

	@Before
	public void setup() {
		this.datastoreTemplate = new DatastoreTemplate(this.datastore,
				this.datastoreEntityConverter, new DatastoreMappingContext());
	}

	@Test
	public void readTest() {
		Key key = createFakeKey();
		DatastoreReadOptions readOptions = new DatastoreReadOptions()
				.addReadOption(ReadOption.eventualConsistency());
		Object obj = new Object();
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		doReturn(ImmutableList.of(obj)).when(spy).read(eq(Object.class),
				(Iterable<Key>) any(), same(readOptions));
		assertSame(obj, spy.read(Object.class, key, readOptions));
	}

	@Test
	public void readNothingTest() {
		Key key = createFakeKey();
		DatastoreReadOptions readOptions = new DatastoreReadOptions()
				.addReadOption(ReadOption.eventualConsistency());
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		doReturn(ImmutableList.of()).when(spy).read(eq(Object.class),
				(Iterable<Key>) any(), same(readOptions));
		assertNull(spy.read(Object.class, key, readOptions));
	}

	@Test
	public void readManyTest() {
		Key key1 = createFakeKey();
		Key key2 = createFakeKey();
		List<Key> keys = ImmutableList.of(key1, key2);
		Object ob1 = new Object();
		Object ob2 = new Object();
		Entity e1 = Entity.newBuilder(createFakeKey()).build();
		Entity e2 = Entity.newBuilder(createFakeKey()).build();
		DatastoreReadOptions readOptions = new DatastoreReadOptions()
				.addReadOption(ReadOption.eventualConsistency());
		when(this.datastore.get(same(keys)))
				.thenReturn(ImmutableList.of(e1, e2).iterator());
		when(this.datastore.get(same(keys), notNull()))
				.thenReturn(ImmutableList.of(e1, e2).iterator());
		when(this.datastoreEntityConverter.read(eq(Object.class), any()))
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

		assertThat(this.datastoreTemplate.read(Object.class, keys, readOptions),
				contains(ob1, ob2));
		assertThat(this.datastoreTemplate.read(Object.class, keys, null),
				contains(ob1, ob2));
	}

	@Test
	public void readAllTest() {
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		DatastoreReadOptions readOptions = new DatastoreReadOptions()
				.addReadOption(ReadOption.eventualConsistency());
		doReturn(null).when(spy).query(eq(TestEntity.class), any(), same(readOptions));
		spy.readAll(TestEntity.class, readOptions);
		verify(spy, times(1)).query(eq(TestEntity.class),
				eq(Query.newEntityQueryBuilder().setKind("custom_test_kind").build()),
				same(readOptions));
	}

	@Test
	public void queryTest() {
		Object ob1 = new Object();
		Object ob2 = new Object();
		Query<Entity> query = Query.newEntityQueryBuilder().setKind("custom_test_kind")
				.build();
		Entity e1 = Entity.newBuilder(createFakeKey()).build();
		Entity e2 = Entity.newBuilder(createFakeKey()).build();
		DatastoreReadOptions readOptions = new DatastoreReadOptions()
				.addReadOption(ReadOption.eventualConsistency());
		QueryResults queryResults = mock(QueryResults.class);
		when(this.datastore.run(same(query))).thenReturn(queryResults);
		when(this.datastore.run(same(query), notNull())).thenReturn(queryResults);
		doAnswer(invocation -> {
			ImmutableList.of(e1, e2).iterator()
					.forEachRemaining((Consumer) invocation.getArgument(0));
			return null;
		}).when(queryResults).forEachRemaining(any());
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

		assertThat(this.datastoreTemplate.query(TestEntity.class, query, readOptions),
				contains(ob1, ob2));
		assertThat(this.datastoreTemplate.query(TestEntity.class, query, null),
				contains(ob1, ob2));
	}

	@Test
	public void deleteKeyTest() {
		Key key = createFakeKey();
		this.datastoreTemplate.delete(key);
		verify(this.datastore, times(1)).delete(same(key));
	}

	@Test
	public void deleteManyKeysTest() {
		Key k1 = createFakeKey();
		Key k2 = createFakeKey();
		this.datastoreTemplate.delete(k1, k2);
		verify(this.datastore, times(1)).delete(eq(k1), eq(k2));
	}

	@Test
	public void deleteObjectTest() {
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		Object object = new Object();
		Key key = createFakeKey();
		doReturn(key).when(spy).getKey(same(object));
		spy.delete(object);
		verify(this.datastore, times(1)).delete(same(key));
	}

	@Test
	public void updateTest() {
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		Object object = new Object();
		Entity entity = Entity.newBuilder(createFakeKey()).build();
		Key key = createFakeKey();
		doReturn(key).when(spy).getKey(same(object));
		spy.update(object);
		verify(this.datastore, times(1)).update(eq(entity));
		verify(this.datastoreEntityConverter, times(1)).write(same(object), notNull());
	}

	@Test
	public void putTest() {
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		Object object = new Object();
		Entity entity = Entity.newBuilder(createFakeKey()).build();
		Key key = createFakeKey();
		doReturn(key).when(spy).getKey(same(object));
		spy.put(object);
		verify(this.datastore, times(1)).put(eq(entity));
		verify(this.datastoreEntityConverter, times(1)).write(same(object), notNull());
	}

	@Test
	public void countTest() {
		DatastoreTemplate spy = spy(this.datastoreTemplate);
		doReturn(ImmutableList.of(new Object(), new Object(), new Object())).when(spy)
				.readAll(eq(Object.class), any());
		assertEquals(3, spy.count(Object.class));
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntity {

	}
}
