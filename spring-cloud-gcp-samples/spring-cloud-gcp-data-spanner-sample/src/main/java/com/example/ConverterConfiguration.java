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

package com.example;

import java.util.Arrays;

import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Balint Pato
 */
@Configuration
public class ConverterConfiguration {

	@Bean
	public SpannerConverter spannerConverter(SpannerMappingContext spannerMappingContext) {
		return new MappingSpannerConverter(spannerMappingContext,
				Arrays.asList(new Person.PersonWriteConverter()),
				Arrays.asList(new Person.PersonReadConverter()));
	}
}
