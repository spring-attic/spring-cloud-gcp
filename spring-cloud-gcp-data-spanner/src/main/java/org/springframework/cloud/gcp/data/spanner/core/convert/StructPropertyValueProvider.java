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

import java.util.List;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.mapping.model.PropertyValueProvider;

/**
 * A {@link PropertyValueProvider} based on a Struct that uses the
 * {@link MappingSpannerReadConverter} to convert resulting values from the
 * {@link StructAccessor}
 *
 * @author Balint Pato
 */
class StructPropertyValueProvider implements PropertyValueProvider<SpannerPersistentProperty> {

	private final MappingSpannerReadConverter readConverter;

	private StructAccessor structAccessor;

	StructPropertyValueProvider(StructAccessor structAccessor, MappingSpannerReadConverter readConverter) {
		this.structAccessor = structAccessor;
		this.readConverter = readConverter;
	}

	@Override
	public Object getPropertyValue(SpannerPersistentProperty spannerPersistentProperty) {
		String colName = spannerPersistentProperty.getColumnName();
		if (!this.structAccessor.hasColumn(colName)) {
			throw new SpannerDataException("Column not found: " + colName);
		}
		Class propType = spannerPersistentProperty.getType();
		Object value = ConversionUtils.isIterableNonByteArrayType(propType)
				? readIterableWithConversion(spannerPersistentProperty, this.structAccessor)
				: readSingleWithConversion(spannerPersistentProperty, this.structAccessor);

		if (value == null) {
			throw new SpannerDataException(String.format(
					"The value in column with name %s"
							+ " could not be converted to the corresponding property in the entity."
							+ " The property's type is %s.",
					colName, propType));
		}
		return value;

	}

	private Object readSingleWithConversion(SpannerPersistentProperty spannerPersistentProperty,
			StructAccessor struct) {
		String colName = spannerPersistentProperty.getColumnName();
		Class targetType = spannerPersistentProperty.getType();
		Object value = struct.getSingleValue(colName);
		return this.readConverter.convert(value, targetType);
	}

	private Object readIterableWithConversion(SpannerPersistentProperty spannerPersistentProperty,
			StructAccessor struct) {
		String colName = spannerPersistentProperty.getColumnName();
		List listValue = struct.getListValue(colName);
		return ConversionUtils.convertIterable(
						listValue,
						spannerPersistentProperty.getColumnInnerType(),
						this.readConverter);
	}
}
