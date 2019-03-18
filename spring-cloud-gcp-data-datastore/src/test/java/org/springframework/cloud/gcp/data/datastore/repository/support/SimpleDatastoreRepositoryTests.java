/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.datastore.repository.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.google.cloud.datastore.Cursor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreResultsIterable;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.repository.query.DatastorePageable;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the default Datastore Repository implementation.
 *
 * @author Chengyuan Zhao
 */
public class SimpleDatastoreRepositoryTests {
	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private final DatastoreTemplate datastoreTemplate = mock(DatastoreTemplate.class);

	private final SimpleDatastoreRepository<Object, String> simpleDatastoreRepository = new SimpleDatastoreRepository<>(
			this.datastoreTemplate, Object.class);

	@Test
	public void saveTest() {
		Object object = new Object();
		this.simpleDatastoreRepository.save(object);
		verify(this.datastoreTemplate, times(1)).save(same(object));
	}

	@Test
	public void saveAllTest() {
		Iterable entities = Arrays.asList();
		this.simpleDatastoreRepository.saveAll(entities);
		verify(this.datastoreTemplate, times(1)).saveAll(same(entities));
	}

	@Test
	public void findByIdTest() {
		String id = "key";
		this.simpleDatastoreRepository.findById(id);
		verify(this.datastoreTemplate, times(1)).findById(eq(id), eq(Object.class));
	}

	@Test
	public void existsByIdTest() {
		String id = "key";
		this.simpleDatastoreRepository.existsById(id);
		verify(this.datastoreTemplate, times(1)).existsById(eq(id), eq(Object.class));
	}

