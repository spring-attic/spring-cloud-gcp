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

package org.springframework.cloud.gcp.data.datastore.repository.support;

import java.util.List;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreQueryOptions;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class SimpleDatastoreRepositoryTests {

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
		Iterable entities = ImmutableList.of();
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
		List<String> keys = ImmutableList.of("1", "2");
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
		Iterable entities = ImmutableList.of();
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
		when(this.datastoreTemplate.performTransaction(any())).thenAnswer(invocation -> {
			Function<DatastoreOperations, String> f = invocation.getArgument(0);
			return f.apply(this.datastoreTemplate);
		});
		assertEquals("test",
				new SimpleDatastoreRepository<Object, String>(this.datastoreTemplate,
						Object.class).performTransaction(repo -> "test"));
	}

	@Test
	public void findAllPageableAsc() {
		this.simpleDatastoreRepository.findAll(PageRequest.of(0, 5, Sort.Direction.ASC, "property1"));

		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class),
				eq(new DatastoreQueryOptions(5, 0, new Sort(Sort.Direction.ASC, "property1"))));
	}

	@Test
	public void findAllPageableDesc() {
		this.simpleDatastoreRepository.findAll(PageRequest.of(1, 5, Sort.Direction.DESC, "property1", "property2"));
		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class),
				eq(new DatastoreQueryOptions(5, 5,
						Sort.by(
								new Sort.Order(Sort.Direction.DESC, "property1"),
								new Sort.Order(Sort.Direction.DESC, "property2")))));
	}

	@Test
	public void findAllSortAsc() {
		this.simpleDatastoreRepository.findAll(Sort.by(
				new Sort.Order(Sort.Direction.DESC, "property1"),
				new Sort.Order(Sort.Direction.ASC, "property2")));
		verify(this.datastoreTemplate, times(1)).findAll(eq(Object.class),
				eq(new DatastoreQueryOptions(null, null,
						Sort.by(
								new Sort.Order(Sort.Direction.DESC, "property1"),
								new Sort.Order(Sort.Direction.ASC, "property2")))));

	}
}
