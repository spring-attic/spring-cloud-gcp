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

import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.ColumnInnerType;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKeyColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
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
		TestEntity t = new TestEntity();
		List<Mutation> mutations = this.spannerMutationFactory.insert(t);
		t.id = "a";

		Mutation parentMutation = mutations.get(0);
		assertEquals(1, mutations.size());

		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(Op.INSERT, parentMutation.getOperation());

		ChildEntity c1 = new ChildEntity();
		c1.id = t.id;
		c1.id2 = "c1";
		ChildEntity c2 = new ChildEntity();
		c2.id = t.id;
		c2.id2 = "c2";
		ChildEntity c3 = new ChildEntity();
		c3.id = t.id;
		c3.id2 = "c3";

		t.childEntity = c1;
		t.childEntities = Arrays.asList(new ChildEntity[] { c2, c3 });

		mutations = this.spannerMutationFactory.insert(t);
		parentMutation = mutations.get(0);
		assertEquals(4, mutations.size());
		List<Mutation> childMutations = mutations.subList(1, mutations.size());

		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(Op.INSERT, parentMutation.getOperation());

		for (Mutation childMutation : childMutations) {
			assertEquals("child_test_table", childMutation.getTable());
			assertEquals(Op.INSERT, childMutation.getOperation());
		}
	}

	@Test(expected = SpannerDataException.class)
	public void insertChildMismatchIdTest() {
		TestEntity t = new TestEntity();
		t.id = "a";

		ChildEntity c1 = new ChildEntity();
		c1.id = "b";
		c1.id2 = "c1";

		t.childEntity = c1;

		// throws exception because child entity's id column does not match that of its
		// parent.
		this.spannerMutationFactory.insert(t);
	}

	@Test(expected = SpannerDataException.class)
	public void insertChildrenMismatchIdTest() {
		TestEntity t = new TestEntity();
		t.id = "a";

		ChildEntity c1 = new ChildEntity();
		c1.id = "b";
		c1.id2 = "c1";

		t.childEntities = Arrays.asList(c1);

		// throws exception because child entity's id column does not match that of its
		// parent.
		this.spannerMutationFactory.insert(t);
	}

	@Test
	public void updateTest() {
		TestEntity t = new TestEntity();
		List<Mutation> mutations = this.spannerMutationFactory.update(t, null);
		t.id = "a";

		Mutation parentMutation = mutations.get(0);
		assertEquals(1, mutations.size());

		ChildEntity c = new ChildEntity();
		c.id = t.id;

		t.childEntity = c;
		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(Op.UPDATE, parentMutation.getOperation());

		mutations = this.spannerMutationFactory.update(t, null);
		parentMutation = mutations.get(0);
		Mutation childMutation = mutations.get(1);

		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(Op.UPDATE, parentMutation.getOperation());

		assertEquals("child_test_table", childMutation.getTable());
		assertEquals(Op.UPDATE, childMutation.getOperation());
	}

	@Test
	public void upsertTest() {
		TestEntity t = new TestEntity();
		List<Mutation> mutations = this.spannerMutationFactory.upsert(t, null);
		t.id = "a";

		Mutation parentMutation = mutations.get(0);
		assertEquals(1, mutations.size());

		ChildEntity c = new ChildEntity();
		c.id = t.id;

		t.childEntity = c;

		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(Op.INSERT_OR_UPDATE, parentMutation.getOperation());

		mutations = this.spannerMutationFactory.upsert(t, null);
		parentMutation = mutations.get(0);
		Mutation childMutation = mutations.get(1);

		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(Op.INSERT_OR_UPDATE, parentMutation.getOperation());

		assertEquals("child_test_table", childMutation.getTable());
		assertEquals(Op.INSERT_OR_UPDATE, childMutation.getOperation());
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

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKeyColumn(keyOrder = 1)
		String id;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		ChildEntity childEntity;

		@ColumnInnerType(innerType = ChildEntity.class)
		List<ChildEntity> childEntities;
	}

	@Table(name = "child_test_table")
	private static class ChildEntity {
		@PrimaryKeyColumn(keyOrder = 1)
		String id;

		@PrimaryKeyColumn(keyOrder = 2)
		String id2;
	}
}
