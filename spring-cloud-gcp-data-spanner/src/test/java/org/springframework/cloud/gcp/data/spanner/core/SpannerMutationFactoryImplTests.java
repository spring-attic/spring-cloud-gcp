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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.Op;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerMutationFactoryImplTests {

	private SpannerMappingContext mappingContext;

	private SpannerConverter objectMapper;

	private SpannerMutationFactoryImpl spannerMutationFactory;

	@Before
	public void setUp() {
		this.mappingContext = new SpannerMappingContext();
		this.objectMapper = mock(SpannerConverter.class);
		this.spannerMutationFactory = new SpannerMutationFactoryImpl(this.objectMapper,
				this.mappingContext);
	}

	@Test
	public void insertTest() {
		Mutation mutation = this.spannerMutationFactory.insert(new TestEntity());
		assertEquals("custom_test_table", mutation.getTable());
		assertEquals(Op.INSERT, mutation.getOperation());
	}

	@Test
	public void updateTest() {
		Mutation mutation = this.spannerMutationFactory.update(new TestEntity());
		assertEquals("custom_test_table", mutation.getTable());
		assertEquals(Op.UPDATE, mutation.getOperation());
	}

	@Test
	public void upsertTest() {
		Mutation mutation = this.spannerMutationFactory.upsert(new TestEntity());
		assertEquals("custom_test_table", mutation.getTable());
		assertEquals(Op.INSERT_OR_UPDATE, mutation.getOperation());
	}

	@Test
	public void deleteEntitiesTest() {
		TestEntity t1 = new TestEntity();
		t1.id = "key1";
		TestEntity t2 = new TestEntity();
		t2.id = "key2";

		Mutation mutation = this.spannerMutationFactory.delete(TestEntity.class,
				Arrays.asList(t1, t2));
		assertEquals("custom_test_table", mutation.getTable());
		assertEquals(Op.DELETE, mutation.getOperation());
		List<String> keys = new ArrayList<>();
		mutation.getKeySet().getKeys().forEach((key) -> {
			keys.add((String) (key.getParts().iterator().next()));
		});
		assertThat(keys, containsInAnyOrder(t1.id, t2.id));
	}

	@Test
	public void deleteEntityTest() {
		TestEntity t1 = new TestEntity();
		t1.id = "key1";

		Mutation mutation = this.spannerMutationFactory.delete(t1);
		assertEquals("custom_test_table", mutation.getTable());
		assertEquals(Op.DELETE, mutation.getOperation());
		List<String> keys = new ArrayList<>();
		mutation.getKeySet().getKeys().forEach((key) -> {
			keys.add((String) (key.getParts().iterator().next()));
		});
		assertThat(keys, containsInAnyOrder(t1.id));
	}

	@Test
	public void deleteKeysTest() {
		KeySet keySet = KeySet.newBuilder().addKey(Key.of("key1")).addKey(Key.of("key2"))
				.build();
		Mutation mutation = this.spannerMutationFactory.delete(TestEntity.class, keySet);
		assertEquals("custom_test_table", mutation.getTable());
		assertEquals(Op.DELETE, mutation.getOperation());
		List<String> keys = new ArrayList<>();
		mutation.getKeySet().getKeys().forEach((key) -> {
			keys.add((String) (key.getParts().iterator().next()));
		});
		assertThat(keys, containsInAnyOrder("key1", "key2"));
	}

	@Test
	public void deleteKeyTest() {
		Key key = Key.of("key1");
		Mutation mutation = this.spannerMutationFactory.delete(TestEntity.class, key);
		assertEquals("custom_test_table", mutation.getTable());
		assertEquals(Op.DELETE, mutation.getOperation());
		List<String> keys = new ArrayList<>();
		mutation.getKeySet().getKeys().forEach((k) -> {
			keys.add((String) (k.getParts().iterator().next()));
		});
		assertThat(keys, containsInAnyOrder("key1"));
	}

	@SpannerTable(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@SpannerColumn(name = "custom_col")
		String something;

		@SpannerColumn(name = "")
		String other;
	}
}
