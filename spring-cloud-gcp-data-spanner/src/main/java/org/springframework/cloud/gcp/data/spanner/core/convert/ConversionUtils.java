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

import java.lang.reflect.Array;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.ByteArray;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class ConversionUtils {

	// @formatter:off
	public static final Converter<java.sql.Date, com.google.cloud.Date>
			JAVA_SQL_TO_SPANNER_DATE_CONVERTER =
			new Converter<java.sql.Date, com.google.cloud.Date>() {
				// @formatter:on
				@Nullable
				@Override
				public com.google.cloud.Date convert(java.sql.Date date) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);
					return com.google.cloud.Date.fromYearMonthDay(
							cal.get(Calendar.YEAR),
							cal.get(Calendar.MONTH) + 1,
							cal.get(Calendar.DAY_OF_MONTH));
				}
			};

	// @formatter:off
	public static final Converter<com.google.cloud.Date, java.sql.Date>
			SPANNER_TO_JAVA_SQL_DATE_CONVERTER =
			new Converter<com.google.cloud.Date, java.sql.Date>() {
				// @formatter:on
				@Nullable
				@Override
				public java.sql.Date convert(com.google.cloud.Date date) {
					Calendar cal = Calendar.getInstance();
					cal.set(date.getYear(), date.getMonth() - 1, date.getDayOfMonth());
					return new java.sql.Date(cal.getTimeInMillis());
				}
			};

	// @formatter:off
	public static final Converter<Date, com.google.cloud.Date>
					JAVA_TO_SPANNER_DATE_CONVERTER = new Converter<Date, com.google.cloud.Date>() {
		// @formatter:on
		@Nullable
		@Override
		public com.google.cloud.Date convert(Date date) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			return com.google.cloud.Date.fromYearMonthDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
					cal.get(Calendar.DAY_OF_MONTH));
		}
	};

	// @formatter:off
	public static final Converter<com.google.cloud.Date, Date> SPANNER_TO_JAVA_DATE_CONVERTER =
			new Converter<com.google.cloud.Date, Date>() {
				// @formatter:on
				@Nullable
				@Override
				public Date convert(com.google.cloud.Date date) {
					Calendar cal = Calendar.getInstance();
					cal.set(date.getYear(), date.getMonth() - 1, date.getDayOfMonth());
					return cal.getTime();
				}
			};

	// @formatter:off
	public static final Converter<Instant, Timestamp> INSTANT_TIMESTAMP_CONVERTER =
			new Converter<Instant, Timestamp>() {
				// @formatter:on
				@Nullable
				@Override
				public Timestamp convert(Instant instant) {
					return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
				}
			};

	// @formatter:off
	public static final Converter<Timestamp, Instant> TIMESTAMP_INSTANT_CONVERTER =
			new Converter<Timestamp, Instant>() {
				// @formatter:on
				@Nullable
				@Override
				public Instant convert(Timestamp timestamp) {
					return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
				}
			};

	// @formatter:off
	public static final Converter<java.sql.Timestamp, Timestamp> JAVA_TO_SPANNER_TIMESTAMP_CONVERTER =
			new Converter<java.sql.Timestamp, Timestamp>() {
				// @formatter:on
				@Nullable
				@Override
				public Timestamp convert(java.sql.Timestamp timestamp) {
					return Timestamp.of(timestamp);
				}
			};

	// @formatter:off
	public static final Converter<Timestamp, java.sql.Timestamp> SPANNER_TO_JAVA_TIMESTAMP_CONVERTER =
			new Converter<Timestamp, java.sql.Timestamp>() {
				// @formatter:on
				@Nullable
				@Override
				public java.sql.Timestamp convert(Timestamp timestamp) {
					return java.sql.Timestamp.from(TIMESTAMP_INSTANT_CONVERTER.convert(timestamp));
				}
			};

	// @formatter:off
	public static final Converter<byte[], ByteArray> JAVA_TO_SPANNER_BYTE_ARRAY_CONVERTER =
			new Converter<byte[], ByteArray>() {
				// @formatter:on
				@Nullable
				@Override
				public ByteArray convert(byte[] bytes) {
					return ByteArray.copyFrom(bytes);
				}
			};

	// @formatter:off
	public static final Converter<ByteArray, byte[]> SPANNER_TO_JAVA_BYTE_ARRAY_CONVERTER =
			new Converter<ByteArray, byte[]>() {
				// @formatter:on
				@Nullable
				@Override
				public byte[] convert(ByteArray bytes) {
					return bytes.toByteArray();
				}
			};

	/** Converters from common types to those used by Spanner. */
	public static final Collection<Converter> DEFAULT_SPANNER_WRITE_CONVERTERS = ImmutableSet.of(
			JAVA_TO_SPANNER_DATE_CONVERTER,
			INSTANT_TIMESTAMP_CONVERTER,
			JAVA_TO_SPANNER_BYTE_ARRAY_CONVERTER,
			JAVA_TO_SPANNER_TIMESTAMP_CONVERTER,
			JAVA_SQL_TO_SPANNER_DATE_CONVERTER);

	/** Converters from common types to those used by Spanner. */
	public static final Collection<Converter> DEFAULT_SPANNER_READ_CONVERTERS = ImmutableSet.of(
			SPANNER_TO_JAVA_DATE_CONVERTER,
			TIMESTAMP_INSTANT_CONVERTER,
			SPANNER_TO_JAVA_BYTE_ARRAY_CONVERTER,
			SPANNER_TO_JAVA_TIMESTAMP_CONVERTER,
			SPANNER_TO_JAVA_SQL_DATE_CONVERTER);

	public static final Map<Class, Code> JAVA_TYPE_TO_SPANNER_SIMPLE_COLUMN_TYPE_MAPPING;

	public static final Map<Class, Code> JAVA_TYPE_TO_SPANNER_ARRAY_COLUMN_TYPE_MAPPING;

	// @formatter:off
	private static final Map<Code, Class> SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING =
					new ImmutableMap.Builder<Type.Code, Class>()
					// @formatter:on
					.put(Code.BOOL, Boolean.class)
					.put(Code.BYTES, ByteArray.class)
					.put(Code.DATE, com.google.cloud.Date.class)
					.put(Code.FLOAT64, Double.class)
					.put(Code.INT64, Long.class)
					.put(Code.STRING, String.class)
					.put(Code.STRUCT, Struct.class)
					.put(Code.TIMESTAMP, Timestamp.class)
					.build();

	// @formatter:off
	private static final Map<Type.Code, Class> SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING
					= new ImmutableMap.Builder<Type.Code, Class>()
					// @formatter:on
					.put(Code.BOOL, boolean[].class)
					.put(Code.BYTES, ByteArray[].class)
					.put(Code.DATE, com.google.cloud.Date[].class)
					.put(Code.FLOAT64, double[].class)
					.put(Code.INT64, long[].class)
					.put(Code.STRING, String[].class)
					.put(Code.STRUCT, Struct[].class)
					.put(Code.TIMESTAMP, Timestamp[].class)
					.build();

	static {
		ImmutableMap.Builder<Class, Code> builder = new Builder<>();
		SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING
				.keySet()
				.stream()
				.forEach(
						type -> builder.put(SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(type), type));
		JAVA_TYPE_TO_SPANNER_SIMPLE_COLUMN_TYPE_MAPPING = builder.build();
	}

	static {
		ImmutableMap.Builder<Class, Code> builder = new Builder<>();
		SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING
				.keySet()
				.stream()
				.forEach(
						type -> builder.put(SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(type), type));
		JAVA_TYPE_TO_SPANNER_ARRAY_COLUMN_TYPE_MAPPING = builder.build();
	}

	public static <T> Class<T> boxIfNeeded(Class<T> propertyType) {
		if (propertyType == null) {
			return null;
		}
		return propertyType.isPrimitive()
				? (Class<T>) Array.get(Array.newInstance(propertyType, 1), 0).getClass()
				: propertyType;
	}

	public static boolean isIterableNonByteArrayType(Class propType) {
		return Iterable.class.isAssignableFrom(propType) && !ByteArray.class.isAssignableFrom(propType);
	}

	static Iterable convertIterable(
			Iterable source, Class targetType, AbstractSpannerCustomConverter converter) {
		return (Iterable) StreamSupport.stream(source.spliterator(), false)
				.map(item -> converter.convert(item, targetType))
				.collect(Collectors.toList());
	}

	public static Class getSimpleJavaClassFor(Code code) {
		return SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(code);
	}

	public static Class getArrayJavaClassFor(Code code) {
		return SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(code);
	}
}
