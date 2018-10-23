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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.cloud.ByteArray;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.ReadContext;
import com.google.cloud.spanner.ReadOnlyTransaction;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.TimestampBound;
import com.google.cloud.spanner.TransactionContext;
import com.google.cloud.spanner.TransactionRunner;
import com.google.cloud.spanner.TransactionRunner.TransactionCallable;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Column;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Interleaved;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SpannerTemplateTests {

	private DatabaseClient databaseClient;

	private SpannerMappingContext mappingContext;

	private SpannerEntityProcessor objectMapper;

	private SpannerMutationFactory mutationFactory;

	private ReadContext readContext;

	private SpannerTemplate spannerTemplate;

	private SpannerSchemaUtils schemaUtils;

	@Before
	public void setUp() {
		this.databaseClient = mock(DatabaseClient.class);
		this.mappingContext = new SpannerMappingContext();
		this.objectMapper = mock(SpannerEntityProcessor.class);
		this.mutationFactory = mock(SpannerMutationFactory.class);
		this.schemaUtils = new SpannerSchemaUtils(this.mappingContext, this.objectMapper,
				true);
		this.readContext = mock(ReadContext.class);
		when(this.databaseClient.singleUse()).thenReturn(this.readContext);
		this.spannerTemplate = new SpannerTemplate(this.databaseClient,
				this.mappingContext, this.objectMapper, this.mutationFactory,
				this.schemaUtils);
	}

	@Test
	public void readWriteTransactionTest() {

		TransactionRunner transactionRunner = mock(TransactionRunner.class);
		when(this.databaseClient.readWriteTransaction()).thenReturn(transactionRunner);

		TransactionContext transactionContext = mock(TransactionContext.class);

		when(transactionRunner.run(any())).thenAnswer(invocation -> {
			TransactionCallable transactionCallable = invocation.getArgument(0);
			return transactionCallable.run(transactionContext);
		});

		TestEntity t = new TestEntity();

		String finalResult = this.spannerTemplate
				.performReadWriteTransaction(spannerOperations -> {
					List<TestEntity> items = spannerOperations.readAll(TestEntity.class);
					spannerOperations.update(t);
					return "all done";
				});

		assertEquals("all done", finalResult);
		verify(transactionContext, times(1)).buffer((List<Mutation>) any());
		verify(transactionContext, times(1)).read(eq("custom_test_table"), any(), any());
	}

	@Test
	public void readOnlyTransactionTest() {

		ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
		when(this.databaseClient.readOnlyTransaction(
				eq(TimestampBound.ofReadTimestamp(Timestamp.ofTimeMicroseconds(333)))))
						.thenReturn(readOnlyTransaction);

		String finalResult = this.spannerTemplate
				.performReadOnlyTransaction(spannerOperations -> {
					List<TestEntity> items = spannerOperations.readAll(TestEntity.class);
					TestEntity item = spannerOperations.read(TestEntity.class,
							Key.of("key"));
					return "all done";
				}, new SpannerReadOptions()
						.setTimestamp(Timestamp.ofTimeMicroseconds(333)));

		assertEquals("all done", finalResult);
		verify(readOnlyTransaction, times(2)).read(eq("custom_test_table"), any(), any());
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullDatabaseClientTest() {
		new SpannerTemplate(null, this.mappingContext, this.objectMapper,
				this.mutationFactory, this.schemaUtils);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMappingContextTest() {
		new SpannerTemplate(this.databaseClient, null, this.objectMapper,
				this.mutationFactory, this.schemaUtils);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullObjectMapperTest() {
		new SpannerTemplate(this.databaseClient, this.mappingContext, null,
				this.mutationFactory, this.schemaUtils);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMutationFactoryTest() {
		new SpannerTemplate(this.databaseClient, this.mappingContext, this.objectMapper,
				null, this.schemaUtils);
	}

	@Test
	public void getMappingContextTest() {
		assertSame(this.mappingContext, this.spannerTemplate.getMappingContext());
	}

	@Test
	public void findSingleKeyNullTest() {
		when(this.readContext.read(any(), any(), any())).thenReturn(null);
		assertNull(this.spannerTemplate.read(TestEntity.class, Key.of("key")));
	}

	@Test
	public void findSingleKeyTest() {
		SpannerTemplate spyTemplate = spy(this.spannerTemplate);
		spyTemplate.read(TestEntity.class, Key.of("key"));
		verify(spyTemplate, times(1)).read(eq(TestEntity.class), eq(Key.of("key")),
				eq(null));
	}

	@Test
	public void findKeySetTest() {
		SpannerTemplate spyTemplate = spy(this.spannerTemplate);
		KeySet keys = KeySet.newBuilder().addKey(Key.of("key1")).addKey(Key.of("key2"))
				.build();
		spyTemplate.read(TestEntity.class, keys);
		verify(spyTemplate, times(1)).read(eq(TestEntity.class), same(keys), eq(null));
	}

	@Test
	public void findMultipleKeysTest() {
		ResultSet results = mock(ResultSet.class);
		ReadOption readOption = mock(ReadOption.class);
		SpannerReadOptions options = new SpannerReadOptions().addReadOption(readOption);
		KeySet keySet = KeySet.singleKey(Key.of("key"));
		when(this.readContext.read(any(), any(), any(), any())).thenReturn(results);
		this.spannerTemplate.read(TestEntity.class, keySet, options);
		verify(this.objectMapper, times(1)).mapToList(same(results),
				eq(TestEntity.class), isNull(), eq(false));
		verify(this.readContext, times(1)).read(eq("custom_test_table"), same(keySet),
				any(), same(readOption));
	}

	@Test
	public void findMultipleKeysWithIndexTest() {
		ResultSet results = mock(ResultSet.class);
		ReadOption readOption = mock(ReadOption.class);
		SpannerReadOptions options = new SpannerReadOptions().addReadOption(readOption)
				.setIndex("index");
		KeySet keySet = KeySet.singleKey(Key.of("key"));
		when(this.readContext.readUsingIndex(any(), any(), any(), any(), any()))
				.thenReturn(results);
		this.spannerTemplate.read(TestEntity.class, keySet, options);
		verify(this.objectMapper, times(1)).mapToList(same(results),
				eq(TestEntity.class), isNull(), eq(false));
		verify(this.readContext, times(1)).readUsingIndex(eq("custom_test_table"),
				eq("index"), same(keySet), any(), same(readOption));
	}

	@Test
	public void findAllTest() {
		SpannerTemplate spyTemplate = spy(this.spannerTemplate);
		SpannerReadOptions options = new SpannerReadOptions();
		spyTemplate.readAll(TestEntity.class, options);
		verify(spyTemplate, times(1)).read(eq(TestEntity.class), eq(KeySet.all()),
				same(options));
	}

	@Test
	public void insertTest() {
		Mutation mutation = Mutation.newInsertBuilder("custom_test_table").build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.insert(entity))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.insert(entity);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void insertAllTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.insert(same(entity)))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.insertAll(ImmutableList.of(entity, entity, entity));
		verify(this.databaseClient, times(1))
				.write(eq(ImmutableList.of(mutation, mutation, mutation)));
	}

	@Test
	public void updateTest() {
		Mutation mutation = Mutation.newUpdateBuilder("custom_test_table").build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.update(entity, null))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.update(entity);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void updateAllTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.update(same(entity), isNull()))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.updateAll(ImmutableList.of(entity, entity, entity));
		verify(this.databaseClient, times(1))
				.write(eq(ImmutableList.of(mutation, mutation, mutation)));
	}

	@Test
	public void updateColumnsArrayTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.update(same(entity),
				eq(new HashSet<>(Arrays.asList("a", "b")))))
						.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.update(entity, "a", "b");
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void updateColumnsSetTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		Set<String> cols = new HashSet<>(Arrays.asList("a", "b"));
		when(this.mutationFactory.update(same(entity), eq(cols)))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.update(entity, cols);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void upsertTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.upsert(same(entity), isNull()))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.upsert(entity);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void upsertAllTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.upsert(same(entity), isNull()))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.upsertAll(ImmutableList.of(entity, entity, entity));
		verify(this.databaseClient, times(1))
				.write(eq(ImmutableList.of(mutation, mutation, mutation)));
	}

	@Test
	public void upsertColumnsArrayTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.upsert(same(entity),
				eq(new HashSet<>(Arrays.asList("a", "b")))))
						.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.upsert(entity, "a", "b");
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void upsertColumnsSetTest() {
		Mutation mutation = Mutation.newInsertOrUpdateBuilder("custom_test_table")
				.build();
		TestEntity entity = new TestEntity();
		Set<String> cols = new HashSet<>(Arrays.asList("a", "b"));
		when(this.mutationFactory.upsert(same(entity), eq(cols)))
				.thenReturn(Collections.singletonList(mutation));
		this.spannerTemplate.upsert(entity, cols);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void deleteByKeyTest() {
		Key key = Key.of("key");
		Mutation mutation = Mutation.delete("custom_test_table", key);
		when(this.mutationFactory.delete(eq(TestEntity.class), same(key)))
				.thenReturn(mutation);
		this.spannerTemplate.delete(TestEntity.class, key);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void deleteObjectTest() {
		Mutation mutation = Mutation.delete("custom_test_table", Key.of("key"));
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.delete(entity)).thenReturn(mutation);
		this.spannerTemplate.delete(entity);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void deleteAllObjectTest() {
		Mutation mutation = Mutation.delete("custom_test_table", Key.of("key"));
		TestEntity entity = new TestEntity();
		when(this.mutationFactory.delete(entity)).thenReturn(mutation);
		this.spannerTemplate.deleteAll(ImmutableList.of(entity, entity, entity));
		verify(this.databaseClient, times(1))
				.write(eq(ImmutableList.of(mutation, mutation, mutation)));
	}

	@Test
	public void deleteKeysTest() {
		KeySet keys = KeySet.newBuilder().addKey(Key.of("key1")).addKey(Key.of("key2"))
				.build();
		Mutation mutation = Mutation.delete("custom_test_table", keys);
		when(this.mutationFactory.delete(eq(TestEntity.class), same(keys)))
				.thenReturn(mutation);
		this.spannerTemplate.delete(TestEntity.class, keys);
		verify(this.databaseClient, times(1))
				.write(eq(Collections.singletonList(mutation)));
	}

	@Test
	public void countTest() {
		ResultSet results = mock(ResultSet.class);
		when(this.readContext
				.executeQuery(eq(Statement.of("SELECT COUNT(*) FROM custom_test_table"))))
						.thenReturn(results);
		this.spannerTemplate.count(TestEntity.class);
		verify(results, times(1)).next();
		verify(results, times(1)).getLong(eq(0));
		verify(results, times(1)).close();
	}

	@Test
	public void findAllPageableTest() {
		SpannerTemplate spyTemplate = spy(this.spannerTemplate);
		Sort sort = mock(Sort.class);
		Pageable pageable = mock(Pageable.class);

		long offset = 5L;
		int limit = 3;
		SpannerPageableQueryOptions queryOption = new SpannerPageableQueryOptions()
				.setOffset(offset)
				.setLimit(limit);

		when(pageable.getOffset()).thenReturn(offset);
		when(pageable.getPageSize()).thenReturn(limit);
		when(pageable.getSort()).thenReturn(sort);

		TestEntity t1 = new TestEntity();
		t1.id = "a";
		TestEntity t2 = new TestEntity();
		t2.id = "b";
		TestEntity t3 = new TestEntity();
		t3.id = "c";

		List<TestEntity> items = new ArrayList<>();
		items.add(t1);
		items.add(t2);
		items.add(t3);

		doReturn(items).when(spyTemplate).queryAll(eq(TestEntity.class),
				same(queryOption));

		List results = spyTemplate.queryAll(TestEntity.class,
				queryOption.setSort(pageable.getSort()));
		assertEquals("a", ((TestEntity) results.get(0)).id);
		assertEquals("b", ((TestEntity) results.get(1)).id);
		assertEquals("c", ((TestEntity) results.get(2)).id);
	}

	@Test
	public void resolveChildEntityTest() {
		ParentEntity p = new ParentEntity();
		p.id = "key";
		p.id2 = "key2";
		ChildEntity c = new ChildEntity();
		c.id = "key";
		c.id_2 = "key2";
		c.id3 = "key3";
		GrandChildEntity gc = new GrandChildEntity();
		gc.id = "key";
		gc.id_2 = "key2";
		gc.id3 = "key3";
		gc.id4 = "key4";
		when(this.objectMapper.mapToList(any(), eq(ParentEntity.class)))
				.thenReturn(ImmutableList.of(p));
		when(this.objectMapper.mapToList(any(), eq(ParentEntity.class), any(), eq(false)))
				.thenReturn(ImmutableList.of(p));
		when(this.objectMapper.mapToList(any(), eq(ChildEntity.class), any(), eq(false)))
				.thenReturn(ImmutableList.of(c));
		when(this.objectMapper.mapToList(any(), eq(GrandChildEntity.class), any(),
				eq(false))).thenReturn(ImmutableList.of(gc));

		ParentEntity resultWithoutChildren = this.spannerTemplate
				.readAll(ParentEntity.class,
						new SpannerReadOptions()
								.setIncludeProperties(Collections.singleton("id")))
				.get(0);
		assertNull(resultWithoutChildren.childEntities);

		ParentEntity result = this.spannerTemplate.readAll(ParentEntity.class).get(0);
		assertEquals(1, result.childEntities.size());
		assertSame(c, result.childEntities.get(0));
		assertEquals(1, result.childEntities.get(0).childEntities.size());
		assertSame(gc, result.childEntities.get(0).childEntities.get(0));
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		long id2;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		ByteArray bytes;

		List<ByteArray> bytesList;

		List<Integer> integerList;

		double[] doubles;
	}

	@Table(name = "parent_test_table")
	private static class ParentEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		@Column(name = "id_2")
		String id2;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		@Interleaved
		List<ChildEntity> childEntities;
	}

	@Table(name = "child_test_table")
	private static class ChildEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		String id_2;

		@PrimaryKey(keyOrder = 3)
		String id3;

		@Interleaved
		List<GrandChildEntity> childEntities;
	}

	@Table(name = "grand_child_test_table")
	private static class GrandChildEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 2)
		String id_2;

		@PrimaryKey(keyOrder = 3)
		String id3;

		@PrimaryKey(keyOrder = 4)
		String id4;
	}
}
