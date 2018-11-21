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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.Op;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Embedded;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
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

	private SpannerEntityProcessor objectMapper;

	private SpannerSchemaUtils spannerSchemaUtils;

	private SpannerMutationFactoryImpl spannerMutationFactory;

	@Before
	public void setUp() {
		this.mappingContext = new SpannerMappingContext();
		this.objectMapper = mock(SpannerEntityProcessor.class);
		this.spannerSchemaUtils = new SpannerSchemaUtils(this.mappingContext,
				this.objectMapper, true);
		this.spannerMutationFactory = new SpannerMutationFactoryImpl(this.objectMapper,
				this.mappingContext, this.spannerSchemaUtils);
	}

	private void executeWriteTest(Function<TestEntity, List<Mutation>> writeFunc,
			Op writeMethod) {
		TestEntity t = new TestEntity();
		List<Mutation> mutations = writeFunc.apply(t);
		t.id = "a";
		Mutation parentMutation = mutations.get(0);
		assertEquals(1, mutations.size());
		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(writeMethod, parentMutation.getOperation());
		ChildEntity c1 = new ChildEntity();
		c1.keyComponents = new EmbeddedKeyComponents();
		c1.keyComponents.id = t.id;
		c1.keyComponents.id2 = "c2";
		ChildEntity c2 = new ChildEntity();
		c2.keyComponents = new EmbeddedKeyComponents();
		c2.keyComponents.id = t.id;
		c2.keyComponents.id2 = "c3";
		t.childEntities = Arrays.asList(c1, c2);
		mutations = writeFunc.apply(t);
		parentMutation = mutations.get(0);
		assertEquals(3, mutations.size());
		List<Mutation> childMutations = mutations.subList(1, mutations.size());
		assertEquals("custom_test_table", parentMutation.getTable());
		assertEquals(writeMethod, parentMutation.getOperation());
		for (Mutation childMutation : childMutations) {
			assertEquals("child_test_table", childMutation.getTable());
			assertEquals(writeMethod, childMutation.getOperation());
		}
	}

	@Test
	public void insertTest() {
		executeWriteTest(t -> this.spannerMutationFactory.insert(t), Op.INSERT);
	}

	@Test(expected = SpannerDataException.class)
	public void insertChildrenMismatchIdTest() {
		TestEntity t = new TestEntity();
		t.id = "a";
		ChildEntity c1 = new ChildEntity();
		c1.keyComponents = new EmbeddedKeyComponents();
		c1.keyComponents.id = "b";
		c1.keyComponents.id2 = "c1";
		t.childEntities = Collections.singletonList(c1);
		// throws exception because child entity's id column does not match that of its
		// parent.
		this.spannerMutationFactory.insert(t);
	}

	@Test
	public void updateTest() {
		executeWriteTest(t -> this.spannerMutationFactory.update(t, null), Op.UPDATE);
	}

	@Test
	public void upsertTest() {
		executeWriteTest(t -> this.spannerMutationFactory.upsert(t, null),
				Op.INSERT_OR_UPDATE);
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
		@PrimaryKey(keyOrder = 1)
		String id;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		@Interleaved
		List<ChildEntity> childEntities;
	}

	@Table(name = "child_test_table")
	private static class ChildEntity {
		@Embedded
		@PrimaryKey(keyOrder = 1)
		EmbeddedKeyComponents keyComponents;
	}

	private static class EmbeddedKeyComponents {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		String id2;
	}
}
