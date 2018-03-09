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

import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumn;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerColumnInnerType;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerTable;
import org.springframework.data.annotation.Id;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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

	private SpannerConverter objectMapper;

	@Before
	public void setUp() {
		this.objectMapper = new MappingSpannerConverter(new SpannerMappingContext());
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
		t.doubleList = new ArrayList<>();
		t.doubleList.add(3.33);
		t.stringList = new ArrayList<>();
		t.stringList.add("stringstringstring");
		t.dateField = Date.fromYearMonthDay(2018, 11, 22);
		t.timestampField = Timestamp.ofTimeMicroseconds(333);
		t.bytes = ByteArray.copyFrom("333");
		t.booleanList = new ArrayList<>();
		t.booleanList.add(t.booleanField);
		t.longList = new ArrayList<>();
		t.longList.add(t.longField);
		t.dateList = new ArrayList<>();
		t.dateList.add(t.dateField);
		t.timestampList = new ArrayList<>();
		t.timestampList.add(t.timestampField);
		t.bytesList = new ArrayList<>();
		t.bytesList.add(t.bytes);

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

		ValueBinder<WriteBuilder> doubleListFieldBinder = mock(ValueBinder.class);
		when(doubleListFieldBinder.toFloat64Array((Iterable<Double>) any()))
				.thenReturn(null);
		when(writeBuilder.set(eq("doubleList"))).thenReturn(doubleListFieldBinder);

		ValueBinder<WriteBuilder> stringListFieldBinder = mock(ValueBinder.class);
		when(stringListFieldBinder.toStringArray(any())).thenReturn(null);
		when(writeBuilder.set(eq("stringList"))).thenReturn(stringListFieldBinder);

		ValueBinder<WriteBuilder> booleanListFieldBinder = mock(ValueBinder.class);
		when(booleanListFieldBinder.toBoolArray((Iterable<Boolean>) any()))
				.thenReturn(null);
		when(writeBuilder.set(eq("booleanList"))).thenReturn(booleanListFieldBinder);

		ValueBinder<WriteBuilder> longListFieldBinder = mock(ValueBinder.class);
		when(longListFieldBinder.toInt64Array((Iterable<Long>) any())).thenReturn(null);
		when(writeBuilder.set(eq("longList"))).thenReturn(longListFieldBinder);

		ValueBinder<WriteBuilder> timestampListFieldBinder = mock(ValueBinder.class);
		when(timestampListFieldBinder.toTimestampArray(any())).thenReturn(null);
		when(writeBuilder.set(eq("timestampList"))).thenReturn(timestampListFieldBinder);

		ValueBinder<WriteBuilder> dateListFieldBinder = mock(ValueBinder.class);
		when(dateListFieldBinder.toDateArray(any())).thenReturn(null);
		when(writeBuilder.set(eq("dateList"))).thenReturn(dateListFieldBinder);

		ValueBinder<WriteBuilder> bytesListFieldBinder = mock(ValueBinder.class);
		when(bytesListFieldBinder.toDateArray(any())).thenReturn(null);
		when(writeBuilder.set(eq("bytesList"))).thenReturn(bytesListFieldBinder);

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
		verify(doubleListFieldBinder, times(1)).toFloat64Array(eq(t.doubleList));
		verify(stringListFieldBinder, times(1)).toStringArray(eq(t.stringList));
		verify(booleanListFieldBinder, times(1)).toBoolArray(eq(t.booleanList));
		verify(longListFieldBinder, times(1)).toInt64Array(eq(t.longList));
		verify(timestampListFieldBinder, times(1)).toTimestampArray(eq(t.timestampList));
		verify(dateListFieldBinder, times(1)).toDateArray(eq(t.dateList));
		verify(bytesListFieldBinder, times(1)).toBytesArray(eq(t.bytesList));
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

		List<Double> doubleList = new ArrayList<>();
		doubleList.add(3.33);
		List<String> stringList = new ArrayList<>();
		stringList.add("string");

		Struct struct2 = Struct.newBuilder().add("id", Value.string("key2"))
				.add("custom_col", Value.string("string2"))
				.add("booleanField", Value.bool(true)).add("longField", Value.int64(5L))
				.add("doubleField", Value.float64(5.55))
				.add("doubleArray", Value.float64Array(new double[] { 5.55, 5.55 }))
				.add("doubleList", Value.float64Array(doubleList))
				.add("stringList", Value.stringArray(stringList))
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
		assertEquals(1, t2.doubleList.size());
		assertEquals(3.33, t2.doubleList.get(0), 0.000001);
		assertThat(t2.stringList, containsInAnyOrder("string"));
		assertNull(t2.bytes);
	}

	@Test(expected = SpannerDataException.class)
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

	@Test(expected = IllegalStateException.class)
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

	@Test(expected = SpannerDataException.class)
	public void writeUnannotatedIterableTest() {
		FaultyTestEntity ft = new FaultyTestEntity();
		ft.doubleList = new ArrayList<>();
		WriteBuilder writeBuilder = Mutation.newInsertBuilder("faulty_test_table");
		this.objectMapper.write(ft, writeBuilder);
	}

	@Test(expected = SpannerDataException.class)
	public void writeUnsupportedTypeIterableTest() {
		FaultyTestEntity2 ft = new FaultyTestEntity2();
		ft.listWithUnsupportedInnerType = new ArrayList<TestEntity>();
		WriteBuilder writeBuilder = Mutation.newInsertBuilder("faulty_test_table_2");
		this.objectMapper.write(ft, writeBuilder);
	}

	@Test(expected = SpannerDataException.class)
	public void readUnmatachableTypesTest() {
		Struct struct1 = Struct.newBuilder().add("fieldWithUnsupportedType", Value.string("key1"))
				.build();
		this.objectMapper.read(FaultyTestEntity.class, struct1);
	}

	@Test(expected = SpannerDataException.class)
	public void writeIncompatibleTypeTest() {
		FaultyTestEntity ft = new FaultyTestEntity();
		ft.fieldWithUnsupportedType = new TestEntity();
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

		@SpannerColumnInnerType(innerType = Double.class)
		List<Double> doubleList;

		@SpannerColumnInnerType(innerType = String.class)
		List<String> stringList;

		@SpannerColumnInnerType(innerType = Boolean.class)
		List<Boolean> booleanList;

		@SpannerColumnInnerType(innerType = Long.class)
		List<Long> longList;

		@SpannerColumnInnerType(innerType = Timestamp.class)
		List<Timestamp> timestampList;

		@SpannerColumnInnerType(innerType = Date.class)
		List<Date> dateList;

		@SpannerColumnInnerType(innerType = ByteArray.class)
		List<ByteArray> bytesList;

		Date dateField;

		Timestamp timestampField;

		ByteArray bytes;
	}

	@SpannerTable(name = "faulty_test_table")
	private static class FaultyTestEntity {
		TestEntity fieldWithUnsupportedType;

		List<Double> doubleList;
	}

	@SpannerTable(name = "faulty_test_table_2")
	private static class FaultyTestEntity2 {

		@SpannerColumnInnerType(innerType = TestEntity.class)
		List<TestEntity> listWithUnsupportedInnerType;
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
