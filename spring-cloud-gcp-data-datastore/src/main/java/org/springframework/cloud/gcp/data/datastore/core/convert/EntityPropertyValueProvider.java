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

import java.util.Map;

import com.google.cloud.datastore.BaseEntity;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.ConversionException;
import org.springframework.data.mapping.model.PropertyValueProvider;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

/**
 * A {@link PropertyValueProvider} for Datastore entities
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class EntityPropertyValueProvider implements PropertyValueProvider<DatastorePersistentProperty> {
	private final BaseEntity entity;

	private final ReadWriteConversions conversion;


	public EntityPropertyValueProvider(BaseEntity entity, ReadWriteConversions readWriteConversions) {
		if (entity == null) {
			throw new DatastoreDataException("A non-null entity is required");
		}
		this.entity = entity;
		this.conversion = readWriteConversions;
	}

	@Override
	public <T> T getPropertyValue(DatastorePersistentProperty persistentProperty) {
		boolean isMap = Map.class.isAssignableFrom(persistentProperty.getType());
		TypeInformation singularType;
		if (isMap) {
			singularType = persistentProperty.getEmbeddedMapValueType();
		}
		else {
			singularType = persistentProperty.getComponentType() == null ? null
					: ClassTypeInformation.from(persistentProperty.getComponentType());
		}
		Class collectionType = persistentProperty.getType();
		if (singularType == null) {
			singularType = ClassTypeInformation.from(collectionType);
			collectionType = null;
		}
		return getPropertyValue(persistentProperty.getFieldName(),
				persistentProperty.isEmbedded(), isMap, collectionType,
				singularType);
	}

	/**
	 * Get a property value from the entity.
	 * @param fieldName the name of the field to get.
	 * @param isEmbedded if the property is an embedded entity.
	 * @param collectionType the collection type if the property is not singular. null if
	 * the property is singular.
	 * @param componentType the singular item type.
	 * @return the property converted from the entity.
	 */
	public <T> T getPropertyValue(String fieldName, boolean isEmbedded,
			Class collectionType, Class componentType) {
		return (T) getPropertyValue(fieldName, isEmbedded, false, collectionType,
				ClassTypeInformation.from(componentType));
	}

	@SuppressWarnings("unchecked")
	private <T> T getPropertyValue(String fieldName, boolean isEmbedded, boolean isMap,
			Class collectionType, TypeInformation componentType) {
		if (!this.entity.contains(fieldName)) {
			return null;
		}
		try {
			if (isEmbedded) {
				return isMap
						? this.conversion.convertOnReadEmbeddedMap(
								this.entity.getEntity(fieldName), collectionType,
								componentType)
						: this.conversion.convertOnReadEmbedded(
								this.entity.getValue(fieldName).get(), collectionType,
								componentType.getType());
			}
			else {
				return this.conversion.convertOnRead(
						this.entity.getValue(fieldName).get(), collectionType,
						componentType.getType());
			}
		}
		catch (ConversionException | DatastoreDataException e) {
			throw new DatastoreDataException("Unable to read property " + fieldName, e);
		}
	}
}
