/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.gcp.data.spanner.core.SpannerPageableQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the standard Spanner repository implementation.
 *
 * @author Chengyuan Zhao
 */
public class SpannerRepositoryImplTests {

	private SpannerTemplate template;

	private SpannerEntityProcessor entityProcessor;

	private static final Key A_KEY = Key.of("key");

	/**
	 * checks exceptions for messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void setup() {
		this.template = mock(SpannerTemplate.class);
		this.entityProcessor = mock(SpannerEntityProcessor.class);
		when(this.template.getSpannerEntityProcessor()).thenReturn(this.entityProcessor);

	}

	@Test
	public void constructorNullSpannerOperationsTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A valid SpannerTemplate object is required.");
		new SimpleSpannerRepository<Object, Key>(null, Object.class);
	}

	@Test
	public void constructorNullEntityTypeTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A valid entity type is required.");
		new SimpleSpannerRepository<Object, Key>(this.template, null);
	}

	@Test
	public void getSpannerOperationsTest() {
		assertThat(this.template).isSameAs(
				new SimpleSpannerRepository<Object, Key>(this.template, Object.class).getSpannerTemplate());
	}

	@Test
	public void saveNullObjectTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null entity is required for saving.");
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class).save(null);
	}

	@Test
	public void findNullIdTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null ID is required.");
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.findById(null);
	}

	@Test
	public void existsNullIdTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null ID is required.");
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.existsById(null);
	}

	@Test
	public void deleteNullIdTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null ID is required.");
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.deleteById(null);
	}

	@Test
	public void deleteNullEntityTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null entity is required.");
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.delete(null);
	}

	@Test
	public void deleteAllNullEntityTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null list of entities is required.");
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.deleteAll(null);
	}

	@Test
	public void saveAllNullEntityTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null list of entities is required for saving.");
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.saveAll(null);
	}

	@Test
	public void saveTest() {
		Object ob = new Object();
		assertThat(new SimpleSpannerRepository<Object, Key>(this.template, Object.class).save(ob)).isEqualTo(ob);
		verify(this.template, times(1)).upsert(eq(ob));
	}

	@Test
	public void saveAllTest() {
		Object ob = new Object();
		Object ob2 = new Object();
		Iterable<Object> ret = new SimpleSpannerRepository<Object, Key>(this.template,
				Object.class)
				.saveAll(Arrays.asList(ob, ob2));
		assertThat(ret).containsExactlyInAnyOrder(ob, ob2);
		verify(this.template, times(1)).upsertAll(eq(ImmutableList.of(ob, ob2)));
	}

	@Test
	public void findByIdTest() {
		Object ret = new Object();
		when(this.entityProcessor.convertToKey(eq(A_KEY))).thenReturn(A_KEY);
		when(this.template.read(eq(Object.class), eq(A_KEY))).thenReturn(ret);
		assertThat(new SimpleSpannerRepository<Object, Key>(this.template, Object.class).findById(A_KEY).get())
				.isEqualTo(ret);
		verify(this.template, times(1)).read(eq(Object.class), eq(A_KEY));
	}

	@Test
	public void findByIdKeyWritingThrowsAnException() {
		this.expectedEx.expect(SpannerDataException.class);
		when(this.entityProcessor.convertToKey(any())).thenThrow(SpannerDataException.class);
		new SimpleSpannerRepository<Object, Object[]>(this.template, Object.class)
				.findById(new Object[] {});
	}

	@Test
	public void existsByIdTestFound() {
		Object ret = new Object();
		when(this.entityProcessor.convertToKey(eq(A_KEY))).thenReturn(A_KEY);
		when(this.template.read(eq(Object.class), eq(A_KEY))).thenReturn(ret);
		assertThat(new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.existsById(A_KEY)).isTrue();
	}

	@Test
	public void existsByIdTestNotFound() {
		when(this.entityProcessor.convertToKey(eq(A_KEY))).thenReturn(A_KEY);
		when(this.template.read(eq(Object.class), (Key) any())).thenReturn(null);
		assertThat(new SimpleSpannerRepository<Object, Key>(this.template, Object.class).existsById(A_KEY)).isFalse();
	}

	@Test
	public void findAllTest() {
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class).findAll();
		verify(this.template, times(1)).readAll(eq(Object.class));
	}

	@Test
	public void findAllSortTest() {
		Sort sort = mock(Sort.class);
		when(this.template.queryAll(eq(Object.class), any())).thenAnswer((invocation) -> {
			SpannerPageableQueryOptions spannerQueryOptions = invocation.getArgument(1);
			assertThat(spannerQueryOptions.getSort()).isSameAs(sort);
			return null;
		});
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.findAll(sort);
		verify(this.template, times(1)).queryAll(eq(Object.class), any());
	}

	@Test
	public void findAllPageableTest() {
		Pageable pageable = mock(Pageable.class);
		Sort sort = mock(Sort.class);
		when(pageable.getSort()).thenReturn(sort);
		when(pageable.getOffset()).thenReturn(3L);
		when(pageable.getPageSize()).thenReturn(5);
		when(this.template.queryAll(eq(Object.class), any())).thenAnswer((invocation) -> {
			SpannerPageableQueryOptions spannerQueryOptions = invocation.getArgument(1);
			assertThat(spannerQueryOptions.getSort()).isSameAs(sort);
			assertThat(spannerQueryOptions.getOffset()).isEqualTo(3);
			assertThat(spannerQueryOptions.getLimit()).isEqualTo(5);
			return new ArrayList<>();
		});
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.findAll(pageable);
		verify(this.template, times(1)).queryAll(eq(Object.class), any());
	}

	@Test
	public void findAllByIdTest() {
		List<Key> unconvertedKey = Arrays.asList(Key.of("key1"), Key.of("key2"));

		when(this.entityProcessor.convertToKey(eq(Key.of("key1")))).thenReturn(Key.of("key1"));
		when(this.entityProcessor.convertToKey(eq(Key.of("key2")))).thenReturn(Key.of("key2"));
		when(this.template.read(eq(Object.class), (KeySet) any())).thenAnswer((invocation) -> {
			KeySet keys = invocation.getArgument(1);
			assertThat(keys.getKeys()).containsExactlyInAnyOrder(Key.of("key2"), Key.of("key1"));
			return null;
		});

		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.findAllById(unconvertedKey);
	}

	@Test
	public void countTest() {
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class).count();
		verify(this.template, times(1)).count(eq(Object.class));
	}

	@Test
	public void deleteByIdTest() {
		when(this.entityProcessor.convertToKey(eq(A_KEY))).thenReturn(A_KEY);
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
				.deleteById(A_KEY);
		verify(this.template, times(1)).delete(eq(Object.class), eq(A_KEY));
		verify(this.template, times(1)).delete(eq(Object.class), eq(A_KEY));
	}

	@Test
	public void deleteTest() {
		Object ob = new Object();
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class).delete(ob);
		verify(this.template, times(1)).delete(eq(ob));
	}

	@Test
	public void deleteAllTest() {
		new SimpleSpannerRepository<Object, Key>(this.template, Object.class).deleteAll();
		verify(this.template, times(1)).delete(eq(Object.class), eq(KeySet.all()));
	}

	@Test
	public void readOnlyTransactionTest() {
		when(this.template.performReadOnlyTransaction(any(), any()))
				.thenAnswer((invocation) -> {
					Function<SpannerTemplate, String> f = invocation.getArgument(0);
					return f.apply(this.template);
				});

		Object object =
				new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
						.performReadOnlyTransaction((repo) -> "test");
		assertThat(object).isEqualTo("test");
	}

	@Test
	public void readWriteTransactionTest() {
		when(this.template.performReadWriteTransaction(any())).thenAnswer((invocation) -> {
			Function<SpannerTemplate, String> f = invocation.getArgument(0);
			return f.apply(this.template);
		});

		Object object =
				new SimpleSpannerRepository<Object, Key>(this.template, Object.class)
						.performReadWriteTransaction((repo) -> "test");
		assertThat(object).isEqualTo("test");
	}
}
