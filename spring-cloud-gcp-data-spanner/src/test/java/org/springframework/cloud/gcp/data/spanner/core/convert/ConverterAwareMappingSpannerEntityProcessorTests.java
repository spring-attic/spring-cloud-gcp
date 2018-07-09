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
import java.util.List;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.TestEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class ConverterAwareMappingSpannerEntityProcessorTests {

	private static final Converter<SpannerType, JavaType> SPANNER_TO_JAVA = new Converter<SpannerType, JavaType>() {
		@Nullable
		@Override
		public JavaType convert(SpannerType source) {
			return new JavaType() {
			};
		}
	};

	private static final Converter<JavaType, SpannerType> JAVA_TO_SPANNER = new Converter<JavaType, SpannerType>() {
		@Nullable
		@Override
		public SpannerType convert(JavaType source) {
			return new SpannerType() {
			};
		}
	};

	private SpannerEntityProcessor spannerEntityProcessor;

	@Before
	public void setUp() {
		this.spannerEntityProcessor = new ConverterAwareMappingSpannerEntityProcessor(
				new SpannerMappingContext());
	}

	@Test
	public void canConvertDefaultTypesNoCustomConverters() {
		ConverterAwareMappingSpannerEntityProcessor converter = new ConverterAwareMappingSpannerEntityProcessor(
				new SpannerMappingContext());

		verifyCanConvert(converter, java.util.Date.class, Date.class);
		verifyCanConvert(converter, Instant.class, Timestamp.class);
	}

	@Test
	public void canConvertDefaultTypesCustomConverters() {
		ConverterAwareMappingSpannerEntityProcessor converter = new ConverterAwareMappingSpannerEntityProcessor(
				new SpannerMappingContext(), Arrays.asList(JAVA_TO_SPANNER),
				Arrays.asList(SPANNER_TO_JAVA));

		verifyCanConvert(converter, java.util.Date.class, Date.class);
		verifyCanConvert(converter, Instant.class, Timestamp.class);
		verifyCanConvert(converter, JavaType.class, SpannerType.class);
	}

	private void verifyCanConvert(ConverterAwareMappingSpannerEntityProcessor converter,
			Class javaType, Class spannerType) {
		SpannerWriteConverter writeConverter = converter.getWriteConverter();
		SpannerReadConverter readConverter = converter.getReadConverter();

		assertTrue(writeConverter.canConvert(javaType, spannerType));
		assertTrue(readConverter.canConvert(spannerType, javaType));
	}

	@Test
	public void mapToListTest() {
		List<Double> doubleList = new ArrayList<>();
		doubleList.add(3.33);
		List<String> stringList = new ArrayList<>();
		stringList.add("string");

		Instant i1 = Instant.ofEpochSecond(111);
		Instant i2 = Instant.ofEpochSecond(222);
		Instant i3 = Instant.ofEpochSecond(333);
		List<Instant> instants = new ArrayList<>();
		instants.add(i1);
		instants.add(i2);
		instants.add(i3);

		Timestamp ts1 = Timestamp.ofTimeSecondsAndNanos(111, 0);
		Timestamp ts2 = Timestamp.ofTimeSecondsAndNanos(222, 0);
		Timestamp ts3 = Timestamp.ofTimeSecondsAndNanos(333, 0);
		List<Timestamp> timestamps = new ArrayList<>();
		timestamps.add(ts1);
		timestamps.add(ts2);
		timestamps.add(ts3);

		Struct struct1 = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("id2").to(Value.string("key2")).set("id3").to(Value.string("key3"))
				.set("id4").to(Value.string("key4"))
				.set("custom_col").to(Value.string("string1")).set("booleanField")
				.to(Value.bool(true)).set("intField").to(Value.int64(123L))
				.set("intField2").to(Value.int64(333L))
				.set("longField").to(Value.int64(3L)).set("doubleField")
				.to(Value.float64(3.33)).set("doubleArray")
				.to(Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.set("doubleList").to(Value.float64Array(doubleList)).set("stringList")
				.to(Value.stringArray(stringList)).set("booleanList")
				.to(Value.boolArray(new boolean[] {})).set("longList")
				.to(Value.int64Array(new long[] {})).set("timestampList")
				.to(Value.timestampArray(new ArrayList<>())).set("dateList")
				.to(Value.dateArray(new ArrayList<>())).set("bytesList")
				.to(Value.bytesArray(new ArrayList<>())).set("dateField")
				.to(Value.date(Date.fromYearMonthDay(2018, 11, 22))).set("timestampField")
				.to(Value.timestamp(Timestamp.ofTimeMicroseconds(333))).set("bytes")
				.to(Value.bytes(ByteArray.copyFrom("string1"))).set("momentsInTime")
				.to(Value.timestampArray(timestamps)).build();

		Struct struct2 = Struct.newBuilder().set("id").to(Value.string("key12"))
				.set("id2").to(Value.string("key22")).set("id3").to(Value.string("key32"))
				.set("id4").to(Value.string("key42"))
				.set("custom_col").to(Value.string("string2")).set("booleanField")
				.to(Value.bool(true)).set("intField").to(Value.int64(222L))
				.set("intField2").to(Value.int64(555L))
				.set("longField").to(Value.int64(5L)).set("doubleField")
				.to(Value.float64(5.55)).set("doubleArray")
				.to(Value.float64Array(new double[] { 5.55, 5.55 })).set("doubleList")
				.to(Value.float64Array(doubleList)).set("stringList")
				.to(Value.stringArray(stringList)).set("booleanList")
				.to(Value.boolArray(new boolean[] {})).set("longList")
				.to(Value.int64Array(new long[] {})).set("timestampList")
				.to(Value.timestampArray(new ArrayList<>())).set("dateList")
				.to(Value.dateArray(new ArrayList<>())).set("bytesList")
				.to(Value.bytesArray(new ArrayList<>())).set("dateField")
				.to(Value.date(Date.fromYearMonthDay(2019, 11, 22))).set("timestampField")
				.to(Value.timestamp(Timestamp.ofTimeMicroseconds(555)))
				.set("momentsInTime").to(Value.timestampArray(timestamps)).set("bytes")
				.to(Value.bytes(ByteArray.copyFrom("string2"))).build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(struct1, struct2);

		ResultSet results = mock(ResultSet.class);
		when(results.next()).thenAnswer(invocation -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer(invocation -> mockResults.getCurrent());

		List<TestEntity> entities = this.spannerEntityProcessor.mapToList(results,
				TestEntity.class);

		verify(results, times(1)).close();

		assertEquals(2, entities.size());

		TestEntity t1 = entities.get(0);
		TestEntity t2 = entities.get(1);

		assertEquals("key1", t1.id);
		assertEquals("key2", t1.testEmbeddedColumns.id2);
		assertEquals("key3", t1.testEmbeddedColumns.id3);
		assertEquals("key4", t1.id4);
		assertEquals("string1", t1.stringField);
		assertEquals(true, t1.booleanField);
		assertEquals(123, t1.intField);
		assertEquals(333, t1.testEmbeddedColumns.intField2);
		assertEquals(3L, t1.longField);
		assertEquals(3.33, t1.doubleField, 0.00001);
		assertEquals(3, t1.doubleArray.length);
		assertEquals(2018, t1.dateField.getYear());
		assertEquals(instants, t1.momentsInTime);
		assertEquals(ByteArray.copyFrom("string1"), t1.bytes);

		assertEquals("key12", t2.id);
		assertEquals("key22", t2.testEmbeddedColumns.id2);
		assertEquals("key32", t2.testEmbeddedColumns.id3);
		assertEquals("key42", t2.id4);
		assertEquals("string2", t2.stringField);
		assertEquals(true, t2.booleanField);
		assertEquals(222, t2.intField);
		assertEquals(555, t2.testEmbeddedColumns.intField2);
		assertEquals(5L, t2.longField);
		assertEquals(5.55, t2.doubleField, 0.00001);
		assertEquals(2, t2.doubleArray.length);
		assertEquals(2019, t2.dateField.getYear());
		assertEquals(1, t2.doubleList.size());
		assertEquals(3.33, t2.doubleList.get(0), 0.000001);
		assertEquals(instants, t2.momentsInTime);
		assertThat(t2.stringList, containsInAnyOrder("string"));
		assertEquals(ByteArray.copyFrom("string2"), t2.bytes);
	}

	@Test
	public void mapToListPartialColumnsTest() {
		List<Double> doubleList = new ArrayList<>();
		doubleList.add(3.33);
		List<String> stringList = new ArrayList<>();
		stringList.add("string");

		Struct struct1 = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("custom_col").to(Value.string("string1")).set("doubleList")
				.to(Value.float64Array(doubleList)).set("stringList")
				.to(Value.stringArray(stringList)).build();

		Struct struct2 = Struct.newBuilder().set("id").to(Value.string("key2"))
				.set("custom_col").to(Value.string("string2")).set("doubleList")
				.to(Value.float64Array(doubleList)).set("stringList")
				.to(Value.stringArray(stringList)).build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(struct1, struct2);

		ResultSet results = mock(ResultSet.class);
		when(results.next()).thenAnswer(invocation -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer(invocation -> mockResults.getCurrent());

		List<TestEntity> entities = this.spannerEntityProcessor.mapToList(results,
				TestEntity.class, "id", "custom_col");

		verify(results, times(1)).close();

		assertEquals(2, entities.size());

		TestEntity t1 = entities.get(0);
		TestEntity t2 = entities.get(1);

		assertEquals("key1", t1.id);
		assertEquals("string1", t1.stringField);

		// This should not have been set
		assertNull(t1.doubleList);

		assertEquals("key2", t2.id);
		assertEquals("string2", t2.stringField);

		// This should not have been set
		assertNull(t2.stringList);
	}

	private interface SpannerType {
	}

	private interface JavaType {
	}

	static class MockResults {
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
