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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Mutation;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ValueBinder;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.FaultyTestEntity;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.FaultyTestEntity2;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.TestEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.CustomConversions.StoreConversions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class MappingSpannerWriteConverterTest {

	private SpannerEntityWriter writeConverter;

	@Before
	public void setup() {
		this.writeConverter = new MappingSpannerWriteConverter(new SpannerMappingContext(),
				new CustomConversions(StoreConversions.NONE,
						SpannerConverters.DEFAULT_SPANNER_WRITE_CONVERTERS));
	}

	@Test
	public void writeTest() {
		TestEntity t = new TestEntity();
		t.id = "key1";
		t.stringField = "string";
		t.booleanField = true;
		t.intField = 123;
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

		Instant i1 = Instant.ofEpochSecond(111);
		Instant i2 = Instant.ofEpochSecond(222);
		Instant i3 = Instant.ofEpochSecond(333);
		t.momentsInTime = new ArrayList<>();
		t.momentsInTime.add(i1);
		t.momentsInTime.add(i2);
		t.momentsInTime.add(i3);

		Timestamp t1 = Timestamp.ofTimeSecondsAndNanos(111, 0);
		Timestamp t2 = Timestamp.ofTimeSecondsAndNanos(222, 0);
		Timestamp t3 = Timestamp.ofTimeSecondsAndNanos(333, 0);
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(t1);
		timestamps.add(t2);
		timestamps.add(t3);

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

		ValueBinder<WriteBuilder> intFieldBinder = mock(ValueBinder.class);
		when(intFieldBinder.to(anyLong())).thenReturn(null);
		when(writeBuilder.set(eq("intField"))).thenReturn(intFieldBinder);

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

		ValueBinder<WriteBuilder> instantListFieldBinder = mock(ValueBinder.class);
		when(instantListFieldBinder.toTimestampArray(any())).thenReturn(null);
		when(writeBuilder.set(eq("momentsInTime"))).thenReturn(instantListFieldBinder);

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

		this.writeConverter.write(t, writeBuilder);

		verify(idBinder, times(1)).to(eq(t.id));
		verify(stringFieldBinder, times(1)).to(eq(t.stringField));
		verify(booleanFieldBinder, times(1)).to(eq(Boolean.valueOf(t.booleanField)));
		verify(intFieldBinder, times(1)).to(eq(Long.valueOf(t.intField)));
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
		verify(instantListFieldBinder, times(1)).toTimestampArray(eq(timestamps));
	}

	@Test
	public void writeSomeColumnsTest() throws ClassNotFoundException {
		TestEntity t = new TestEntity();
		t.id = "key1";
		t.stringField = "string";

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

		this.writeConverter.write(t, writeBuilder,
				new HashSet<>(Arrays.asList(new String[] { "id", "custom_col" })));

		verify(idBinder, times(1)).to(eq(t.id));
		verify(stringFieldBinder, times(1)).to(eq(t.stringField));
		verifyZeroInteractions(booleanFieldBinder);
	}

	@Test(expected = SpannerDataException.class)
	public void writeUnsupportedTypeIterableTest() {
		FaultyTestEntity2 ft = new FaultyTestEntity2();
		ft.listWithUnsupportedInnerType = new ArrayList<TestEntity>();
		WriteBuilder writeBuilder = Mutation.newInsertBuilder("faulty_test_table_2");
		this.writeConverter.write(ft, writeBuilder);
	}

	@Test(expected = SpannerDataException.class)
	public void writeIncompatibleTypeTest() {
		FaultyTestEntity ft = new FaultyTestEntity();
		ft.fieldWithUnsupportedType = new TestEntity();
		WriteBuilder writeBuilder = Mutation.newInsertBuilder("faulty_test_table");
		this.writeConverter.write(ft, writeBuilder);
	}

}
