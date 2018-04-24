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

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Value;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.FaultyTestEntity;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.OuterTestEntity;
import org.springframework.cloud.gcp.data.spanner.core.convert.TestEntities.TestEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.CustomConversions.StoreConversions;

import static org.junit.Assert.assertEquals;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class MappingSpannerReadConverterTest {

	private SpannerEntityReader readConverter;

	@Before
	public void setup() {
		this.readConverter = new MappingSpannerReadConverter(new SpannerMappingContext(),
				new CustomConversions(StoreConversions.NONE,
						SpannerConverters.DEFAULT_SPANNER_READ_CONVERTERS));
	}

	@Test
	public void readNestedStructTest() {
		Struct innerStruct = Struct.newBuilder().add("value", Value.string("value")).build();
		Struct outerStruct = Struct.newBuilder().add("id", Value.string("key1"))
				.add("innerTestEntities",
						ImmutableList.of(Type.StructField.of("value", Type.string())),
						ImmutableList.of(innerStruct))
				.build();

		OuterTestEntity result = this.readConverter.read(OuterTestEntity.class, outerStruct);
		assertEquals("key1", result.id);
		assertEquals(1, result.innerTestEntities.size());
		assertEquals("value", result.innerTestEntities.get(0).value);
	}

	@Test(expected = SpannerDataException.class)
	public void readNotFoundColumnTest() {
		Struct struct1 = Struct.newBuilder().add("id", Value.string("key1"))
				.add("custom_col", Value.string("string1"))
				.add("booleanField", Value.bool(true)).add("longField", Value.int64(3L))
				.add("doubleArray", Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.add("dateField", Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.add("timestampField", Value.timestamp(Timestamp.ofTimeMicroseconds(333)))
				.add("bytes", Value.bytes(ByteArray.copyFrom("string1"))).build();

		this.readConverter.read(TestEntity.class, struct1);
	}

	@Test(expected = ConversionFailedException.class)
	public void readUnconvertableValueTest() {
		Struct struct1 = Struct.newBuilder().add("id", Value.string("key1"))
				.add("custom_col", Value.string("string1"))
				.add("booleanField", Value.bool(true)).add("longField", Value.int64(3L))
				.add("doubleField", Value.string("UNCONVERTABLE VALUE"))
				.add("doubleArray", Value.float64Array(new double[] { 3.33, 3.33, 3.33 }))
				.add("dateField", Value.date(Date.fromYearMonthDay(2018, 11, 22)))
				.add("timestampField", Value.timestamp(Timestamp.ofTimeMicroseconds(333)))
				.add("bytes", Value.bytes(ByteArray.copyFrom("string1"))).build();

		this.readConverter.read(TestEntity.class, struct1);
	}

	@Test(expected = SpannerDataException.class)
	public void readUnmatachableTypesTest() {
		Struct struct1 = Struct.newBuilder()
				.add("fieldWithUnsupportedType", Value.string("key1")).build();
		this.readConverter.read(FaultyTestEntity.class, struct1);
	}

	@Test(expected = SpannerDataException.class)
	public void zeroArgsListShouldThrowError() {
		Struct struct1 = Struct.newBuilder()
				.add("zeroArgsListOfObjects", Value.stringArray(ImmutableList.of("hello", "world"))).build();
		this.readConverter.read(TestEntities.TestEntityWithListWithZeroTypeArgs.class, struct1);
	}

}
