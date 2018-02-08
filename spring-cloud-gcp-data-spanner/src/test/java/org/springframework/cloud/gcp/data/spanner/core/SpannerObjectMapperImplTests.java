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

import java.util.Arrays;
import java.util.List;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.ValueBinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerObjectMapper;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerObjectMapperImpl;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerObjectMapperImplTests {

	private SpannerObjectMapper objectMapper;

	@Before
	public void setUp() {
		this.objectMapper = new SpannerObjectMapperImpl(new SpannerMappingContext());
	}

	@Test
	public void writeTest() throws ClassNotFoundException {
		TestEntity t = new TestEntity();
		t.id = "key1";
		t.stringThing = "string";
		t.booleanThing = true;
		t.longThing = 3L;
		t.doubleThing = 3.33;
		t.doubleArray = new double[] { 3.33, 3.33, 3.33 };
		t.dateThing = Date.fromYearMonthDay(2018, 11, 22);
		t.timestampThing = Timestamp.ofTimeMicroseconds(333);
		t.bytes = ByteArray.copyFrom("333");

		WriteBuilder writeBuilder = mock(WriteBuilder.class);

		ValueBinder<WriteBuilder> idBinder = mock(ValueBinder.class);
		when(idBinder.to(anyString())).thenReturn(null);
		when(writeBuilder.set(eq("id"))).thenReturn(idBinder);

		ValueBinder<WriteBuilder> stringThingBinder = mock(ValueBinder.class);
		when(stringThingBinder.to(anyString())).thenReturn(null);
		when(writeBuilder.set(eq("custom_col"))).thenReturn(stringThingBinder);

		ValueBinder<WriteBuilder> booleanThingBinder = mock(ValueBinder.class);
		when(booleanThingBinder.to((Boolean) any())).thenReturn(null);
		when(writeBuilder.set(eq("booleanThing"))).thenReturn(booleanThingBinder);

		ValueBinder<WriteBuilder> longThingBinder = mock(ValueBinder.class);
		when(longThingBinder.to(anyLong())).thenReturn(null);
		when(writeBuilder.set(eq("longThing"))).thenReturn(longThingBinder);

		ValueBinder<WriteBuilder> doubleThingBinder = mock(ValueBinder.class);
		when(doubleThingBinder.to(anyDouble())).thenReturn(null);
		when(writeBuilder.set(eq("doubleThing"))).thenReturn(doubleThingBinder);

		ValueBinder<WriteBuilder> doubleArrayThingBinder = mock(ValueBinder.class);
		when(doubleArrayThingBinder.toFloat64Array((double[]) any())).thenReturn(null);
		when(writeBuilder.set(eq("doubleArray"))).thenReturn(doubleArrayThingBinder);

		ValueBinder<WriteBuilder> dateThingBinder = mock(ValueBinder.class);
		when(dateThingBinder.to((Date) any())).thenReturn(null);
		when(writeBuilder.set(eq("dateThing"))).thenReturn(dateThingBinder);

		ValueBinder<WriteBuilder> timestampThingBinder = mock(ValueBinder.class);
		when(timestampThingBinder.to((Timestamp) any())).thenReturn(null);
		when(writeBuilder.set(eq("timestampThing"))).thenReturn(timestampThingBinder);

		ValueBinder<WriteBuilder> bytesThingBinder = mock(ValueBinder.class);
		when(bytesThingBinder.to((ByteArray) any())).thenReturn(null);
		when(writeBuilder.set(eq("bytes"))).thenReturn(bytesThingBinder);

		this.objectMapper.write(t, writeBuilder);

		verify(idBinder, times(1)).to(eq(t.id));
		verify(stringThingBinder, times(1)).to(eq(t.stringThing));
		verify(booleanThingBinder, times(1)).to(eq(Boolean.valueOf(t.booleanThing)));
		verify(longThingBinder, times(1)).to(eq(Long.valueOf(t.longThing)));
		verify(doubleThingBinder, times(1)).to(eq(Double.valueOf(t.doubleThing)));
		verify(doubleArrayThingBinder, times(1)).toFloat64Array(eq(t.doubleArray));
		verify(dateThingBinder, times(1)).to(eq(t.dateThing));
		verify(timestampThingBinder, times(1)).to(eq(t.timestampThing));
		verify(bytesThingBinder, times(1)).to(eq(t.bytes));
	}

	@Test
	public void mapToUnmodifiableListTest() {

		Struct struct1 = Struct.newBuilder().add("id", Value.string("key1"))
				.add("custom_col", Value.string("string1"))
				.add("booleanThing", Value.bool(true)).add("longThing", Value.int64(3L))
				.add("doubleThing", Value.float64(3.33))
				.add("doubleArray", Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.add("dateThing", Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.add("timestampThing", Value.timestamp(Timestamp.ofTimeMicroseconds(333)))
				.add("bytes", Value.bytes(ByteArray.copyFrom("string1"))).build();

		Struct struct2 = Struct.newBuilder().add("id", Value.string("key2"))
				.add("custom_col", Value.string("string2"))
				.add("booleanThing", Value.bool(true)).add("longThing", Value.int64(5L))
				.add("doubleThing", Value.float64(5.55))
				.add("doubleArray", Value.float64Array(new double[] { 5.55, 5.55 }))
				.add("dateThing", Value.date(Date.fromYearMonthDay(2019, 11, 22)))
				.add("timestampThing", Value.timestamp(Timestamp.ofTimeMicroseconds(555)))
				.add("bytes", Value.bytes(ByteArray.copyFrom("string2"))).build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(struct1, struct2);

		ResultSet results = mock(ResultSet.class);
		when(results.next()).thenAnswer(invocation -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer(invocation -> mockResults.getCurrent());

		List<TestEntity> entities = this.objectMapper.mapToUnmodifiableList(results,
				TestEntity.class);
		assertEquals(2, entities.size());

		TestEntity t1 = entities.get(0);
		TestEntity t2 = entities.get(1);

		assertEquals("key1", t1.id);
		assertEquals("string1", t1.stringThing);
		assertEquals(true, t1.booleanThing);
		assertEquals(3L, t1.longThing);
		assertEquals(3.33, t1.doubleThing, 0.00001);
		assertEquals(3, t1.doubleArray.length);
		assertEquals(2018, t1.dateThing.getYear());
		assertEquals(ByteArray.copyFrom("string1"), t1.bytes);

		assertEquals("key2", t2.id);
		assertEquals("string2", t2.stringThing);
		assertEquals(true, t2.booleanThing);
		assertEquals(5L, t2.longThing);
		assertEquals(5.55, t2.doubleThing, 0.00001);
		assertEquals(2, t2.doubleArray.length);
		assertEquals(2019, t2.dateThing.getYear());
		assertEquals(ByteArray.copyFrom("string2"), t2.bytes);
	}

	@SpannerTable(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@SpannerColumn(name = "custom_col")
		String stringThing;

		@SpannerColumn(name = "")
		boolean booleanThing;

		long longThing;

		double doubleThing;

		double[] doubleArray;

		Date dateThing;

		Timestamp timestampThing;

		ByteArray bytes;
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
