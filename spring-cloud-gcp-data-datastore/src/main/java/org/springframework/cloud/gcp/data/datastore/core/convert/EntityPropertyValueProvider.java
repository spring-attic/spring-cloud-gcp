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

import com.google.cloud.datastore.BaseEntity;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.cloud.gcp.data.datastore.core.mapping.EmbeddedType;
import org.springframework.core.convert.ConversionException;
import org.springframework.data.mapping.model.PropertyValueProvider;
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
		if (!persistentProperty.isColumnBacked()) {
			return null;
		}
		return getPropertyValue(persistentProperty.getFieldName(),
				persistentProperty.getEmbeddedType(),
				persistentProperty.getTypeInformation());
	}

	@SuppressWarnings("unchecked")
	public <T> T getPropertyValue(String fieldName, EmbeddedType embeddedType, TypeInformation targetTypeInformation) {
		if (!this.entity.contains(fieldName)) {
			return null;
		}

		try {
			return this.conversion.convertOnRead(this.entity.getValue(fieldName).get(),
					embeddedType, targetTypeInformation);
		}
		catch (ConversionException | DatastoreDataException e) {
			throw new DatastoreDataException("Unable to read property " + fieldName, e);
		}
	}
}
