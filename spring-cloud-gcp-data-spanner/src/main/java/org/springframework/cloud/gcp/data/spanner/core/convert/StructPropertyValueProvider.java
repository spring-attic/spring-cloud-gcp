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
 */
class StructPropertyValueProvider implements PropertyValueProvider<SpannerPersistentProperty> {

	private final SpannerCustomConverter readConverter;

	private SpannerEntityReader entityReader;

	private StructAccessor structAccessor;

	StructPropertyValueProvider(StructAccessor structAccessor, SpannerCustomConverter readConverter,
			SpannerEntityReader entityReader) {
		this.structAccessor = structAccessor;
		this.readConverter = readConverter;
		this.entityReader = entityReader;
	}

	@Override
	public Object getPropertyValue(SpannerPersistentProperty spannerPersistentProperty) {
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
		return value;

	}

	private Object readSingleWithConversion(SpannerPersistentProperty spannerPersistentProperty) {
		String colName = spannerPersistentProperty.getColumnName();
		Class targetType = spannerPersistentProperty.getType();
		Object value = this.structAccessor.getSingleValue(colName);
		return convertOrRead(targetType, value);
	}

	private <T> T convertOrRead(Class<T> targetType, Object sourceValue) {
		Class<?> sourceClass = sourceValue.getClass();
		return Struct.class.isAssignableFrom(sourceClass)
				? this.entityReader.read(targetType, (Struct) sourceValue)
				: this.readConverter.convert(sourceValue, targetType);
	}

	private Iterable readIterableWithConversion(SpannerPersistentProperty spannerPersistentProperty) {
		String colName = spannerPersistentProperty.getColumnName();
		List listValue = this.structAccessor.getListValue(colName);
		return convertOrReadIterable(listValue, spannerPersistentProperty.getColumnInnerType());
	}

	private <T> Iterable<T> convertOrReadIterable(Iterable source, Class<T> targetType) {
		List<T> result = new ArrayList<>();
		source.forEach(item -> result.add(convertOrRead(targetType, item)));
		return result;
	}
}
