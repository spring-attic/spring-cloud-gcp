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
import java.util.Collections;
import java.util.Optional;

import com.google.common.collect.ImmutableList;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerWriteConverter extends SpannerCustomConverter {

	public SpannerWriteConverter() {
		this((Collection<Converter>) null);
	}

	public SpannerWriteConverter(Collection<Converter> readConverters) {
		this(getCustomConversions(ImmutableList.<Converter>builder()
				.addAll(SpannerConverters.DEFAULT_SPANNER_WRITE_CONVERTERS)
				.addAll(Optional.ofNullable(readConverters)
						.orElse(Collections.emptyList()))
				.build()));
	}

	public SpannerWriteConverter(CustomConversions customConversions) {
		this(customConversions, null);
	}

	public SpannerWriteConverter(CustomConversions customConversions, GenericConversionService conversionService) {
		super(customConversions, conversionService);
	}
}
