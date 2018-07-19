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

import com.google.cloud.datastore.Entity;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.util.ClassUtils;

/**
 * A {@link PropertyValueProvider} for Datastore entities
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class EntityPropertyValueProvider implements PropertyValueProvider<DatastorePersistentProperty> {
	private Entity entity;

	EntityPropertyValueProvider(Entity entity) {
		this.entity = entity;
	}

	@Override
	public <T> T getPropertyValue(DatastorePersistentProperty datastorePersistentProperty) {
		String fieldName = datastorePersistentProperty.getFieldName();

		if (!this.entity.contains(fieldName)) {
			throw new DatastoreDataException("Field not found: " + fieldName);
		}

		Class propType = datastorePersistentProperty.getType();
		Object value = readSingleWithConversion(datastorePersistentProperty);

		if (value == null) {
			throw new DatastoreDataException(String.format(
					"The value in column with name %s"
							+ " could not be converted to the corresponding property in the entity."
							+ " The property's type is %s.",
					fieldName, propType));
		}
		return (T) value;
	}

	private Object readSingleWithConversion(DatastorePersistentProperty persistentProperty) {
		Object val = entity.getValue(persistentProperty.getFieldName()).get();
		Class<?> targetType = persistentProperty.getType();
		if (ClassUtils.isAssignable(targetType, val.getClass())) {
			return val;
		}
		return null;
	}

}
