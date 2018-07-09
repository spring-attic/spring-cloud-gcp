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

import java.util.Collection;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 *
 * @since 1.1
 */
public abstract class SpannerCustomConverter {

	private final ConfigurableConversionService conversionService;

	/**
	 * Constructor
	 * @param customConversions must not be null.
	 * @param conversionService if null, then {@link DefaultConversionService} is used.
	 */
	SpannerCustomConverter(CustomConversions customConversions,
																GenericConversionService conversionService) {
		Assert.notNull(customConversions, "Valid custom conversions are required!");
		this.conversionService = conversionService == null
				? new DefaultConversionService()
				: conversionService;

		customConversions.registerConvertersIn(this.conversionService);
	}

	boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		Class boxedTargetType = ConversionUtils.boxIfNeeded(targetType);
		Class boxedSourceType = ConversionUtils.boxIfNeeded(sourceType);
		return boxedSourceType.equals(boxedTargetType)
				|| this.conversionService.canConvert(boxedSourceType, boxedTargetType);
	}

	@SuppressWarnings("unchecked")
	public <T> T convert(Object sourceValue, Class<T> targetType) {
		Class<?> boxedSourceType = ConversionUtils.boxIfNeeded(sourceValue.getClass());
		Class<T> boxedTargetType = ConversionUtils.boxIfNeeded(targetType);
		return boxedTargetType.isAssignableFrom(boxedSourceType)
						? (T) sourceValue
						: this.conversionService.convert(sourceValue, boxedTargetType);
	}

	static CustomConversions getCustomConversions(
					Collection<Converter> converters) {
		return new CustomConversions(CustomConversions.StoreConversions.NONE, converters);
	}

}
