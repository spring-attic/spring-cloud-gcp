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

package org.springframework.cloud.gcp.data.spanner.repository.support;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SpannerRepositoryImplTests {

	private SpannerTemplate template;

	private SpannerEntityProcessor entityProcessor;

	public static final Key A_KEY = Key.of("key");

	@Before
	public void setup() {
		this.template = mock(SpannerTemplate.class);
		this.entityProcessor = mock(SpannerEntityProcessor.class);
		when(this.template.getSpannerEntityProcessor()).thenReturn(this.entityProcessor);

	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullSpannerOperationsTest() {
		new SimpleSpannerRepository(null, Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullEntityTypeTest() {
		new SimpleSpannerRepository(this.template, null);
	}

	@Test
	public void getSpannerOperationsTest() {
		assertSame(this.template,
				new SimpleSpannerRepository(this.template, Object.class).getSpannerTemplate());
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveNullObjectTest() {
		new SimpleSpannerRepository(this.template, Object.class).save(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findNullIdTest() {
		new SimpleSpannerRepository(this.template, Object.class)
				.findById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void existsNullIdTest() {
		new SimpleSpannerRepository(this.template, Object.class)
				.existsById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteNullIdTest() {
		new SimpleSpannerRepository(this.template, Object.class)
				.deleteById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteNullEntityTest() {
		new SimpleSpannerRepository(this.template, Object.class)
				.delete(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteAllNullEntityTest() {
		new SimpleSpannerRepository(this.template, Object.class)
				.deleteAll(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveAllNullEntityTest() {
		new SimpleSpannerRepository(this.template, Object.class)
				.saveAll(null);
	}

	@Test
	public void saveTest() {
		Object ob = new Object();
		assertEquals(ob, new SimpleSpannerRepository(this.template, Object.class).save(ob));
		verify(this.template, times(1)).upsert(eq(ob));
	}

	@Test
	public void saveAllTest() {
		Object ob = new Object();
		Object ob2 = new Object();
		Iterable<Object> ret = new SimpleSpannerRepository(this.template, Object.class)
				.saveAll(Arrays.asList(ob, ob2));
		assertThat(ret, containsInAnyOrder(ob, ob2));
		verify(this.template, times(1)).upsert(eq(ob));
		verify(this.template, times(1)).upsert(eq(ob2));
	}

	@Test
	public void findByIdTest() {
		Object ret = new Object();
		when(this.entityProcessor.writeToKey(eq(A_KEY))).thenReturn(A_KEY);
		when(this.template.read(eq(Object.class), eq(A_KEY))).thenReturn(ret);
		assertEquals(ret, new SimpleSpannerRepository(this.template, Object.class).findById(A_KEY).get());
		verify(this.template, times(1)).read(eq(Object.class), eq(A_KEY));
	}

	@Test(expected = SpannerDataException.class)
	public void findByIdKeyWritingThrowsAnException() {
		when(this.entityProcessor.writeToKey(any())).thenThrow(SpannerDataException.class);
		new SimpleSpannerRepository(this.template, Object.class).findById(new Object[] {});
	}

	@Test
	public void existsByIdTestFound() {
		Object ret = new Object();
		when(this.entityProcessor.writeToKey(eq(A_KEY))).thenReturn(A_KEY);
		when(this.template.read(eq(Object.class), eq(A_KEY))).thenReturn(ret);
		assertTrue(new SimpleSpannerRepository(this.template, Object.class).existsById(A_KEY));
	}

	@Test
	public void existsByIdTestNotFound() {
		when(this.entityProcessor.writeToKey(eq(A_KEY))).thenReturn(A_KEY);
		when(this.template.read(eq(Object.class), (Key) any())).thenReturn(null);
		assertFalse(
				new SimpleSpannerRepository(this.template, Object.class)
						.existsById(A_KEY));
	}

	@Test
	public void findAllTest() {
		new SimpleSpannerRepository(this.template, Object.class).findAll();
		verify(this.template, times(1)).readAll(eq(Object.class));
	}

	@Test
	public void findAllSortTest() {
		Sort sort = mock(Sort.class);
		new SimpleSpannerRepository(this.template, Object.class).findAll(sort);
		verify(this.template, times(1)).queryAll(eq(Object.class), same(sort));
	}

	@Test
	public void findAllPageableTest() {
		Pageable pageable = mock(Pageable.class);
		new SimpleSpannerRepository(this.template, Object.class).findAll(pageable);
		verify(this.template, times(1)).queryAll(eq(Object.class), same(pageable));
	}

	@Test
	public void findAllByIdTest() {
		List<Key> unconvertedKey = Arrays.asList(Key.of("key1"), Key.of("key2"));

		when(this.entityProcessor.writeToKey(eq(Key.of("key1")))).thenReturn(Key.of("key1"));
		when(this.entityProcessor.writeToKey(eq(Key.of("key2")))).thenReturn(Key.of("key2"));
		when(this.template.read(eq(Object.class), (KeySet) any())).thenAnswer(invocation -> {
			KeySet keys = invocation.getArgument(1);
			assertThat(keys.getKeys(),
					containsInAnyOrder(Key.of("key2"), Key.of("key1")));
			return null;
		});

		new SimpleSpannerRepository(this.template, Object.class)
				.findAllById(unconvertedKey);
	}

	@Test
	public void countTest() {
		new SimpleSpannerRepository(this.template, Object.class).count();
		verify(this.template, times(1)).count(eq(Object.class));
	}

	@Test
	public void deleteByIdTest() {
		when(this.entityProcessor.writeToKey(eq(A_KEY))).thenReturn(A_KEY);
		new SimpleSpannerRepository(this.template, Object.class).deleteById(A_KEY);
		verify(this.template, times(1)).delete(eq(Object.class), eq(A_KEY));
		verify(this.template, times(1)).delete(eq(Object.class), eq(A_KEY));
	}

	@Test
	public void deleteTest() {
		Object ob = new Object();
		new SimpleSpannerRepository(this.template, Object.class).delete(ob);
		verify(this.template, times(1)).delete(eq(ob));
	}

	@Test
	public void deleteManyObsTest() {
		Iterable<String> obs = Arrays.asList("ob1", "ob2");
		doAnswer(invocation -> {
			Iterable<String> toDelete = invocation.getArgument(1);
			assertThat(toDelete, containsInAnyOrder("ob1", "ob2"));
			return null;
		}).when(this.template).delete(eq(String.class), same(obs));
		new SimpleSpannerRepository(this.template, Object.class).deleteAll(obs);
	}

	@Test
	public void deleteAllTest() {
		new SimpleSpannerRepository(this.template, Object.class).deleteAll();
		verify(this.template, times(1)).delete(eq(Object.class), eq(KeySet.all()));
	}

	@Test
	public void readOnlyTransactionTest() {
		when(this.template.performReadOnlyTransaction(any(), any()))
				.thenAnswer(invocation -> {
					Function f = invocation.getArgument(0);
					return f.apply(this.template);
				});
		assertEquals("test", new SimpleSpannerRepository(this.template, Object.class)
				.performReadOnlyTransaction(repo -> "test"));
	}

	@Test
	public void readWriteTransactionTest() {
		when(this.template.performReadWriteTransaction(any())).thenAnswer(invocation -> {
			Function f = invocation.getArgument(0);
			return f.apply(this.template);
		});
		assertEquals("test", new SimpleSpannerRepository(this.template, Object.class)
				.performReadWriteTransaction(repo -> "test"));
	}
}
