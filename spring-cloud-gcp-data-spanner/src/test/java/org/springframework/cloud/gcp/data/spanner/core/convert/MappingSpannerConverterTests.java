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

import java.time.Instant;
import java.util.Arrays;

import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import org.junit.Test;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import static org.junit.Assert.assertTrue;

/**
 * @author Chengyuan Zhao
 */
public class MappingSpannerConverterTests {

	private static final Converter<SpannerType, JavaType> SPANNER_TO_JAVA = new Converter<SpannerType, JavaType>() {
		@Nullable
		@Override
		public JavaType convert(SpannerType source) {
			return new JavaType() {
			};
		}
	};

	private static final Converter<JavaType, SpannerType> JAVA_TO_SPANNER = new Converter<JavaType, SpannerType>() {
		@Nullable
		@Override
		public SpannerType convert(JavaType source) {
			return new SpannerType() {
			};
		}
	};

	@Test
	public void canConvertDefaultTypesNoCustomConverters() {
		MappingSpannerConverter converter = new MappingSpannerConverter(
				new SpannerMappingContext());

		verifyCanConvert(converter, java.util.Date.class, Date.class);
		verifyCanConvert(converter, Instant.class, Timestamp.class);
	}

	@Test
	public void canConvertDefaultTypesCustomConverters() {
		MappingSpannerConverter converter = new MappingSpannerConverter(
				new SpannerMappingContext(), Arrays.asList(JAVA_TO_SPANNER),
				Arrays.asList(SPANNER_TO_JAVA));

		verifyCanConvert(converter, java.util.Date.class, Date.class);
		verifyCanConvert(converter, Instant.class, Timestamp.class);
		verifyCanConvert(converter, JavaType.class, SpannerType.class);
	}

	private void verifyCanConvert(MappingSpannerConverter converter, Class javaType,
			Class spannerType) {
		MappingSpannerWriteConverter writeConverter = converter.getWriteConverter();
		MappingSpannerReadConverter readConverter = converter.getReadConverter();

		assertTrue(converter.canConvert(javaType, spannerType));
		assertTrue(converter.canConvert(spannerType, javaType));

		assertTrue(writeConverter.canConvert(javaType, spannerType));
		assertTrue(readConverter.canConvert(spannerType, javaType));
	}

	private interface SpannerType {
	}

	private interface JavaType {
	}
}
