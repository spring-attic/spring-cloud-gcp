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

import java.util.Map;

import com.google.cloud.ByteArray;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.Type.Code;
import com.google.common.collect.ImmutableMap;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 *
 * @since 1.1
 */
public class SpannerTypeMapper {

	private static final Map<Class, Type.Code> JAVA_TYPE_TO_SPANNER_SIMPLE_COLUMN_TYPE_MAPPING;

	private static final Map<Class, Type.Code> JAVA_TYPE_TO_SPANNER_ARRAY_COLUMN_TYPE_MAPPING;

	// @formatter:off
	private static final Map<Type.Code, Class> SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING =
					new ImmutableMap.Builder<Type.Code, Class>()
					// @formatter:on
					.put(Type.Code.BOOL, Boolean.class)
					.put(Type.Code.BYTES, ByteArray.class)
					.put(Type.Code.DATE, com.google.cloud.Date.class)
					.put(Type.Code.FLOAT64, Double.class)
					.put(Type.Code.INT64, Long.class)
					.put(Type.Code.STRING, String.class)
					.put(Type.Code.STRUCT, Struct.class)
					.put(Type.Code.TIMESTAMP, Timestamp.class)
					.build();

	// @formatter:off
	private static final Map<Type.Code, Class> SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING
					= new ImmutableMap.Builder<Type.Code, Class>()
					// @formatter:on
					.put(Type.Code.BOOL, boolean[].class)
					.put(Type.Code.BYTES, ByteArray[].class)
					.put(Type.Code.DATE, com.google.cloud.Date[].class)
					.put(Type.Code.FLOAT64, double[].class)
					.put(Type.Code.INT64, long[].class)
					.put(Type.Code.STRING, String[].class)
					.put(Type.Code.STRUCT, Struct[].class)
					.put(Type.Code.TIMESTAMP, Timestamp[].class)
					.build();

	static {
		ImmutableMap.Builder<Class, Type.Code> builder = new ImmutableMap.Builder<>();
		SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING
				.keySet()
				.stream()
				.forEach(
						type -> builder.put(SPANNER_SIMPLE_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(type), type));
		builder.put(double.class, Code.FLOAT64);
		builder.put(long.class, Code.INT64);
		JAVA_TYPE_TO_SPANNER_SIMPLE_COLUMN_TYPE_MAPPING = builder.build();
	}

	static {
		ImmutableMap.Builder<Class, Type.Code> builder = new ImmutableMap.Builder<>();
		SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING
				.keySet()
				.stream()
				.forEach(
						type -> builder.put(SPANNER_ARRAY_COLUMN_CODES_TO_JAVA_TYPE_MAPPING.get(type), type));
		JAVA_TYPE_TO_SPANNER_ARRAY_COLUMN_TYPE_MAPPING = builder.build();
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
