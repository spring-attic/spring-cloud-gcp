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
import java.util.Map;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Statement;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Value;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
public class SpannerSqlTemplateTests {

	private SpannerTemplate spannerTemplate;

	private SpannerMappingContext spannerMappingContext;

	private SpannerConverter spannerConverter;

	private ResultSet resultSet;

	@Before
	public void setup() {
		this.spannerMappingContext = new SpannerMappingContext();
		this.spannerConverter = new MappingSpannerConverter(this.spannerMappingContext);
		this.spannerTemplate = new SpannerTemplate(mock(DatabaseClient.class),
				this.spannerMappingContext, this.spannerConverter,
				mock(SpannerMutationFactory.class));

		Struct innerStruct = Struct.newBuilder().add("value", Value.string("value"))
				.build();
		Struct outerStruct1 = Struct.newBuilder().add("id", Value.string("key1"))
				.add("innerTestEntities",
						ImmutableList.of(Type.StructField.of("value", Type.string())),
						ImmutableList.of(innerStruct))
				.build();
		Struct outerStruct2 = Struct.newBuilder().add("col", Value.int64(3L))
				.add("inner_thing",
						ImmutableList.of(Type.StructField.of("value", Type.string())),
						ImmutableList.of(innerStruct))
				.build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(outerStruct1, outerStruct2);

		this.resultSet = mock(ResultSet.class);
		when(this.resultSet.next()).thenAnswer(invocation -> mockResults.next());
		when(this.resultSet.getCurrentRowAsStruct())
				.thenAnswer(invocation -> mockResults.getCurrent());
	}

	@Test
	public void testExecuteQuery() {
		Statement statement = Statement.of("test");
		SpannerQueryOptions queryOptions = new SpannerQueryOptions();

		SpannerTemplate spyTemplate = spy(this.spannerTemplate);

		doReturn(this.resultSet).when(spyTemplate).executeQuery(any(), any());
		verifyMappedResult(spyTemplate.getSpannerSqlTemplate().executeQuery(statement,
				queryOptions, x -> x));
		verify(spyTemplate, times(1)).executeQuery(same(statement), same(queryOptions));
	}

	@Test
	public void testExecuteRead() {
		String table = "test";
		List<String> columns = new ArrayList();
		KeySet keySet = KeySet.all();
		SpannerReadOptions readOptions = new SpannerReadOptions();
		SpannerTemplate spyTemplate = spy(this.spannerTemplate);

		doReturn(this.resultSet).when(spyTemplate).executeRead(any(), any(), any(),
				any());
		verifyMappedResult(spyTemplate.getSpannerSqlTemplate().executeRead(table, keySet,
				columns, readOptions, x -> x));

		verify(spyTemplate, times(1)).executeRead(same(table), same(keySet),
				same(columns), same(readOptions));
	}

	private void verifyMappedResult(List results) {
		assertEquals(2, results.size());
		Map struct1 = (Map) results.get(0);
		Map struct2 = (Map) results.get(1);

		assertEquals("key1", struct1.get("id"));
		assertEquals(3L, struct2.get("col"));

		List<Map> inner1 = (List<Map>) struct1.get("innerTestEntities");
		List<Map> inner2 = (List<Map>) struct2.get("inner_thing");

		assertEquals(1, inner1.size());
		assertEquals(1, inner2.size());

		assertEquals(inner1.get(0), inner2.get(0));

		assertEquals("value", inner1.get(0).get("value"));
	}

	private static class MockResults {
		List<Struct> structs;

		int counter = -1;

		boolean next() {
			if (this.counter < this.structs.size() - 1) {
				this.counter++;
				return true;
			}
			return false;
		}

		Struct getCurrent() {
			return this.structs.get(this.counter);
		}
	}
}
