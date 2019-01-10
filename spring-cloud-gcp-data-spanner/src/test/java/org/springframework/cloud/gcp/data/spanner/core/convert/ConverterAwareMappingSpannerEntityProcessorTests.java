/*
 * Copyright 2017-2018 the original author or authors.
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
import org.assertj.core.data.Offset;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.TestEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for Spanner mapping and converting.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class ConverterAwareMappingSpannerEntityProcessorTests {

	private static final Offset<Double> DELTA = Offset.offset(0.00001);

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

		assertThat(writeConverter.canConvert(javaType, spannerType)).isTrue();
		assertThat(readConverter.canConvert(spannerType, javaType)).isTrue();
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
				.set("custom_col").to(Value.string("WHITE")).set("booleanField")
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
				.to(Value.timestampArray(timestamps))
				.set("commitTimestamp").to(Value.timestamp(Timestamp.ofTimeMicroseconds(1))).build();

		Struct struct2 = Struct.newBuilder().set("id").to(Value.string("key12"))
				.set("id2").to(Value.string("key22")).set("id3").to(Value.string("key32"))
				.set("id4").to(Value.string("key42"))
				.set("custom_col").to(Value.string("BLACK")).set("booleanField")
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
				.to(Value.bytes(ByteArray.copyFrom("string2")))
				.set("commitTimestamp").to(Value.timestamp(Timestamp.ofTimeMicroseconds(1))).build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(struct1, struct2);

		ResultSet results = mock(ResultSet.class);
		when(results.next()).thenAnswer((invocation) -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer((invocation) -> mockResults.getCurrent());

		List<TestEntity> entities = this.spannerEntityProcessor.mapToList(results,
				TestEntity.class);

		verify(results, times(1)).close();

		assertThat(entities).hasSize(2);

		TestEntity t1 = entities.get(0);
		TestEntity t2 = entities.get(1);

		assertThat(t1.id).isEqualTo("key1");
		assertThat(t1.testEmbeddedColumns.id2).isEqualTo("key2");
		assertThat(t1.testEmbeddedColumns.id3).isEqualTo("key3");
		assertThat(t1.id4).isEqualTo("key4");
		assertThat(t1.enumField).isEqualTo(TestEntity.Color.WHITE);
		assertThat(t1.booleanField).isEqualTo(true);
		assertThat(t1.intField).isEqualTo(123);
		assertThat(t1.testEmbeddedColumns.intField2).isEqualTo(333);
		assertThat(t1.longField).isEqualTo(3L);
		assertThat(t1.doubleField).isEqualTo(3.33, DELTA);
		assertThat(t1.doubleArray.length).isEqualTo(3);
		assertThat(t1.dateField.getYear()).isEqualTo(2018);
		assertThat(t1.momentsInTime).isEqualTo(instants);
		assertThat(t1.bytes).isEqualTo(ByteArray.copyFrom("string1"));
		assertThat(t1.commitTimestamp).isEqualTo(Timestamp.ofTimeMicroseconds(1));

		assertThat(t2.id).isEqualTo("key12");
		assertThat(t2.testEmbeddedColumns.id2).isEqualTo("key22");
		assertThat(t2.testEmbeddedColumns.id3).isEqualTo("key32");
		assertThat(t2.id4).isEqualTo("key42");
		assertThat(t2.enumField).isEqualTo(TestEntity.Color.BLACK);
		assertThat(t2.booleanField).isEqualTo(true);
		assertThat(t2.intField).isEqualTo(222);
		assertThat(t2.testEmbeddedColumns.intField2).isEqualTo(555);
		assertThat(t2.longField).isEqualTo(5L);
		assertThat(t2.doubleField).isEqualTo(5.55, DELTA);
		assertThat(t2.doubleArray.length).isEqualTo(2);
		assertThat(t2.dateField.getYear()).isEqualTo(2019);
		assertThat(t2.doubleList).hasSize(1);
		assertThat(t2.doubleList.get(0)).isEqualTo(3.33, DELTA);
		assertThat(t2.momentsInTime).isEqualTo(instants);
		assertThat(t2.stringList).containsExactly("string");
		assertThat(t2.bytes).isEqualTo(ByteArray.copyFrom("string2"));
		assertThat(t2.commitTimestamp).isEqualTo(Timestamp.ofTimeMicroseconds(1));
	}

	@Test
	public void mapToListPartialColumnsTest() {
		List<Double> doubleList = new ArrayList<>();
		doubleList.add(3.33);
		List<String> stringList = new ArrayList<>();
		stringList.add("string");

		Struct struct1 = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("custom_col").to(Value.string("WHITE")).set("doubleList")
				.to(Value.float64Array(doubleList)).set("stringList")
				.to(Value.stringArray(stringList)).build();

		Struct struct2 = Struct.newBuilder().set("id").to(Value.string("key2"))
				.set("custom_col").to(Value.string("BLACK")).set("doubleList")
				.to(Value.float64Array(doubleList)).set("stringList")
				.to(Value.stringArray(stringList)).build();

		MockResults mockResults = new MockResults();
		mockResults.structs = Arrays.asList(struct1, struct2);

		ResultSet results = mock(ResultSet.class);
		when(results.next()).thenAnswer((invocation) -> mockResults.next());
		when(results.getCurrentRowAsStruct())
				.thenAnswer((invocation) -> mockResults.getCurrent());

		List<TestEntity> entities = this.spannerEntityProcessor.mapToList(results,
				TestEntity.class, "id", "custom_col");

		verify(results, times(1)).close();

		assertThat(entities).hasSize(2);

		TestEntity t1 = entities.get(0);
		TestEntity t2 = entities.get(1);

		assertThat(t1.id).isEqualTo("key1");
		assertThat(t1.enumField).isEqualTo(TestEntity.Color.WHITE);

		// This should not have been set
		assertThat(t1.doubleList).isNull();

		assertThat(t2.id).isEqualTo("key2");
		assertThat(t2.enumField).isEqualTo(TestEntity.Color.BLACK);

		// This should not have been set
		assertThat(t2.stringList).isNull();
	}

	private interface SpannerType {
	}

	private interface JavaType {
	}

	/**
	 * A mock results class for mocked queries.
	 */
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
