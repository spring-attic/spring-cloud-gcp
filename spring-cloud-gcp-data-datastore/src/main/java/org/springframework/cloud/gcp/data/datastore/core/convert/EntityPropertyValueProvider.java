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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.datastore.Entity;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * A {@link PropertyValueProvider} for Datastore entities
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class EntityPropertyValueProvider implements PropertyValueProvider<DatastorePersistentProperty> {
	private final Entity entity;

	private final ReadWriteConversions conversion;

	EntityPropertyValueProvider(Entity entity, ReadWriteConversions readWriteConversions) {
		this.entity = entity;

		this.conversion = readWriteConversions;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getPropertyValue(DatastorePersistentProperty datastorePersistentProperty) {
		String fieldName = datastorePersistentProperty.getFieldName();

		if (!this.entity.contains(fieldName)) {
			throw new DatastoreDataException("Field not found: " + fieldName);
		}

		Class<?> targetType = datastorePersistentProperty.getTypeInformation().getType();
		if (isNonNullCollection(datastorePersistentProperty)) {
			Class<?> componentType = datastorePersistentProperty.getTypeInformation().getComponentType().getType();
			List<?> elements = this.entity.getList(datastorePersistentProperty.getFieldName())
					.stream()
					.map(v -> readSingleWithConversion(v.get(), componentType, datastorePersistentProperty))
					.collect(Collectors.toList());
			try {
				return (T) convertToCollection(elements, targetType);
			}
			catch (ConversionFailedException e) {
				throw new DatastoreDataException(
						"Unable to read property " + datastorePersistentProperty.getFieldName(), e);
			}
		}

		Object val = this.entity.getValue(datastorePersistentProperty.getFieldName()).get();
		return readSingleWithConversion(val, targetType, datastorePersistentProperty);
	}

	@SuppressWarnings("unchecked")
	private <T> T readSingleWithConversion(Object val, Class<?> aClass,
			DatastorePersistentProperty persistentProperty) {
		if (val == null) {
			return null;
		}

		T result;
		try {
			result = this.conversion.convertOnRead(val, aClass);
		}
		catch (ConverterNotFoundException e) {
			throw new DatastoreDataException(getErrorMessage(val, aClass, persistentProperty), e);
		}

		if (result != null) {
			return result;
		}
		throw new DatastoreDataException(getErrorMessage(val, aClass, persistentProperty));
	}

	private String getErrorMessage(Object val, Class<?> aClass, DatastorePersistentProperty persistentProperty) {
		return "Unable to read "
				+ (isNonNullCollection(persistentProperty) ? "an element of " : "")
				+ "property " + persistentProperty.getFieldName() + " ("
				+ aClass + " can't be converted to " + val.getClass() + ")";
	}

	private boolean isNonNullCollection(DatastorePersistentProperty persistentProperty) {
		return persistentProperty != null && persistentProperty.isCollectionLike() &&
				this.entity.getList(persistentProperty.getFieldName()) != null;
	}

	private Object convertToCollection(@Nullable Object value, @Nullable Class<?> target) {

		if (value == null || target == null || ClassUtils.isAssignableValue(target, value)) {
			return value;
		}

		return this.conversion.convertCollection(value, target);
	}

}
