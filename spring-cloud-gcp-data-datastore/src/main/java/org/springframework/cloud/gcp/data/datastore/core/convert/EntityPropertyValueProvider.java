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
import org.springframework.core.convert.ConversionService;
import org.springframework.data.convert.CustomConversions;
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

	private ConversionService conversionService;

	private ConversionService internalConversionService;

	private CustomConversions customConversions;

	private DefaultDatastoreEntityConverter.DatastoreSimpleTypes datastoreSimpleTypes;

	EntityPropertyValueProvider(Entity entity, ConversionService conversionService,
			ConversionService internalConversionService, CustomConversions customConversions,
			DefaultDatastoreEntityConverter.DatastoreSimpleTypes datastoreSimpleTypes) {
		this.entity = entity;
		this.conversionService = conversionService;
		this.internalConversionService = internalConversionService;
		this.customConversions = customConversions;
		this.datastoreSimpleTypes = datastoreSimpleTypes;
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
		if (val == null) {
			return null;
		}

		Class<?> targetType = persistentProperty.getType();
		T result = null;

		DefaultDatastoreEntityConverter.TwoStepConversion twoStepConversion =
				new DefaultDatastoreEntityConverter.TwoStepConversion(
						targetType,
						this.customConversions,
						this.datastoreSimpleTypes);

		if (twoStepConversion.getFirstStepTarget() == null && twoStepConversion.getSecondStepTarget() == null
				&& ClassUtils.isAssignable(targetType, val.getClass())) {
			//neither first or second steps were applied, no conversion is necessary
			result = (T) val;
		}
		else if (twoStepConversion.getFirstStepTarget() == null && twoStepConversion.getSecondStepTarget() != null) {
			//only second step was applied on write
			result = (T) this.internalConversionService.convert(val, targetType);
		}
		else if (twoStepConversion.getFirstStepTarget() != null && twoStepConversion.getSecondStepTarget() == null) {
			//only first step was applied on write
			result = (T) this.conversionService.convert(val, targetType);
		}
		else if (twoStepConversion.getFirstStepTarget() != null && twoStepConversion.getSecondStepTarget() != null) {
			//both steps were applied
			val = this.internalConversionService.convert(val, twoStepConversion.getFirstStepTarget());
			result = (T) this.conversionService.convert(val, targetType);
		}

		if (result != null) {
			return result;
		}
		throw new DatastoreDataException("The value in entity's property with name " + persistentProperty.getFieldName()
				+ " could not be converted to the corresponding property in the class. " +
				"The property's type is " + targetType + " but the value's type is " + val.getClass());
	}

}
