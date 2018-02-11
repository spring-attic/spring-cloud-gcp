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
import com.google.cloud.spanner.Mutation;
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
import static org.junit.Assert.assertNull;
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
		t.stringField = "string";
		t.booleanField = true;
		t.longField = 3L;
		t.doubleField = 3.33;
		t.doubleArray = new double[] { 3.33, 3.33, 3.33 };
		t.dateField = Date.fromYearMonthDay(2018, 11, 22);
		t.timestampField = Timestamp.ofTimeMicroseconds(333);
		t.bytes = ByteArray.copyFrom("333");

		WriteBuilder writeBuilder = mock(WriteBuilder.class);

		ValueBinder<WriteBuilder> idBinder = mock(ValueBinder.class);
		when(idBinder.to(anyString())).thenReturn(null);
		when(writeBuilder.set(eq("id"))).thenReturn(idBinder);

		ValueBinder<WriteBuilder> stringFieldBinder = mock(ValueBinder.class);
		when(stringFieldBinder.to(anyString())).thenReturn(null);
		when(writeBuilder.set(eq("custom_col"))).thenReturn(stringFieldBinder);

		ValueBinder<WriteBuilder> booleanFieldBinder = mock(ValueBinder.class);
		when(booleanFieldBinder.to((Boolean) any())).thenReturn(null);
		when(writeBuilder.set(eq("booleanField"))).thenReturn(booleanFieldBinder);

		ValueBinder<WriteBuilder> longFieldBinder = mock(ValueBinder.class);
		when(longFieldBinder.to(anyLong())).thenReturn(null);
		when(writeBuilder.set(eq("longField"))).thenReturn(longFieldBinder);

		ValueBinder<WriteBuilder> doubleFieldBinder = mock(ValueBinder.class);
		when(doubleFieldBinder.to(anyDouble())).thenReturn(null);
		when(writeBuilder.set(eq("doubleField"))).thenReturn(doubleFieldBinder);

		ValueBinder<WriteBuilder> doubleArrayFieldBinder = mock(ValueBinder.class);
		when(doubleArrayFieldBinder.toFloat64Array((double[]) any())).thenReturn(null);
		when(writeBuilder.set(eq("doubleArray"))).thenReturn(doubleArrayFieldBinder);

		ValueBinder<WriteBuilder> dateFieldBinder = mock(ValueBinder.class);
		when(dateFieldBinder.to((Date) any())).thenReturn(null);
		when(writeBuilder.set(eq("dateField"))).thenReturn(dateFieldBinder);

		ValueBinder<WriteBuilder> timestampFieldBinder = mock(ValueBinder.class);
		when(timestampFieldBinder.to((Timestamp) any())).thenReturn(null);
		when(writeBuilder.set(eq("timestampField"))).thenReturn(timestampFieldBinder);

		ValueBinder<WriteBuilder> bytesFieldBinder = mock(ValueBinder.class);
		when(bytesFieldBinder.to((ByteArray) any())).thenReturn(null);
		when(writeBuilder.set(eq("bytes"))).thenReturn(bytesFieldBinder);

		this.objectMapper.write(t, writeBuilder);

		verify(idBinder, times(1)).to(eq(t.id));
		verify(stringFieldBinder, times(1)).to(eq(t.stringField));
		verify(booleanFieldBinder, times(1)).to(eq(Boolean.valueOf(t.booleanField)));
		verify(longFieldBinder, times(1)).to(eq(Long.valueOf(t.longField)));
		verify(doubleFieldBinder, times(1)).to(eq(Double.valueOf(t.doubleField)));
		verify(doubleArrayFieldBinder, times(1)).toFloat64Array(eq(t.doubleArray));
		verify(dateFieldBinder, times(1)).to(eq(t.dateField));
		verify(timestampFieldBinder, times(1)).to(eq(t.timestampField));
		verify(bytesFieldBinder, times(1)).to(eq(t.bytes));
	}

	@Test
	public void mapToListTest() {

		Struct struct1 = Struct.newBuilder().add("id", Value.string("key1"))
				.add("custom_col", Value.string("string1"))
				.add("booleanField", Value.bool(true)).add("longField", Value.int64(3L))
				.add("doubleField", Value.float64(3.33))
				.add("doubleArray", Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.add("dateField", Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.add("timestampField", Value.timestamp(Timestamp.ofTimeMicroseconds(333)))
				.add("bytes", Value.bytes(ByteArray.copyFrom("string1"))).build();

		Struct struct2 = Struct.newBuilder().add("id", Value.string("key2"))
				.add("custom_col", Value.string("string2"))
				.add("booleanField", Value.bool(true)).add("longField", Value.int64(5L))
				.add("doubleField", Value.float64(5.55))
				.add("doubleArray", Value.float64Array(new double[] { 5.55, 5.55 }))
				.add("dateField", Value.date(Date.fromYearMonthDay(2019, 11, 22)))
				.add("timestampField", Value.timestamp(Timestamp.ofTimeMicroseconds(555)))
				.build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(struct1, struct2);

		ResultSet results = mock(ResultSet.class);
		when(results.next()).thenAnswer(invocation -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer(invocation -> mockResults.getCurrent());

		List<TestEntity> entities = this.objectMapper.mapToList(results,
				TestEntity.class);
		assertEquals(2, entities.size());

		TestEntity t1 = entities.get(0);
		TestEntity t2 = entities.get(1);

		assertEquals("key1", t1.id);
		assertEquals("string1", t1.stringField);
		assertEquals(true, t1.booleanField);
		assertEquals(3L, t1.longField);
		assertEquals(3.33, t1.doubleField, 0.00001);
		assertEquals(3, t1.doubleArray.length);
		assertEquals(2018, t1.dateField.getYear());
		assertEquals(ByteArray.copyFrom("string1"), t1.bytes);

		assertEquals("key2", t2.id);
		assertEquals("string2", t2.stringField);
		assertEquals(true, t2.booleanField);
		assertEquals(5L, t2.longField);
		assertEquals(5.55, t2.doubleField, 0.00001);
		assertEquals(2, t2.doubleArray.length);
		assertEquals(2019, t2.dateField.getYear());
		assertNull(t2.bytes);
	}

	@Test(expected = IllegalStateException.class)
	public void readUnexpectedColumnTest() {
		Struct struct1 = Struct.newBuilder().add("id", Value.string("key1"))
				.add("custom_col", Value.string("string1"))
				.add("booleanField", Value.bool(true)).add("longField", Value.int64(3L))
				.add("UNEXPECTED_COLUMN", Value.float64(3.33))
				.add("doubleArray", Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.add("dateField", Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.add("timestampField", Value.timestamp(Timestamp.ofTimeMicroseconds(333)))
				.add("bytes", Value.bytes(ByteArray.copyFrom("string1"))).build();

		this.objectMapper.read(TestEntity.class, struct1);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readUnconvertableValueTest() {
		Struct struct1 = Struct.newBuilder().add("id", Value.string("key1"))
				.add("custom_col", Value.string("string1"))
				.add("booleanField", Value.bool(true)).add("longField", Value.int64(3L))
				.add("doubleField", Value.string("UNCONVERTABLE VALUE"))
				.add("doubleArray", Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.add("dateField", Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.add("timestampField", Value.timestamp(Timestamp.ofTimeMicroseconds(333)))
				.add("bytes", Value.bytes(ByteArray.copyFrom("string1"))).build();

		this.objectMapper.read(TestEntity.class, struct1);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void readUnmatachableTypesTest() {
		Struct struct1 = Struct.newBuilder().add("unuseableField", Value.string("key1"))
				.build();
		this.objectMapper.read(FaultyTestEntity.class, struct1);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void writeIncompatibleTypeTest() {
		FaultyTestEntity ft = new FaultyTestEntity();
		ft.unuseableField = new TestEntity();

		WriteBuilder writeBuilder = Mutation.newInsertBuilder("faulty_test_table");
		this.objectMapper.write(ft, writeBuilder);
	}

	@SpannerTable(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@SpannerColumn(name = "custom_col")
		String stringField;

		@SpannerColumn(name = "")
		boolean booleanField;

		long longField;

		double doubleField;

		double[] doubleArray;

		Date dateField;

		Timestamp timestampField;

		ByteArray bytes;
	}

	@SpannerTable(name = "faulty_test_table")
	private static class FaultyTestEntity {
		TestEntity unuseableField;
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
