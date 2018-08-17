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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.util.ClassUtils;

/**
 * In order to support {@link CustomConversions}, this class applies 2-step conversions.
 * The first step produces one of {@link org.springframework.data.mapping.model.SimpleTypeHolder}'s simple types.
 * The second step converts simple types to Datastore-native types.
 * The second step is skipped if the first one produces a Datastore-native type.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class TwoStepsConversions implements ReadWriteConversions {
	private final GenericConversionService conversionService;

	private final GenericConversionService internalConversionService;

	private final CustomConversions customConversions;

	private final Map<Class, Optional<Class<?>>> writeConverters = new HashMap<>();

	public TwoStepsConversions(CustomConversions customConversions) {
		this.conversionService = new DefaultConversionService();
		this.internalConversionService = new DefaultConversionService();
		this.customConversions = customConversions;
		this.customConversions.registerConvertersIn(this.conversionService);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convertOnRead(Object val, Class<?> targetType) {
		if (val == null) {
			return null;
		}
		Object result = null;
		TypeTargets typeTargets = computeTypeTargets(targetType);

		if (typeTargets.getFirstStepTarget() == null && typeTargets.getSecondStepTarget() == null
				&& ClassUtils.isAssignable(targetType, val.getClass())) {
			//neither first or second steps were applied, no conversion is necessary
			result = val;
		}
		else if (typeTargets.getFirstStepTarget() == null && typeTargets.getSecondStepTarget() != null) {
			//only second step was applied on write
			result = this.internalConversionService.convert(val, targetType);
		}
		else if (typeTargets.getFirstStepTarget() != null && typeTargets.getSecondStepTarget() == null) {
			//only first step was applied on write
			result = this.conversionService.convert(val, targetType);
		}
		else if (typeTargets.getFirstStepTarget() != null && typeTargets.getSecondStepTarget() != null) {
			//both steps were applied
			Object secondStepVal = this.internalConversionService.convert(val, typeTargets.getFirstStepTarget());
			result = this.conversionService.convert(secondStepVal, targetType);
		}
		return (T) result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convertOnWrite(Object propertyVal) {
		Object result = propertyVal;
		if (result != null) {
			TypeTargets typeTargets = computeTypeTargets(result.getClass());
			if (typeTargets.getFirstStepTarget() != null) {
				result = this.conversionService.convert(propertyVal, typeTargets.getFirstStepTarget());
			}

			if (typeTargets.getSecondStepTarget() != null) {
				result = this.internalConversionService.convert(result, typeTargets.getSecondStepTarget());
			}
		}
		return (T) result;
	}

	private TypeTargets computeTypeTargets(Class<?> firstStepSource) {
		Class<?> firstStepTarget = null;
		Class<?> secondStepTarget = null;

		if (!DatastoreNativeTypes.isNativeType(firstStepSource)) {
			Optional<Class<?>> simpleType = this.customConversions.getCustomWriteTarget(firstStepSource);
			if (simpleType.isPresent()) {
				firstStepTarget = simpleType.get();
			}

			Class<?> effectiveFirstStepTarget =
					firstStepTarget == null ? firstStepSource : firstStepTarget;

			Optional<Class<?>> datastoreBasicType = getCustomWriteTarget(effectiveFirstStepTarget);

			if (datastoreBasicType.isPresent()) {
				secondStepTarget = datastoreBasicType.get();
			}
		}
		return new TypeTargets(firstStepTarget, secondStepTarget);
	}

	private Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
		if (DatastoreNativeTypes.isNativeType(sourceType)) {
			return Optional.empty();
		}
		return this.writeConverters.computeIfAbsent(sourceType, this::getSimpleTypeWithBidirectionalConversion);
	}

	private Optional<Class<?>> getSimpleTypeWithBidirectionalConversion(Class inputType) {
		return DatastoreNativeTypes.DATASTORE_NATIVE_TYPES.stream()
				.filter(simpleType ->
						this.internalConversionService.canConvert(inputType, simpleType)
								&& this.internalConversionService.canConvert(simpleType, inputType))
				.findAny();
	}

	private static class TypeTargets {
		private Class<?> firstStepTarget;

		private Class<?> secondStepTarget;

		TypeTargets(Class<?> firstStepTarget, Class<?> secondStepTarget) {
			this.firstStepTarget = firstStepTarget;
			this.secondStepTarget = secondStepTarget;
		}

		Class<?> getFirstStepTarget() {
			return this.firstStepTarget;
		}

		Class<?> getSecondStepTarget() {
			return this.secondStepTarget;
		}
	}
}
