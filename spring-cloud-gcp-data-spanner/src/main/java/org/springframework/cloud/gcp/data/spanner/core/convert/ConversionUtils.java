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
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.ByteArray;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class ConversionUtils {

	public static final Map<Type, Class> SPANNER_COLUMN_TYPE_TO_JAVA_TYPE_MAPPING =
			new ImmutableMap.Builder<Type, Class>()
					.put(Type.bool(), Boolean.class).put(Type.bytes(), ByteArray.class)
					.put(Type.date(), com.google.cloud.Date.class)
					.put(Type.float64(), Double.class).put(Type.int64(), Long.class)
					.put(Type.string(), String.class)
					.put(Type.array(Type.float64()), double[].class)
					.put(Type.array(Type.int64()), long[].class)
					.put(Type.array(Type.bool()), boolean[].class)
					.put(Type.timestamp(), Timestamp.class).build();

	public static final Converter<Date, com.google.cloud.Date> JAVA_TO_SPANNER_DATE_CONVERTER =
			new Converter<Date, com.google.cloud.Date>() {
				@Nullable
				@Override
				public com.google.cloud.Date convert(Date date) {
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);
					return com.google.cloud.Date.fromYearMonthDay(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
							cal.get(Calendar.DAY_OF_MONTH));
				}
			};

	public static final Converter<com.google.cloud.Date, Date> SPANNER_TO_JAVA_DATE_CONVERTER =
			new Converter<com.google.cloud.Date, Date>() {
				@Nullable
				@Override
				public Date convert(com.google.cloud.Date date) {
					Calendar cal = Calendar.getInstance();
					cal.set(date.getYear(), date.getMonth() - 1, date.getDayOfMonth());
					return cal.getTime();
				}
			};

	public static final Converter<Instant, Timestamp> INSTANT_TIMESTAMP_CONVERTER =
			new Converter<Instant, Timestamp>() {
				@Nullable
				@Override
				public Timestamp convert(Instant instant) {
					return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
				}
			};

	public static final Converter<Timestamp, Instant> TIMESTAMP_INSTANT_CONVERTER =
			new Converter<Timestamp, Instant>() {
				@Nullable
				@Override
				public Instant convert(Timestamp timestamp) {
					return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
				}
			};

	public static final Converter<java.sql.Timestamp, Timestamp> JAVA_TO_SPANNER_TIMESTAMP_CONVERTER =
			new Converter<java.sql.Timestamp, Timestamp>() {
				@Nullable
				@Override
				public Timestamp convert(java.sql.Timestamp timestamp) {
					return Timestamp.of(timestamp);
				}
			};

	public static final Converter<Timestamp, java.sql.Timestamp> SPANNER_TO_JAVA_TIMESTAMP_CONVERTER =
			new Converter<Timestamp, java.sql.Timestamp>() {
				@Nullable
				@Override
				public java.sql.Timestamp convert(Timestamp timestamp) {
					return java.sql.Timestamp.from(TIMESTAMP_INSTANT_CONVERTER.convert(timestamp));
				}
			};

	public static final Converter<byte[], ByteArray> JAVA_TO_SPANNER_BYTE_ARRAY_CONVERTER =
			new Converter<byte[], ByteArray>() {
				@Nullable
				@Override
				public ByteArray convert(byte[] bytes) {
					return ByteArray.copyFrom(bytes);
				}
			};

	public static final Converter<ByteArray, byte[]> SPANNER_TO_JAVA_BYTE_ARRAY_CONVERTER =
			new Converter<ByteArray, byte[]>() {
				@Nullable
				@Override
				public byte[] convert(ByteArray bytes) {
					return bytes.toByteArray();
				}
			};

	/**
	 * Converters from common types to those used by Spanner.
	 */
	public static final Collection<Converter> DEFAULT_SPANNER_WRITE_CONVERTERS = ImmutableSet
			.of(JAVA_TO_SPANNER_DATE_CONVERTER, INSTANT_TIMESTAMP_CONVERTER, JAVA_TO_SPANNER_BYTE_ARRAY_CONVERTER,
					JAVA_TO_SPANNER_TIMESTAMP_CONVERTER);

	/**
	 * Converters from common types to those used by Spanner.
	 */
	public static final Collection<Converter> DEFAULT_SPANNER_READ_CONVERTERS = ImmutableSet
			.of(SPANNER_TO_JAVA_DATE_CONVERTER, TIMESTAMP_INSTANT_CONVERTER, SPANNER_TO_JAVA_BYTE_ARRAY_CONVERTER,
					SPANNER_TO_JAVA_TIMESTAMP_CONVERTER);

	private static final Map<Class, Type> JAVA_TYPE_TO_SPANNER_COLUMN_TYPE_MAPPING;

	static {
		ImmutableMap.Builder<Class, Type> builder = new Builder<>();
		SPANNER_COLUMN_TYPE_TO_JAVA_TYPE_MAPPING.keySet().stream().forEach(type -> builder
				.put(SPANNER_COLUMN_TYPE_TO_JAVA_TYPE_MAPPING.get(type), type));
		JAVA_TYPE_TO_SPANNER_COLUMN_TYPE_MAPPING = builder.build();
	}

	private static String getTypeDDLString(Type type) {
		Assert.notNull(type, "A valid Spanner column type is required.");
		if (type.getCode() == Code.ARRAY) {
			return "ARRAY<" + getTypeDDLString(type.getArrayElementType()) + ">";
		}
		return type.toString()
				+ (type.getCode() == Code.STRING || type.getCode() == Code.BYTES ? "(MAX)"
				: "");
	}

	private static Class getFullyConvertableType(SpannerConverter spannerConverter,
			Class originalType, boolean isIterableInnerType) {
		Set<Class> spannerTypes = (isIterableInnerType
				? MappingSpannerWriteConverter.iterablePropertyType2ToMethodMap
				: MappingSpannerWriteConverter.singleItemType2ToMethodMap).keySet();
		if (spannerTypes.contains(originalType)) {
			return originalType;
		}
		Class ret = null;
		for (Class spannerType : spannerTypes) {
			if (isIterableInnerType
					&& spannerConverter.canHandlePropertyTypeForArrayRead(originalType,
					spannerType)
					&& spannerConverter.canHandlePropertyTypeForArrayWrite(originalType,
					spannerType)) {
				ret = spannerType;
				break;
			}
			else if (!isIterableInnerType
					&& spannerConverter.canHandlePropertyTypeForSingularRead(originalType,
					spannerType)
					&& spannerConverter.canHandlePropertyTypeForSingularWrite(
					originalType, spannerType)) {
				ret = spannerType;
				break;
			}
		}
		return ret;
	}

	public static String getColumnDDLString(
			SpannerPersistentProperty spannerPersistentProperty,
			SpannerConverter spannerConverter) {
		Class columnType = spannerPersistentProperty.getType();

		if (isIterableNonByteArrayType(columnType)) {
			Class innerType = spannerPersistentProperty.getColumnInnerType();
			if (innerType == null) {
				throw new SpannerDataException(
						"Cannot get column DDL for iterable type without annotated inner type.");
			}
			Type spannerSupportedInnerType = JAVA_TYPE_TO_SPANNER_COLUMN_TYPE_MAPPING
					.get(getFullyConvertableType(spannerConverter, innerType, true));
			if (spannerSupportedInnerType == null) {
				throw new SpannerDataException(
						"Could not find suitable Spanner column inner type for property type:"
								+ innerType);
			}
			return getTypeDDLString(Type.array(spannerSupportedInnerType));
		}
		Type spannerColumnType = JAVA_TYPE_TO_SPANNER_COLUMN_TYPE_MAPPING
				.get(getFullyConvertableType(spannerConverter, columnType, false));
		if (spannerColumnType == null) {
			throw new SpannerDataException(
					"Could not find suitable Spanner column type for property type:"
							+ columnType);
		}
		return getTypeDDLString(spannerColumnType);
	}

	/**
	 * returns true if the given SpannerPersistentProperty refers to entities stored in a
	 * Spanner table. returns false otherwise.
	 *
	 * @param spannerPersistentProperty
	 * @return
	 */
	public static boolean isSpannerTableProperty(
			SpannerPersistentProperty spannerPersistentProperty) {
		Class innerType = spannerPersistentProperty.getColumnInnerType();
		if (spannerPersistentProperty.getType().isAnnotationPresent(Table.class)) {
			return true;
		}
		else if (innerType != null && innerType.isAnnotationPresent(Table.class)) {
			return true;
		}
		return false;
	}

	static Class boxIfNeeded(Class propertyType) {
		if (propertyType == null) {
			return null;
		}
		return propertyType.isPrimitive()
				? Array.get(Array.newInstance(propertyType, 1), 0).getClass()
				: propertyType;
	}

	public static boolean isIterableNonByteArrayType(Class propType) {
		return Iterable.class.isAssignableFrom(propType)
				&& !ByteArray.class.isAssignableFrom(propType);
	}

	/**
	 * Applies the provided function on the underlying child-entity class if the property
	 * is indeed a child entity and returns the result. Returns null if the property is
	 * not a child entity.
	 * @param spannerPersistentProperty the property to check if it is a child entity.
	 * @param function the function to apply to the child entity's class.
	 * @param <T> the result type.
	 * @return returns the result of the given function if the property is a child entity.
	 * returns null otherwise.
	 */
	public static <T> T applyIfChildEntityType(
			SpannerPersistentProperty spannerPersistentProperty,
			Function<Class, T> function) {
		if (!isSpannerTableProperty(spannerPersistentProperty)) {
			return null;
		}
		Class propType = spannerPersistentProperty.getType();
		return function.apply(isIterableNonByteArrayType(propType)
				? spannerPersistentProperty.getColumnInnerType()
				: propType);
	}

	static Iterable convertIterable(Iterable source, Class targetType,
			AbstractSpannerCustomConverter converter) {
		return (Iterable) StreamSupport.stream(source.spliterator(), false)
				.map(item -> converter.convert(item, targetType))
				.collect(Collectors.toList());
	}
}
