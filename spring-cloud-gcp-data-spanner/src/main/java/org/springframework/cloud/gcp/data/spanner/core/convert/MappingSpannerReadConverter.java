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
import java.util.Set;

import com.google.cloud.spanner.Struct;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
class MappingSpannerReadConverter extends AbstractSpannerCustomConverter
		implements SpannerEntityReader {

	private final SpannerMappingContext spannerMappingContext;

	private EntityInstantiators instantiators;

	MappingSpannerReadConverter(
			SpannerMappingContext spannerMappingContext,
			CustomConversions customConversions) {
		super(customConversions, null);
		this.spannerMappingContext = spannerMappingContext;

		this.instantiators = new EntityInstantiators();
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
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(type);

		StructAccessor structAccessor = new StructAccessor(source);

		StructPropertyValueProvider propertyValueProvider = new StructPropertyValueProvider(structAccessor, this);

		PreferredConstructor<?, SpannerPersistentProperty> persistenceConstructor = persistentEntity
				.getPersistenceConstructor();

		// @formatter:off
		ParameterValueProvider<SpannerPersistentProperty> parameterValueProvider =
						new PersistentEntityParameterValueProvider<>(persistentEntity, propertyValueProvider, null);
		// @formatter:on

		EntityInstantiator instantiator = this.instantiators.getInstantiatorFor(persistentEntity);
		Object instance = instantiator.createInstance(persistentEntity, parameterValueProvider);
		PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(instance);

		persistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					if (!shouldSkipProperty(
							structAccessor,
							spannerPersistentProperty,
							includeColumns,
							readAllColumns,
							allowMissingColumns,
							persistenceConstructor)) {

						Object value = propertyValueProvider.getPropertyValue(spannerPersistentProperty);
						accessor.setProperty(spannerPersistentProperty, value);
					}
				});

		return (R) instance;
	}

	private boolean shouldSkipProperty(StructAccessor struct,
			SpannerPersistentProperty spannerPersistentProperty,
			Set<String> includeColumns,
			boolean readAllColumns,
			boolean allowMissingColumns,
			PreferredConstructor<?, SpannerPersistentProperty> persistenceConstructor) {
		String columnName = spannerPersistentProperty.getColumnName();
		boolean notRequiredByPartialRead = !readAllColumns && !includeColumns.contains(columnName);

		return notRequiredByPartialRead
				|| isMissingColumn(struct, allowMissingColumns, columnName)
				|| struct.isNull(columnName)
				|| persistenceConstructor.isConstructorParameter(spannerPersistentProperty);
	}

	private boolean isMissingColumn(StructAccessor struct, boolean allowMissingColumns,
			String columnName) {
		boolean missingColumn = !struct.hasColumn(columnName);
		if (missingColumn && !allowMissingColumns) {
			throw new SpannerDataException(
					"Unable to read column from Spanner results: "
							+ columnName);
		}
		return missingColumn;
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

	List<Object> convertList(List listValue, Class targetInnerType) {
		List<Object> convList = new ArrayList<>();
		for (Object item : listValue) {
			convList.add(convert(item, targetInnerType));
		}
		return convList;
	}

}
