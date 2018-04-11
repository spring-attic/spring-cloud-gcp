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

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.KeySet;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
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

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullSpannerOperationsTest() {
		new SimpleSpannerRepository(null, Object.class);
	}

	@Test(expected = IllegalArgumentException.class)
	public void constructorNullEntityTypeTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), null);
	}

	@Test
	public void getSpannerOperationsTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		assertSame(operations, new SimpleSpannerRepository(operations, Object.class)
				.getSpannerOperations());
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveNullObjectTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), Object.class).save(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void findNullIdTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), Object.class)
				.findById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void existsNullIdTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), Object.class)
				.existsById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteNullIdTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), Object.class)
				.deleteById(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteNullEntityTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), Object.class)
				.delete(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void deleteAllNullEntityTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), Object.class)
				.deleteAll(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void saveAllNullEntityTest() {
		new SimpleSpannerRepository(mock(SpannerOperations.class), Object.class)
				.saveAll(null);
	}

	@Test
	public void saveTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Object ob = new Object();
		assertEquals(ob, new SimpleSpannerRepository(operations, Object.class).save(ob));
		verify(operations, times(1)).upsert(eq(ob));
	}

	@Test
	public void saveAllTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Object ob = new Object();
		Object ob2 = new Object();
		Iterable<Object> ret = new SimpleSpannerRepository(operations, Object.class)
				.saveAll(Arrays.asList(ob, ob2));
		assertThat(ret, containsInAnyOrder(ob, ob2));
		verify(operations, times(1)).upsert(eq(ob));
		verify(operations, times(1)).upsert(eq(ob2));
	}

	@Test
	public void findByIdTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Key key = Key.of("key");
		Object ret = new Object();
		when(operations.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertEquals(ret,
				new SimpleSpannerRepository(operations, Object.class).findById(key).get());
		verify(operations, times(1)).read(eq(Object.class), eq(key));
	}

	@Test
	public void findByIdSingleItemTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Key key = Key.of("key");
		Object ret = new Object();
		when(operations.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertEquals(ret, new SimpleSpannerRepository(operations, Object.class)
				.findById("key").get());
		verify(operations, times(1)).read(eq(Object.class), eq(key));
	}

	@Test
	public void findByIdListItemsTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Timestamp timestamp = Timestamp.ofTimeMicroseconds(333);
		Key key = Key.of("key", timestamp);
		Object ret = new Object();
		when(operations.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertEquals(ret, new SimpleSpannerRepository(operations, Object.class)
				.findById(new Object[] { "key", timestamp }).get());
		verify(operations, times(1)).read(eq(Object.class), eq(key));
	}

	@Test(expected = SpannerDataException.class)
	public void findByIdEmptyKeyTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		new SimpleSpannerRepository(operations, Object.class).findById(new Object[] {})
				.get();
	}

	@Test
	public void existsByIdTestFound() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Key key = Key.of("key");
		Object ret = new Object();
		when(operations.read(eq(Object.class), eq(key))).thenReturn(ret);
		assertTrue(new SimpleSpannerRepository(operations, Object.class).existsById(key));
	}

	@Test
	public void existsByIdTestNotFound() {
		SpannerOperations operations = mock(SpannerOperations.class);
		when(operations.read(eq(Object.class), (Key) any())).thenReturn(null);
		assertFalse(
				new SimpleSpannerRepository(operations, Object.class)
						.existsById(Key.of("key")));
	}

	@Test
	public void findAllTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		new SimpleSpannerRepository(operations, Object.class).findAll();
		verify(operations, times(1)).readAll(eq(Object.class));
	}

	@Test
	public void findAllSortTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Sort sort = mock(Sort.class);
		new SimpleSpannerRepository(operations, Object.class).findAll(sort);
		verify(operations, times(1)).queryAll(eq(Object.class), same(sort));
	}

	@Test
	public void findAllPageableTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Pageable pageable = mock(Pageable.class);
		new SimpleSpannerRepository(operations, Object.class).findAll(pageable);
		verify(operations, times(1)).queryAll(eq(Object.class), same(pageable));
	}

	@Test
	public void findAllByIdTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		when(operations.read(eq(Object.class), (KeySet) any())).thenAnswer(invocation -> {
			KeySet keys = invocation.getArgument(1);
			assertThat(keys.getKeys(),
					containsInAnyOrder(Key.of("key2"), Key.of("key1")));
			return null;
		});
		new SimpleSpannerRepository(operations, Object.class)
				.findAllById(Arrays.asList(Key.of("key1"), Key.of("key2")));
	}

	@Test
	public void countTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		new SimpleSpannerRepository(operations, Object.class).count();
		verify(operations, times(1)).count(eq(Object.class));
	}

	@Test
	public void deleteByIdTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Key key = Key.of("key");
		new SimpleSpannerRepository(operations, Object.class).deleteById(key);
		verify(operations, times(1)).delete(eq(Object.class), eq(key));
	}

	@Test
	public void deleteTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Object ob = new Object();
		new SimpleSpannerRepository(operations, Object.class).delete(ob);
		verify(operations, times(1)).delete(eq(ob));
	}

	@Test
	public void deleteManyObsTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		Iterable<String> obs = Arrays.asList("ob1", "ob2");
		doAnswer(invocation -> {
			Iterable<String> toDelete = invocation.getArgument(1);
			assertThat(toDelete, containsInAnyOrder("ob1", "ob2"));
			return null;
		}).when(operations).delete(eq(String.class), same(obs));
		new SimpleSpannerRepository(operations, Object.class).deleteAll(obs);
	}

	@Test
	public void deleteAllTest() {
		SpannerOperations operations = mock(SpannerOperations.class);
		new SimpleSpannerRepository(operations, Object.class).deleteAll();
		verify(operations, times(1)).delete(eq(Object.class), eq(KeySet.all()));
	}

}
