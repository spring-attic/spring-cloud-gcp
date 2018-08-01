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

import java.util.ArrayList;
import java.util.List;

import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.mapping.model.PropertyValueProvider;

/**
 * A {@link PropertyValueProvider} based on a Struct that uses the
 * {@link ConverterAwareMappingSpannerEntityReader} to convert resulting values from the
 * {@link StructAccessor}
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
class StructPropertyValueProvider implements PropertyValueProvider<SpannerPersistentProperty> {

	private final SpannerCustomConverter readConverter;

	private SpannerEntityReader entityReader;

	private StructAccessor structAccessor;

	private boolean allowMissingColumns;

	/**
	 * Constructor. Missing columns in nested struct column values for corresponding nested Java
	 * object properties is not allowed.
	 * @param structAccessor an accessor used to obtain column values from the struct.
	 * @param readConverter a converter used to convert between struct column types and the required
	 * java types.
	 * @param entityReader a reader used to access the data from each column of the struct.
	 */
	StructPropertyValueProvider(StructAccessor structAccessor, SpannerCustomConverter readConverter,
			SpannerEntityReader entityReader) {
		this(structAccessor, readConverter, entityReader, false);
	}

	/**
	 * Constructor
	 * @param structAccessor an accessor used to obtain column values from the struct.
	 * @param readConverter a converter used to convert between struct column types and the required
	 * java types.
	 * @param entityReader a reader used to access the data from each column of the struct.
	 * @param allowMissingColumns if a nested struct is within this struct's column, then if true
	 * missing columns in the nested struct are also allowed for the corresponding nested Java object.
	 */
	StructPropertyValueProvider(StructAccessor structAccessor, SpannerCustomConverter readConverter,
			SpannerEntityReader entityReader, boolean allowMissingColumns) {
		this.structAccessor = structAccessor;
		this.readConverter = readConverter;
		this.entityReader = entityReader;
		this.allowMissingColumns = allowMissingColumns;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getPropertyValue(SpannerPersistentProperty spannerPersistentProperty) {
		String colName = spannerPersistentProperty.getColumnName();
		if (!this.structAccessor.hasColumn(colName)) {
			throw new SpannerDataException("Column not found: " + colName);
		}
		Class propType = spannerPersistentProperty.getType();
		Object value = ConversionUtils.isIterableNonByteArrayType(propType)
				? readIterableWithConversion(spannerPersistentProperty)
				: readSingleWithConversion(spannerPersistentProperty);

		if (value == null) {
			throw new SpannerDataException(String.format(
					"The value in column with name %s"
							+ " could not be converted to the corresponding property in the entity."
							+ " The property's type is %s.",
					colName, propType));
		}
		return (T) value;
	}

	@SuppressWarnings("unchecked")
	private <T> T readSingleWithConversion(
			SpannerPersistentProperty spannerPersistentProperty) {
		String colName = spannerPersistentProperty.getColumnName();
		Object value = this.structAccessor.getSingleValue(colName);
		return value == null ? null : convertOrRead((Class<T>) spannerPersistentProperty.getType(), value);
	}

	private <T> T convertOrRead(Class<T> targetType, Object sourceValue) {
		Class<?> sourceClass = sourceValue.getClass();
		return Struct.class.isAssignableFrom(sourceClass)
				&& !this.readConverter.canConvert(sourceClass, targetType)
						? this.entityReader.read(targetType, (Struct) sourceValue, null,
								this.allowMissingColumns)
				: this.readConverter.convert(sourceValue, targetType);
	}

	@SuppressWarnings("unchecked")
	private <T> Iterable<T> readIterableWithConversion(
			SpannerPersistentProperty spannerPersistentProperty) {
		String colName = spannerPersistentProperty.getColumnName();
		List<?> listValue = this.structAccessor.getListValue(colName);
		return convertOrReadIterable(listValue,
				(Class<T>) spannerPersistentProperty.getColumnInnerType());
	}

	private <T> Iterable<T> convertOrReadIterable(Iterable<?> source,
			Class<T> targetType) {
		List<T> result = new ArrayList<>();
		source.forEach(item -> result.add(convertOrRead(targetType, item)));
		return result;
	}
}
