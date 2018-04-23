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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbstractStructReader;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.common.collect.ImmutableMap;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
class MappingSpannerReadConverter extends AbstractSpannerCustomConverter
		implements SpannerEntityReader {

	// @formatter:off
	static final Map<Class, BiFunction<Struct, String, List>> readIterableMapping =
			new ImmutableMap.Builder<Class, BiFunction<Struct, String, List>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBooleanList)
					.put(Long.class, AbstractStructReader::getLongList)
					.put(String.class, AbstractStructReader::getStringList)
					.put(Double.class, AbstractStructReader::getDoubleList)
					.put(Timestamp.class, AbstractStructReader::getTimestampList)
					.put(Date.class, AbstractStructReader::getDateList)
					.put(ByteArray.class, AbstractStructReader::getBytesList)
					.put(Struct.class, AbstractStructReader::getStructList)
					.build();

	// @formatter:off
	static final Map<Class, BiFunction<Struct, String, ?>> singleItemReadMethodMapping =
			new ImmutableMap.Builder<Class, BiFunction<Struct, String, ?>>()
					// @formatter:on
					.put(Boolean.class, AbstractStructReader::getBoolean)
					.put(Long.class, AbstractStructReader::getLong)
					.put(String.class, AbstractStructReader::getString)
					.put(Double.class, AbstractStructReader::getDouble)
					.put(Timestamp.class, AbstractStructReader::getTimestamp)
					.put(Date.class, AbstractStructReader::getDate)
					.put(ByteArray.class, AbstractStructReader::getBytes)
					.put(double[].class, AbstractStructReader::getDoubleArray)
					.put(long[].class, AbstractStructReader::getLongArray)
					.put(boolean[].class, AbstractStructReader::getBooleanArray).build();

	private final SpannerMappingContext spannerMappingContext;

	private final SpannerTypeMapper spannerTypeMapper;

	MappingSpannerReadConverter(
			SpannerMappingContext spannerMappingContext,
			CustomConversions customConversions) {
		super(customConversions, null);
		this.spannerMappingContext = spannerMappingContext;
		this.spannerTypeMapper = new SpannerTypeMapper();
	}

	private static <R> R instantiate(Class<R> type) {
		R object;
		try {
			Constructor<R> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			object = constructor.newInstance();
		}
		catch (ReflectiveOperationException e) {
			throw new SpannerDataException(
					"Unable to create a new instance of entity using default constructor.",
					e);
		}
		return object;
	}

	/**
	 * Reads a single POJO from a Spanner row.
	 * @param type the type of POJO
	 * @param source the Spanner row
	 * @param includeColumns the columns to read. If null then all columns will be read.
	 * @param allowMissingColumns if true, then properties with no corresponding column are
	 * not mapped. If false, then an exception is thrown.
	 * @param <R> the type of the POJO.
	 * @return the POJO
	 */
	public <R> R read(Class<R> type, Struct source, Set<String> includeColumns,
			boolean allowMissingColumns) {
		boolean readAllColumns = includeColumns == null;
		R object = instantiate(type);
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(type);

		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(object);

		persistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					if (!shouldSkipProperty(source,
							spannerPersistentProperty,
							includeColumns,
							readAllColumns,
							allowMissingColumns)) {
						Object value = readWithConversion(source, spannerPersistentProperty);
						accessor.setProperty(spannerPersistentProperty, value);
					}

				});
		return object;
	}

	private boolean shouldSkipProperty(Struct struct,
			SpannerPersistentProperty spannerPersistentProperty,
			Set<String> includeColumns,
			boolean readAllColumns,
			boolean allowMissingColumns) {
		String columnName = spannerPersistentProperty.getColumnName();
		boolean notRequiredByPartialRead = !readAllColumns && !includeColumns.contains(columnName);

		return notRequiredByPartialRead
				|| isMissingColumn(struct, allowMissingColumns, columnName)
				|| struct.isNull(columnName);
	}

	private boolean isMissingColumn(Struct struct, boolean allowMissingColumns,
			String columnName) {
		boolean missingColumn = !hasColumn(struct, columnName);
		if (missingColumn && !allowMissingColumns) {
			throw new SpannerDataException(
					"Unable to read column from Spanner results: "
							+ columnName);
		}
		return missingColumn;
	}

	private boolean hasColumn(Struct struct, String columnName) {
		boolean hasColumn = false;
		for (Type.StructField f : struct.getType().getStructFields()) {
			if (f.getName().equals(columnName)) {
				hasColumn = true;
				break;
			}
		}
		return hasColumn;
	}

	protected Object readWithConversion(Struct source, SpannerPersistentProperty spannerPersistentProperty) {
		Class propType = spannerPersistentProperty.getType();
		Object value;
		/*
		 * Due to type erasure, binder methods for Iterable properties must be manually specified.
		 * ByteArray must be excluded since it implements Iterable, but is also explicitly
		 * supported by spanner.
		 */
		if (ConversionUtils.isIterableNonByteArrayType(propType)) {
			value = readIterableWithConversion(spannerPersistentProperty, source);
		}
		else {
			value = readSingleWithConversion(spannerPersistentProperty, source);
		}

		if (value == null) {
			throw new SpannerDataException(String.format(
					"The value in column with name %s"
							+ " could not be converted to the corresponding property in the entity."
							+ " The property's type is %s.",
					spannerPersistentProperty.getColumnName(), propType));
		}
		return value;
	}

	@Override
	public <R> R read(Class<R> type, Struct source) {
		return read(type, source, null, false);
	}

	@Override
	public <T> T convert(Object sourceValue, Class<T> targetType) {
		Class<?> sourceClass = sourceValue.getClass();
		T result = null;
		if (targetType.isAssignableFrom(sourceClass)) {
			return (T) sourceValue;
		}
		else if (Struct.class.isAssignableFrom(sourceClass)) {
			result = read(targetType, (Struct) sourceValue);
		}
		else if (canConvert(sourceClass, targetType)) {
			result = super.convert(sourceValue, targetType);
		}
		return result;
	}

	private Object readSingleWithConversion(SpannerPersistentProperty spannerPersistentProperty, Struct struct) {
		String colName = spannerPersistentProperty.getColumnName();
		Type colType = struct.getColumnType(colName);
		Type.Code code = colType.getCode();
		Class sourceType = code.equals(Type.Code.ARRAY)
				? this.spannerTypeMapper.getArrayJavaClassFor(colType.getArrayElementType().getCode())
				: this.spannerTypeMapper.getSimpleJavaClassFor(code);
		Class targetType = spannerPersistentProperty.getType();
		BiFunction readFunction = singleItemReadMethodMapping.get(sourceType);
		Object value = readFunction.apply(struct, colName);
		return convert(value, targetType);
	}

	private Object readIterableWithConversion(SpannerPersistentProperty spannerPersistentProperty, Struct struct) {
		String colName = spannerPersistentProperty.getColumnName();
		Type.Code innerTypeCode = struct.getColumnType(colName).getArrayElementType().getCode();
		Class clazz = this.spannerTypeMapper.getSimpleJavaClassFor(innerTypeCode);
		BiFunction<Struct, String, List> readMethod = readIterableMapping.get(clazz);
		List listValue = readMethod.apply(struct, colName);

		return convertList(listValue, spannerPersistentProperty.getColumnInnerType());
	}

	private List<Object> convertList(List listValue, Class targetInnerType) {
		List<Object> convList = new ArrayList<>();
		for (Object item : listValue) {
			convList.add(convert(item, targetInnerType));
		}
		return convList;
	}

}
