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

import java.util.Optional;

import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.util.ClassUtils;

/**
 * In order to support {@link CustomConversions}, this class applies 2-step conversions.
 * The first step produces one of {@link SimpleTypeHolder}'s simple types.
 * The second step converts simple types to Datastore-native types.
 * The second step is skipped if the first one produces a Datastore-native type.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
class TwoStepsConversions implements ReadWriteConversions {
	private final GenericConversionService conversionService;

	private final GenericConversionService internalConversionService;

	private final DatastoreSimpleTypes toNativeConversions;

	private final CustomConversions customConversions;

	TwoStepsConversions(CustomConversions customConversions) {
		this.conversionService = new DefaultConversionService();
		this.internalConversionService = new DefaultConversionService();
		this.customConversions = customConversions;
		this.toNativeConversions = new DatastoreSimpleTypes(this.internalConversionService);
		this.customConversions.registerConvertersIn(this.conversionService);
	}

	TwoStepsConversions(GenericConversionService conversionService,
						GenericConversionService internalConversionService, DatastoreSimpleTypes toNativeConversions,
						CustomConversions customConversions) {
		this.conversionService = conversionService;
		this.internalConversionService = internalConversionService;
		this.toNativeConversions = toNativeConversions;
		this.customConversions = customConversions;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convertOnRead(Object val, Class<?> targetType) {
		if (val == null) {
			return null;
		}
		Object result = null;
		TwoStepConversion twoStepConversion = getTwoStepsConversion(targetType);

		if (twoStepConversion.getFirstStepTarget() == null && twoStepConversion.getSecondStepTarget() == null
				&& ClassUtils.isAssignable(targetType, val.getClass())) {
			//neither first or second steps were applied, no conversion is necessary
			result = val;
		}
		else if (twoStepConversion.getFirstStepTarget() == null && twoStepConversion.getSecondStepTarget() != null) {
			//only second step was applied on write
			result = this.internalConversionService.convert(val, targetType);
		}
		else if (twoStepConversion.getFirstStepTarget() != null && twoStepConversion.getSecondStepTarget() == null) {
			//only first step was applied on write
			result = this.conversionService.convert(val, targetType);
		}
		else if (twoStepConversion.getFirstStepTarget() != null && twoStepConversion.getSecondStepTarget() != null) {
			//both steps were applied
			Object secondStepVal = this.internalConversionService.convert(val, twoStepConversion.getFirstStepTarget());
			result = this.conversionService.convert(secondStepVal, targetType);
		}
		return (T) result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convertOnWrite(Object propertyVal) {
		Object result = propertyVal;
		if (result != null) {
			TwoStepConversion twoStepConversion = getTwoStepsConversion(result.getClass());
			if (twoStepConversion.getFirstStepTarget() != null) {
				result = this.conversionService.convert(propertyVal, twoStepConversion.getFirstStepTarget());
			}

			if (twoStepConversion.getSecondStepTarget() != null) {
				result = this.internalConversionService.convert(result, twoStepConversion.getSecondStepTarget());
			}
		}
		return (T) result;
	}

	private TwoStepConversion getTwoStepsConversion(Class<?> firstStepSource) {
		Class<?> firstStepTarget = null;
		Class<?> secondStepTarget = null;

		if (!DatastoreSimpleTypes.isSimple(firstStepSource)) {
			Optional<Class<?>> simpleType = this.customConversions.getCustomWriteTarget(firstStepSource);
			if (simpleType.isPresent()) {
				firstStepTarget = simpleType.get();
			}

			Class<?> effectiveFirstStepTarget =
					firstStepTarget == null ? firstStepSource : firstStepTarget;

			Optional<Class<?>> datastoreBasicType =
					this.toNativeConversions.getCustomWriteTarget(effectiveFirstStepTarget);

			if (datastoreBasicType.isPresent()) {
				secondStepTarget = datastoreBasicType.get();
			}
		}
		return new TwoStepConversion(firstStepTarget, secondStepTarget);
	}

	private static class TwoStepConversion {
		private Class<?> firstStepTarget;

		private Class<?> secondStepTarget;

		TwoStepConversion(Class<?> firstStepTarget, Class<?> secondStepTarget) {
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
