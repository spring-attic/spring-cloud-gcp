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

		return readSingleWithConversion(datastorePersistentProperty);
	}

	@SuppressWarnings("unchecked")
	private <T> T readSingleWithConversion(DatastorePersistentProperty persistentProperty) {
		Object val = this.entity.getValue(persistentProperty.getFieldName()).get();
		Class<?> targetType = persistentProperty.getType();
		if (val == null) {
			return null;
		}
		if (ClassUtils.isAssignable(targetType, val.getClass())) {
			return (T) val;
		}

		throw new DatastoreDataException("The value in entity's property with name " + persistentProperty.getFieldName()
				+ " could not be converted to the corresponding property in the class. " +
				"The property's type is " + targetType + " but the value's type is " + val.getClass());
	}

}
