/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.Arrays;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.StructField;
import com.google.cloud.spanner.Value;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.FaultyTestEntity;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.OuterTestEntity;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.OuterTestEntityFlat;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.OuterTestEntityFlatFaulty;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.OuterTestHoldingStructEntity;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.OuterTestHoldingStructsEntity;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.TestEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for converting and reading Spanner entities and objects.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class ConverterAwareMappingSpannerEntityReaderTests {

	/**
	 * used for checking exception messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private SpannerEntityReader spannerEntityReader;

	private SpannerReadConverter spannerReadConverter;

	@Before
	public void setup() {
		this.spannerReadConverter = new SpannerReadConverter();
		this.spannerEntityReader = new ConverterAwareMappingSpannerEntityReader(
				new SpannerMappingContext(), this.spannerReadConverter);
	}

	@Test
	public void readNestedStructTest() {
		Struct innerStruct = Struct.newBuilder().set("value")
				.to(Value.string("inner-value")).build();
		Struct outerStruct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("innerTestEntities")
				.toStructArray(Type.struct(StructField.of("value", Type.string())),
						Arrays.asList(innerStruct))
				.build();

		OuterTestEntity result = this.spannerEntityReader.read(OuterTestEntity.class,
				outerStruct, null, true);
		assertThat(result.id).isEqualTo("key1");
		assertThat(result.innerTestEntities).hasSize(1);
		assertThat(result.innerTestEntities.get(0).value).isEqualTo("inner-value");
		assertThat(result.innerTestEntities.get(0).missingColumnValue).isNull();
	}

	@Test
	public void readNestedStructsAsStructsTest() {
		Struct innerStruct = Struct.newBuilder().set("value")
				.to(Value.string("inner-value")).build();
		Struct outerStruct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("innerStructs")
				.toStructArray(Type.struct(StructField.of("value", Type.string())),
						Arrays.asList(innerStruct))
				.build();

		OuterTestHoldingStructsEntity result = this.spannerEntityReader
				.read(OuterTestHoldingStructsEntity.class, outerStruct);
		assertThat(result.id).isEqualTo("key1");
		assertThat(result.innerStructs).hasSize(1);
		assertThat(result.innerStructs.get(0).getString("value")).isEqualTo("inner-value");
	}

	@Test
	public void readNestedStructAsStructTest() {
		Struct innerStruct = Struct.newBuilder().set("value")
				.to(Value.string("inner-value")).build();
		Struct outerStruct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("innerStruct").to(innerStruct).build();

		OuterTestHoldingStructEntity result = this.spannerEntityReader
				.read(OuterTestHoldingStructEntity.class, outerStruct);
		assertThat(result.id).isEqualTo("key1");
		assertThat(result.innerStruct.getString("value")).isEqualTo("inner-value");
	}

	@Test
	public void readArraySingularMismatchTest() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("Column is not an ARRAY type: innerTestEntities");

		Struct rowStruct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("innerTestEntities").to(Value.int64(3)).build();
		this.spannerEntityReader.read(OuterTestEntity.class, rowStruct);
	}

	@Test
	public void readSingularArrayMismatchTest() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("The value in column with name innerLengths could not be converted " +
				"to the corresponding property in the entity. The property's type is class java.lang.Integer.");

		Struct colStruct = Struct.newBuilder().set("string_col").to(Value.string("value"))
				.build();
		Struct rowStruct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("innerLengths")
				.toStructArray(Type.struct(StructField.of("string_col", Type.string())),
						Arrays.asList(colStruct))
				.build();

		new ConverterAwareMappingSpannerEntityReader(new SpannerMappingContext(),
				new SpannerReadConverter(Arrays.asList(new Converter<Struct, Integer>() {
					@Nullable
					@Override
					public Integer convert(Struct source) {
						return source.getString("string_col").length();
					}
				}))).read(OuterTestEntityFlatFaulty.class, rowStruct);
	}

	@Test
	public void readConvertedNestedStructTest() {
		Struct colStruct = Struct.newBuilder().set("string_col").to(Value.string("value"))
				.build();
		Struct rowStruct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("innerLengths")
				.toStructArray(Type.struct(StructField.of("string_col", Type.string())),
						Arrays.asList(colStruct))
				.build();

		OuterTestEntityFlat result = new ConverterAwareMappingSpannerEntityReader(
				new SpannerMappingContext(),
				new SpannerReadConverter(Arrays.asList(new Converter<Struct, Integer>() {
					@Nullable
					@Override
					public Integer convert(Struct source) {
						return source.getString("string_col").length();
					}
				}))).read(OuterTestEntityFlat.class, rowStruct);
		assertThat(result.id).isEqualTo("key1");
		assertThat(result.innerLengths).hasSize(1);
		assertThat(result.innerLengths.get(0)).isEqualTo(5);
	}

	@Test
	public void readNotFoundColumnTest() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("Unable to read column from Cloud Spanner results: id4");
		Struct struct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("custom_col").to(Value.string("string1")).set("booleanField")
				.to(Value.bool(true)).set("longField").to(Value.int64(3L))
				.set("doubleArray")
				.to(Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.set("dateField").to(Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.set("timestampField")
				.to(Value.timestamp(Timestamp.ofTimeMicroseconds(333))).set("bytes")
				.to(Value.bytes(ByteArray.copyFrom("string1"))).build();

		this.spannerEntityReader.read(TestEntity.class, struct);
	}

	@Test
	public void readUnconvertableValueTest() {
		this.expectedEx.expect(ConversionFailedException.class);
		this.expectedEx.expectMessage("Failed to convert from type [java.lang.String] to type " +
				"[java.lang.Double] for value 'UNCONVERTABLE VALUE'; nested exception is " +
				"java.lang.NumberFormatException: For input string: \"UNCONVERTABLEVALUE\"");
		Struct struct = Struct.newBuilder().set("id").to(Value.string("key1")).set("id2")
				.to(Value.string("key2")).set("id3").to(Value.string("key3")).set("id4")
				.to(Value.string("key4")).set("intField2").to(Value.int64(333L))
				.set("custom_col").to(Value.string("WHITE")).set("booleanField")
				.to(Value.bool(true)).set("longField").to(Value.int64(3L))
				.set("doubleField").to(Value.string("UNCONVERTABLE VALUE"))
				.set("doubleArray")
				.to(Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.set("dateField").to(Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.set("timestampField")
				.to(Value.timestamp(Timestamp.ofTimeMicroseconds(333))).set("bytes")
				.to(Value.bytes(ByteArray.copyFrom("string1"))).build();

		this.spannerEntityReader.read(TestEntity.class, struct);
	}

	@Test
	public void readUnmatachableTypesTest() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("Unable to read column from Cloud Spanner results: id");
		Struct struct = Struct.newBuilder().set("fieldWithUnsupportedType")
				.to(Value.string("key1")).build();
		this.spannerEntityReader.read(FaultyTestEntity.class, struct);
	}

	@Test
	public void shouldReadEntityWithNoDefaultConstructor() {
		Struct row = Struct.newBuilder().set("id").to(Value.string("1234")).build();
		TestEntities.SimpleConstructorTester result = this.spannerEntityReader
				.read(TestEntities.SimpleConstructorTester.class, row);

		assertThat(result.id).isEqualTo("1234");
	}

	@Test
	public void readNestedStructWithConstructor() {
		Struct innerStruct = Struct.newBuilder().set("value").to(Value.string("value"))
				.build();
		Struct outerStruct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("innerTestEntities")
				.toStructArray(Type.struct(StructField.of("value", Type.string())),
						Arrays.asList(innerStruct))
				.build();

		TestEntities.OuterTestEntityWithConstructor result = this.spannerEntityReader
				.read(TestEntities.OuterTestEntityWithConstructor.class, outerStruct, null, true);
		assertThat(result.id).isEqualTo("key1");
		assertThat(result.innerTestEntities).hasSize(1);
		assertThat(result.innerTestEntities.get(0).value).isEqualTo("value");
	}

	@Test
	public void testPartialConstructor() {
		Struct struct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("custom_col").to(Value.string("string1")).set("booleanField")
				.to(Value.bool(true)).set("longField").to(Value.int64(3L))
				.set("doubleField").to(Value.float64(3.14)).build();

		this.spannerEntityReader.read(TestEntities.PartialConstructor.class, struct);
	}

	@Test
	public void ensureConstructorArgsAreReadOnce() {
		Struct row = mock(Struct.class);
		when(row.getString("id")).thenReturn("1234");
		when(row.getType()).thenReturn(
				Type.struct(Arrays.asList(Type.StructField.of("id", Type.string()))));
		when(row.getColumnType("id")).thenReturn(Type.string());

		TestEntities.SimpleConstructorTester result = this.spannerEntityReader
				.read(TestEntities.SimpleConstructorTester.class, row);

		assertThat(result.id).isEqualTo("1234");
		verify(row, times(1)).getString("id");
	}

	@Test
	public void testPartialConstructorWithNotEnoughArgs() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("Column not found: custom_col");
		Struct struct = Struct.newBuilder().set("id").to(Value.string("key1"))
				.set("booleanField").to(Value.bool(true)).set("longField")
				.to(Value.int64(3L)).set("doubleField").to(Value.float64(3.14)).build();

		this.spannerEntityReader.read(TestEntities.PartialConstructor.class, struct);
	}

	@Test
	public void zeroArgsListShouldThrowError() {
		this.expectedEx.expect(SpannerDataException.class);
		this.expectedEx.expectMessage("in field 'zeroArgsListOfObjects': Unsupported number of " +
				"type parameters found: 0 Only collections of exactly 1 type parameter are supported.");
		Struct struct = Struct.newBuilder().set("zeroArgsListOfObjects")
				.to(Value.stringArray(Arrays.asList("hello", "world"))).build();
		this.spannerEntityReader
				.read(TestEntities.TestEntityWithListWithZeroTypeArgs.class, struct);
	}

}