	@Test
	public void findAllTest() {
		this.simpleDatastoreRepository.findAll();
		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class));
	}

	@Test
	public void findAllByIdTest() {
		List<String> keys = Arrays.asList("1", "2");
		this.simpleDatastoreRepository.findAllById(keys);
		verify(this.datastoreTemplate, times(1)).findAllById(eq(keys), eq(Object.class));
	}

	@Test
	public void countTest() {
		this.simpleDatastoreRepository.count();
		verify(this.datastoreTemplate, times(1)).count(eq(Object.class));
	}

	@Test
	public void deleteByIdTest() {
		String id = "key";
		this.simpleDatastoreRepository.deleteById(id);
		verify(this.datastoreTemplate, times(1)).deleteById(eq(id), eq(Object.class));
	}

	@Test
	public void deleteTest() {
		Object object = new Object();
		this.simpleDatastoreRepository.delete(object);
		verify(this.datastoreTemplate, times(1)).delete(same(object));
	}

	@Test
	public void deleteAllTest() {
		Iterable entities = Arrays.asList();
		this.simpleDatastoreRepository.deleteAll(entities);
		verify(this.datastoreTemplate, times(1)).deleteAll(same(entities));
	}

	@Test
	public void deleteAllClassTest() {
		this.simpleDatastoreRepository.deleteAll();
		verify(this.datastoreTemplate, times(1)).deleteAll(eq(Object.class));
	}

	@Test
	public void runTransactionCallableTest() {
		when(this.datastoreTemplate.performTransaction(any())).thenAnswer((invocation) -> {
			Function<DatastoreOperations, String> f = invocation.getArgument(0);
			return f.apply(this.datastoreTemplate);
		});

		String result = new SimpleDatastoreRepository<Object, String>(this.datastoreTemplate, Object.class)
				.performTransaction((repo) -> "test");
		assertThat(result).isEqualTo("test");
	}

	@Test
	public void findAllPageableAsc() {
		this.simpleDatastoreRepository.findAll(PageRequest.of(0, 5, Sort.Direction.ASC, "property1"));

		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class),
				eq(new DatastoreQueryOptions.Builder().setLimit(5).setOffset(0)
						.setSort(new Sort(Sort.Direction.ASC, "property1")).build()));
		verify(this.datastoreTemplate, times(1)).count(any());
	}

	@Test
	public void findAllPageableDesc() {
		this.simpleDatastoreRepository.findAll(PageRequest.of(1, 5, Sort.Direction.DESC, "property1", "property2"));
		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class),
				eq(new DatastoreQueryOptions.Builder().setLimit(5).setOffset(5).setSort(Sort.by(
						new Sort.Order(Sort.Direction.DESC, "property1"),
						new Sort.Order(Sort.Direction.DESC, "property2"))).setCursor(null).build()));
		verify(this.datastoreTemplate, times(1)).count(any());
	}

	@Test
	public void findAllPageableCursor() {
		Cursor cursor = Cursor.copyFrom("abc".getBytes());
		Pageable pageable = DatastorePageable.from(PageRequest.of(1, 5, Sort.Direction.DESC, "property1", "property2"),
				cursor, 10L);
		this.simpleDatastoreRepository.findAll(pageable);
		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class),
				eq(new DatastoreQueryOptions.Builder().setLimit(5).setOffset(5).setSort(Sort.by(
						new Sort.Order(Sort.Direction.DESC, "property1"),
						new Sort.Order(Sort.Direction.DESC, "property2"))).setCursor(cursor).build()));
		verify(this.datastoreTemplate, times(0)).count(any());
	}

	@Test
	public void findAllByExample() {
		Example<Object> example = Example.of(new Object());
		this.simpleDatastoreRepository.findAll(example);
		verify(this.datastoreTemplate, times(1)).queryByExample(same(example), isNull());
	}

	@Test
	public void findAllByExampleSort() {
		Example<Object> example = Example.of(new Object());
		Sort sort = Sort.by("id");
		this.simpleDatastoreRepository.findAll(example, sort);
		verify(this.datastoreTemplate, times(1)).queryByExample(same(example),
				eq(new DatastoreQueryOptions.Builder().setSort(sort).build()));
	}

	@Test
	public void findAllByExamplePage() {
		Example<Object> example = Example.of(new Object());
		Sort sort = Sort.by("id");

		doAnswer((invocationOnMock) -> new DatastoreResultsIterable(Arrays.asList(1, 2), null))
				.when(this.datastoreTemplate).queryByExample(same(example),
						eq(new DatastoreQueryOptions.Builder().setLimit(2).setOffset(2).setSort(sort)
								.build()));

		doAnswer((invocationOnMock) -> new DatastoreResultsIterable(Arrays.asList(1, 2, 3, 4, 5), null))
				.when(this.datastoreTemplate).keyQueryByExample(same(example), isNull());


		Page<Object> result = this.simpleDatastoreRepository.findAll(example, PageRequest.of(1, 2, sort));
		assertThat(result).containsExactly(1, 2);
		assertThat(result.getTotalElements()).isEqualTo(5);
		verify(this.datastoreTemplate, times(1)).queryByExample(same(example),
				eq(new DatastoreQueryOptions.Builder().setLimit(2).setOffset(2).setSort(sort)
						.build()));
		verify(this.datastoreTemplate, times(1)).keyQueryByExample(same(example), isNull());
	}


	@Test
	public void findAllByExamplePageCursor() {
		Example<Object> example = Example.of(new Object());
		Sort sort = Sort.by("id");
		Cursor cursor = Cursor.copyFrom("abc".getBytes());

		doAnswer((invocationOnMock) -> new DatastoreResultsIterable(Arrays.asList(1, 2), cursor))
				.when(this.datastoreTemplate).queryByExample(same(example),
						eq(new DatastoreQueryOptions.Builder().setLimit(2).setOffset(0).setSort(sort)
								.build()));

		doAnswer((invocationOnMock) -> new DatastoreResultsIterable(Arrays.asList(3, 4), null))
				.when(this.datastoreTemplate).queryByExample(same(example),
						eq(new DatastoreQueryOptions.Builder().setLimit(2).setOffset(2).setSort(sort).setCursor(cursor)
								.build()));


		doAnswer((invocationOnMock) -> new DatastoreResultsIterable(Arrays.asList(1, 2, 3, 4, 5), null))
				.when(this.datastoreTemplate).keyQueryByExample(same(example), isNull());


		Page<Object> result = this.simpleDatastoreRepository.findAll(example, PageRequest.of(0, 2, sort));
		assertThat(result).containsExactly(1, 2);
		assertThat(result.getTotalElements()).isEqualTo(5);

		Page<Object> resultNext = this.simpleDatastoreRepository.findAll(example, result.getPageable().next());
		assertThat(resultNext).containsExactly(3, 4);
		assertThat(resultNext.getTotalElements()).isEqualTo(5);

		verify(this.datastoreTemplate, times(1)).queryByExample(same(example),
				eq(new DatastoreQueryOptions.Builder().setLimit(2).setOffset(0).setSort(sort)
						.build()));
		verify(this.datastoreTemplate, times(1)).queryByExample(same(example),
				eq(new DatastoreQueryOptions.Builder().setLimit(2).setOffset(2).setSort(sort).setCursor(cursor)
						.build()));
		verify(this.datastoreTemplate, times(1)).keyQueryByExample(same(example), isNull());
	}

	@Test
	public void findAllByExamplePageNull() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A non-null pageable is required.");

		this.simpleDatastoreRepository.findAll(Example.of(new Object()), (Pageable) null);
	}

	@Test
	public void findOneByExample() {
		Example<Object> example = Example.of(new Object());

		doAnswer((invocationOnMock) -> new DatastoreResultsIterable(Arrays.asList(1), null))
				.when(this.datastoreTemplate).queryByExample(same(example),
				eq(new DatastoreQueryOptions.Builder().setLimit(1).build()));


		assertThat(this.simpleDatastoreRepository.findOne(example).get()).isEqualTo(1);

		verify(this.datastoreTemplate, times(1)).queryByExample(same(example),
				eq(new DatastoreQueryOptions.Builder().setLimit(1).build()));
	}

	@Test
	public void existsByExampleTrue() {
		Example<Object> example2 = Example.of(new Object());

		doAnswer((invocationOnMock) -> Arrays.asList(1))
				.when(this.datastoreTemplate).keyQueryByExample(same(example2),
						eq(new DatastoreQueryOptions.Builder().setLimit(1).build()));

		assertThat(this.simpleDatastoreRepository.exists(example2)).isEqualTo(true);

		verify(this.datastoreTemplate, times(1)).keyQueryByExample(same(example2),
				eq(new DatastoreQueryOptions.Builder().setLimit(1).build()));
	}

	@Test
	public void existsByExampleFalse() {
		Example<Object> example2 = Example.of(new Object());

		doAnswer((invocationOnMock) -> Arrays.asList())
				.when(this.datastoreTemplate).keyQueryByExample(same(example2),
						eq(new DatastoreQueryOptions.Builder().setLimit(1).build()));

		assertThat(this.simpleDatastoreRepository.exists(example2)).isEqualTo(false);

		verify(this.datastoreTemplate, times(1)).keyQueryByExample(same(example2),
				eq(new DatastoreQueryOptions.Builder().setLimit(1).build()));
	}

	@Test
	public void countByExample() {
		Example<Object> example2 = Example.of(new Object());

		doAnswer((invocationOnMock) -> Arrays.asList(1, 2, 3))
				.when(this.datastoreTemplate).keyQueryByExample(same(example2), isNull());

		assertThat(this.simpleDatastoreRepository.count(example2)).isEqualTo(3);

		verify(this.datastoreTemplate, times(1)).keyQueryByExample(same(example2), isNull());
	}

	@Test
	public void countByExampleZero() {
		Example<Object> example1 = Example.of(new Object());

		doAnswer((invocationOnMock) -> new ArrayList<>())
				.when(this.datastoreTemplate).keyQueryByExample(same(example1), isNull());

		assertThat(this.simpleDatastoreRepository.count(example1)).isEqualTo(0);

		verify(this.datastoreTemplate, times(1)).keyQueryByExample(same(example1), isNull());
	}

	@Test
	public void findAllSortAsc() {
		this.simpleDatastoreRepository.findAll(Sort.by(
				new Sort.Order(Sort.Direction.DESC, "property1"),
				new Sort.Order(Sort.Direction.ASC, "property2")));
		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class),
				eq(new DatastoreQueryOptions.Builder().setSort(Sort.by(
						new Sort.Order(Sort.Direction.DESC, "property1"),
						new Sort.Order(Sort.Direction.ASC, "property2"))).build()));

	}
}
