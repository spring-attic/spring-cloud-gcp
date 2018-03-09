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
import org.springframework.data.convert.EntityReader;
import org.springframework.data.mapping.PersistentPropertyAccessor;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
class MappingSpannerReadConverter implements EntityReader<Object, Struct> {

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

	private final SpannerMappingContext spannerMappingContext;

	MappingSpannerReadConverter(
			SpannerMappingContext spannerMappingContext) {
		this.spannerMappingContext = spannerMappingContext;
	}

	@Override
	public <R> R read(Class<R> type, Struct source) {
		R object = instantiate(type);
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(type);

		PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(object);

		for (Type.StructField structField : source.getType().getStructFields()) {
			String columnName = structField.getName();
			SpannerPersistentProperty property = persistentEntity
					.getPersistentPropertyByColumnName(columnName);
			if (property == null) {
				throw new SpannerDataException(
						String.format("Error trying to read from Spanner struct column named %s"
								+ " that does not correspond to any property in the entity type ",
								columnName,
								type));
			}
			Class propType = property.getType();
			if (source.isNull(columnName)) {
				continue;
			}

			boolean valueSet;

			/*
			 * Due to type erasure, binder methods for Iterable properties must be manually specified.
			 * ByteArray must be excluded since it implements Iterable, but is also explicitly
			 * supported by spanner.
			 */
			if (ConversionUtils.isIterableNonByteArrayType(propType)) {
				valueSet = attemptReadIterableValue(property, source, columnName, accessor);
			}
			else {
				valueSet = attemptReadSingleItemValue(property, source, columnName,
						accessor);
			}

			if (!valueSet) {
				throw new SpannerDataException(
						String.format("The value in column with name %s"
								+ " could not be converted to the corresponding property in the entity."
								+ " The property's type is %s.", columnName, propType));
			}
		}
		return object;
	}

	private boolean attemptReadSingleItemValue(
			SpannerPersistentProperty spannerPersistentProperty, Struct struct,
			String colName, PersistentPropertyAccessor accessor) {
		BiFunction readFunction = singleItemReadMethodMapping
				.get(ConversionUtils.boxIfNeeded(spannerPersistentProperty.getType()));
		if (readFunction == null) {
			return false;
		}
		accessor.setProperty(spannerPersistentProperty,
				readFunction.apply(struct, colName));
		return true;
	}

	private boolean attemptReadIterableValue(
			SpannerPersistentProperty spannerPersistentProperty, Struct struct,
			String colName, PersistentPropertyAccessor accessor) {

		Class innerType = ConversionUtils.boxIfNeeded(spannerPersistentProperty.getColumnInnerType());
		if (innerType == null || !readIterableMapping.containsKey(innerType)) {
			return false;
		}

		List iterableValue = readIterableMapping.get(innerType).apply(struct, colName);
		accessor.setProperty(spannerPersistentProperty, iterableValue);
		return true;

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
