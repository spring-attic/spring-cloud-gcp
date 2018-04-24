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
import java.util.function.Function;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverters;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
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

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullSpannerOperationsTest() {
		new SimpleSpannerRepository(null, Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullEntityTypeTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), null);
	}

	@Test
	public void getSpannerOperationsTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		assertSame(template,
				new SimpleSpannerRepository(template, Object.class).getSpannerTemplate());
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveNullObjectTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), Object.class).save(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findNullIdTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), Object.class)
				.findById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void existsNullIdTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), Object.class)
				.existsById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteNullIdTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), Object.class)
				.deleteById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteNullEntityTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), Object.class)
				.delete(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteAllNullEntityTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), Object.class)
				.deleteAll(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveAllNullEntityTest() {
		new SimpleSpannerRepository(mock(SpannerTemplate.class), Object.class)
				.saveAll(null);
	}

	@Test
	public void saveTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Object ob = new Object();
		assertEquals(ob, new SimpleSpannerRepository(template, Object.class).save(ob));
		verify(template, times(1)).upsert(eq(ob));
	}

	@Test
	public void saveAllTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Object ob = new Object();
		Object ob2 = new Object();
		Iterable<Object> ret = new SimpleSpannerRepository(template, Object.class)
				.saveAll(Arrays.asList(ob, ob2));
		assertThat(ret, containsInAnyOrder(ob, ob2));
		verify(template, times(1)).upsert(eq(ob));
		verify(template, times(1)).upsert(eq(ob2));
	}

	@Test
	public void findByIdTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Key key = Key.of("key");
		Object ret = new Object();
		when(template.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertEquals(ret,
				new SimpleSpannerRepository(template, Object.class).findById(key).get());
		verify(template, times(1)).read(eq(Object.class), eq(key));
	}

	@Test
	public void findByIdSingleItemTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		// A real converter with the default converters is needed to test below. A java
		// time type is given as a key and must be converted to Timestamp.
		when(template.getSpannerConverter())
				.thenReturn(new MappingSpannerConverter(new SpannerMappingContext()));
		Timestamp timestamp = Timestamp.ofTimeMicroseconds(333);
		Key key = Key.of(timestamp);
		Object ret = new Object();
		when(template.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertEquals(ret,
				new SimpleSpannerRepository(template, Object.class).findById(
						SpannerConverters.TIMESTAMP_INSTANT_CONVERTER.convert(timestamp))
						.get());
		verify(template, times(1)).read(eq(Object.class), eq(key));
	}

	@Test
	public void findByIdListItemsTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		// A real converter with the default converters is needed to test below. A java
		// time type is given as a key and must be converted to Timestamp.
		when(template.getSpannerConverter())
				.thenReturn(new MappingSpannerConverter(new SpannerMappingContext()));
		Timestamp timestamp = Timestamp.ofTimeMicroseconds(333);
		Key key = Key.of("key", timestamp);
		Object ret = new Object();
		when(template.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertEquals(ret, new SimpleSpannerRepository(template, Object.class)
				.findById(new Object[] { "key",
								SpannerConverters.TIMESTAMP_INSTANT_CONVERTER.convert(timestamp) })
				.get());
		verify(template, times(1)).read(eq(Object.class), eq(key));
	}

	@Test(expected = SpannerDataException.class)
	public void findByIdEmptyKeyTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		new SimpleSpannerRepository(template, Object.class).findById(new Object[] {})
				.get();
	}

	@Test
	public void existsByIdTestFound() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Key key = Key.of("key");
		Object ret = new Object();
		when(template.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertTrue(new SimpleSpannerRepository(template, Object.class).existsById(key));
	}

	@Test
	public void existsByIdTestNotFound() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		when(template.read(eq(Object.class), (Key) any())).thenReturn(null);
		assertFalse(
				new SimpleSpannerRepository(template, Object.class)
						.existsById(Key.of("key")));
	}

	@Test
	public void findAllTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		new SimpleSpannerRepository(template, Object.class).findAll();
		verify(template, times(1)).readAll(eq(Object.class));
	}

	@Test
	public void findAllSortTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Sort sort = mock(Sort.class);
		new SimpleSpannerRepository(template, Object.class).findAll(sort);
		verify(template, times(1)).queryAll(eq(Object.class), same(sort));
	}

	@Test
	public void findAllPageableTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Pageable pageable = mock(Pageable.class);
		new SimpleSpannerRepository(template, Object.class).findAll(pageable);
		verify(template, times(1)).queryAll(eq(Object.class), same(pageable));
	}

	@Test
	public void findAllByIdTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		when(template.read(eq(Object.class), (KeySet) any())).thenAnswer(invocation -> {
			KeySet keys = invocation.getArgument(1);
			assertThat(keys.getKeys(),
					containsInAnyOrder(Key.of("key2"), Key.of("key1")));
			return null;
		});
		new SimpleSpannerRepository(template, Object.class)
				.findAllById(Arrays.asList(Key.of("key1"), Key.of("key2")));
	}

	@Test
	public void countTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		new SimpleSpannerRepository(template, Object.class).count();
		verify(template, times(1)).count(eq(Object.class));
	}

	@Test
	public void deleteByIdTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Key key = Key.of("key");
		new SimpleSpannerRepository(template, Object.class).deleteById(key);
		verify(template, times(1)).delete(eq(Object.class), eq(key));
	}

	@Test
	public void deleteTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Object ob = new Object();
		new SimpleSpannerRepository(template, Object.class).delete(ob);
		verify(template, times(1)).delete(eq(ob));
	}

	@Test
	public void deleteManyObsTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		Iterable<String> obs = Arrays.asList("ob1", "ob2");
		doAnswer(invocation -> {
			Iterable<String> toDelete = invocation.getArgument(1);
			assertThat(toDelete, containsInAnyOrder("ob1", "ob2"));
			return null;
		}).when(template).delete(eq(String.class), same(obs));
		new SimpleSpannerRepository(template, Object.class).deleteAll(obs);
	}

	@Test
	public void deleteAllTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		new SimpleSpannerRepository(template, Object.class).deleteAll();
		verify(template, times(1)).delete(eq(Object.class), eq(KeySet.all()));
	}

	@Test
	public void readOnlyTransactionTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		when(template.performReadOnlyTransaction(any(), any()))
				.thenAnswer(invocation -> {
					Function f = invocation.getArgument(0);
					return f.apply(template);
				});
		assertEquals("test", new SimpleSpannerRepository(template, Object.class)
				.performReadOnlyTransaction(repo -> "test"));
	}

	@Test
	public void readWriteTransactionTest() {
		SpannerTemplate template = mock(SpannerTemplate.class);
		when(template.performReadWriteTransaction(any())).thenAnswer(invocation -> {
			Function f = invocation.getArgument(0);
			return f.apply(template);
		});
		assertEquals("test", new SimpleSpannerRepository(template, Object.class)
				.performReadWriteTransaction(repo -> "test"));
	}
}
