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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.google.cloud.ByteArray;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;

import org.springframework.cloud.gcp.core.util.MapBuilder;

/**
 * A utility class to map between common Java types and types to use with Spanner.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 * @since 1.1
 */
public final class SpannerTypeMapper {

	private SpannerTypeMapper() {
	}

	private static final Map<Class, Type.Code> JAVA_TYPE_TO_SPANNER_SIMPLE_COLUMN_TYPE_MAPPING;

	private static final Map<Class, Type.Code> JAVA_TYPE_TO_SPANNER_ARRAY_COLUMN_TYPE_MAPPING;

	private static final Map<Type.Code, Class> SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING = new MapBuilder<Code, Class>()
			.put(Type.Code.BOOL, Boolean.class).put(Type.Code.BYTES, ByteArray.class)
			.put(Type.Code.DATE, com.google.cloud.Date.class)
			.put(Type.Code.FLOAT64, Double.class).put(Type.Code.INT64, Long.class)
			.put(Type.Code.STRING, String.class).put(Type.Code.STRUCT, Struct.class)
			.put(Type.Code.TIMESTAMP, Timestamp.class).build();

	private static final Map<Type.Code, Class> SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING = new MapBuilder<Type.Code, Class>()
			.put(Type.Code.BOOL, boolean[].class).put(Type.Code.BYTES, ByteArray[].class)
			.put(Type.Code.DATE, com.google.cloud.Date[].class)
			.put(Type.Code.FLOAT64, double[].class).put(Type.Code.INT64, long[].class)
			.put(Type.Code.STRING, String[].class).put(Type.Code.STRUCT, Struct[].class)
			.put(Type.Code.TIMESTAMP, Timestamp[].class).build();

	static {
		Map<Class, Type.Code> builderMap = new HashMap<>();
		SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.keySet().stream()
				.forEach((type) -> builderMap.put(
						SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(type),
						type));
		builderMap.put(double.class, Code.FLOAT64);
		builderMap.put(long.class, Code.INT64);
		JAVA_TYPE_TO_SPANNER_SIMPLE_COLUMN_TYPE_MAPPING = Collections
				.unmodifiableMap(builderMap);
	}

	static {
		HashMap<Class, Type.Code> builderMap = new HashMap();
		SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.keySet().stream()
				.forEach((type) -> builderMap.put(
						SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(type), type));
		JAVA_TYPE_TO_SPANNER_ARRAY_COLUMN_TYPE_MAPPING = Collections
				.unmodifiableMap(builderMap);
	}

	public static Class getSimpleJavaClassFor(Type.Code code) {
		return SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(code);
	}

	public static Class getArrayJavaClassFor(Type.Code code) {
		return SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(code);
	}

	public static Type.Code getSimpleTypeCodeForJavaType(Class spannerJavaType) {
		return JAVA_TYPE_TO_SPANNER_SIMPLE_COLUMN_TYPE_MAPPING.get(spannerJavaType);
	}

	public static Type.Code getArrayTypeCodeForJavaType(Class spannerJavaType) {
		return JAVA_TYPE_TO_SPANNER_ARRAY_COLUMN_TYPE_MAPPING.get(spannerJavaType);
	}

}
