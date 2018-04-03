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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import org.springframework.data.convert.EntityReader;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
class MappingSpannerReadConverter extends AbstractSpannerCustomConverter
		implements EntityReader<Object, Struct> {

	private static final Map<Class, BiFunction<Struct, String, List>> readIterableMapping =
			new ImmutableMap.Builder<Class, BiFunction<Struct, String, List>>()
			.put(Boolean.class, AbstractStructReader::getBooleanList)
			.put(Long.class, AbstractStructReader::getLongList)
			.put(String.class, AbstractStructReader::getStringList)
			.put(Double.class, AbstractStructReader::getDoubleList)
			.put(Timestamp.class, AbstractStructReader::getTimestampList)
			.put(Date.class, AbstractStructReader::getDateList)
			.put(ByteArray.class, AbstractStructReader::getBytesList)
			.build();

	private static final Map<Class, BiFunction<Struct, String, ?>> singleItemReadMethodMapping =
			new ImmutableMap.Builder<Class, BiFunction<Struct, String, ?>>()
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

	private static final Map<Type, Class> spannerColumnTypeToJavaTypeMapping = new ImmutableMap.Builder<Type, Class>()
			.put(Type.bool(), Boolean.class).put(Type.bytes(), ByteArray.class)
			.put(Type.date(), Date.class).put(Type.float64(), Double.class)
			.put(Type.int64(), Long.class).put(Type.string(), String.class)
			.put(Type.array(Type.float64()), double[].class)
			.put(Type.array(Type.int64()), long[].class)
			.put(Type.array(Type.bool()), boolean[].class)
			.put(Type.timestamp(), Timestamp.class).build();

	private final SpannerMappingContext spannerMappingContext;

	MappingSpannerReadConverter(
			SpannerMappingContext spannerMappingContext,
			CustomConversions customConversions) {
		super(customConversions, null);
		this.spannerMappingContext = spannerMappingContext;
	}

	/**
	 * Reads a single POJO from a Spanner row.
	 * @param type the type of POJO
	 * @param source the Spanner row
	 * @param includeColumns the columns to read. If null then all columns will be read.
	 * @param <R> the type of the POJO.
	 * @return the POJO
	 */
	public <R> R read(Class<R> type, Struct source, Set<String> includeColumns) {
		boolean readAllColumns = includeColumns == null;
		R object = instantiate(type);
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(type);

		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(object);

		persistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					String columnName = spannerPersistentProperty.getColumnName();
					try {
						if ((!readAllColumns && !includeColumns.contains(columnName))
								|| source.isNull(columnName)) {
							return;
						}
					}
					catch (IllegalArgumentException e) {
						throw new SpannerDataException(
								"Unable to read column from Spanner results: "
										+ columnName,
								e);
					}
					Class propType = spannerPersistentProperty.getType();

					boolean valueSet;

					/*
					 * Due to type erasure, binder methods for Iterable properties must be
					 * manually specified. ByteArray must be excluded since it implements
					 * Iterable, but is also explicitly supported by spanner.
					 */
					if (ConversionUtils.isIterableNonByteArrayType(propType)) {
						valueSet = attemptReadIterableValue(spannerPersistentProperty,
								source, columnName, accessor);
					}
					else {
						Class sourceType = this.spannerColumnTypeToJavaTypeMapping
								.get(source.getColumnType(columnName));
							valueSet = attemptReadSingleItemValue(
									spannerPersistentProperty, source, sourceType,
									columnName, accessor);
					}

					if (!valueSet) {
						throw new SpannerDataException(String.format(
								"The value in column with name %s"
										+ " could not be converted to the corresponding property in the entity."
										+ " The property's type is %s.",
								columnName, propType));
					}

				});
		return object;
	}

	@Override
	public <R> R read(Class<R> type, Struct source) {
		return read(type, source, null);
	}

	private boolean attemptReadSingleItemValue(
			SpannerPersistentProperty spannerPersistentProperty, Struct struct,
			Class sourceType,
			String colName, PersistentPropertyAccessor accessor) {
		Class targetType = spannerPersistentProperty.getType();
		if (sourceType == null || !canConvert(sourceType, targetType)) {
			return false;
		}
		BiFunction readFunction = singleItemReadMethodMapping
				.get(ConversionUtils.boxIfNeeded(sourceType));
		if (readFunction == null) {
			return false;
		}
		accessor.setProperty(spannerPersistentProperty,
				convert(readFunction.apply(struct, colName), targetType));
		return true;
	}

	private boolean attemptReadIterableValue(
			SpannerPersistentProperty spannerPersistentProperty, Struct struct,
			String colName, PersistentPropertyAccessor accessor) {

		Class innerType = ConversionUtils.boxIfNeeded(spannerPersistentProperty.getColumnInnerType());
		if (innerType == null) {
			return false;
		}

		// due to checkstyle limit of 3 return statments per function, this variable is
		// used.
		boolean valueSet = false;

		if (this.readIterableMapping.containsKey(innerType)) {
			accessor.setProperty(spannerPersistentProperty,
					this.readIterableMapping.get(innerType).apply(struct, colName));
			valueSet = true;
		}

		if (!valueSet) {
			for (Class sourceType : this.readIterableMapping.keySet()) {
				if (canConvert(sourceType, innerType)) {
					List iterableValue = readIterableMapping.get(sourceType).apply(struct,
							colName);
					accessor.setProperty(spannerPersistentProperty, ConversionUtils
							.convertIterable(iterableValue, innerType, this));
					valueSet = true;
					break;
				}
			}
		}

		if (!valueSet && struct.getColumnType(colName).getArrayElementType().getCode() == Type.Code.STRUCT) {
			List<Struct> iterableValue = struct.getStructList(colName);
			Iterable convertedIterableValue = (Iterable) StreamSupport.stream(iterableValue.spliterator(), false)
					.map(item -> read(innerType, item))
					.collect(Collectors.toList());
			accessor.setProperty(spannerPersistentProperty, convertedIterableValue);
			valueSet = true;
		}

		return valueSet;
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

}
