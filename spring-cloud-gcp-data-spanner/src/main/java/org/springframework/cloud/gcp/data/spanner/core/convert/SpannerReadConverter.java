/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;

/**
 * A custom converter used for reading from Spanner data types.
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 * @since 1.1
 */
public class SpannerReadConverter extends SpannerCustomConverter {

	public SpannerReadConverter() {
		this((Collection<Converter>) null);
	}

	public SpannerReadConverter(Collection<Converter> readConverters) {
		this(getCustomConversions(Stream
				.<Converter>concat(
						SpannerConverters.DEFAULT_SPANNER_READ_CONVERTERS.stream(),
						Optional.ofNullable(readConverters)
								.orElse(Collections.emptyList()).stream())
				.collect(Collectors.toList())));

	}

	public SpannerReadConverter(CustomConversions customConversions) {
		this(customConversions, null);
	}

	public SpannerReadConverter(CustomConversions customConversions,
			GenericConversionService conversionService) {
		super(customConversions, conversionService);
	}

}
