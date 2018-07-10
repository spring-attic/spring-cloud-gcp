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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Set;

import com.google.cloud.spanner.Struct;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Chengyuan Zhao
 */
public class SpannerStructReadMethodCoverageTests {

	private static final Set<String> DISREGARDED_METHOD_NAMES = ImmutableSet
			.<String>builder()
			.add("getColumnIndex")
			.add("getStructList")
			.add("getColumnType")
			.build();

	// Checks that the converter is aware of all Spanner struct getter types
	@Test
	public void allKnownMappingTypesTest() throws NoSuchFieldException {
		for (Method method : Struct.class.getMethods()) {
			String methodName = method.getName();
			// ignoring private methods, ones not named like a getter. Getters must also
			// only take the column index or name
			if (!Modifier.isPublic(method.getModifiers()) || !methodName.startsWith("get")
					|| method.getParameterCount() != 1
					|| DISREGARDED_METHOD_NAMES.contains(methodName)) {
				continue;
			}
			Class returnType = ConversionUtils.boxIfNeeded(method.getReturnType());
			if (ConversionUtils.isIterableNonByteArrayType(returnType)) {
				Class innerReturnType = (Class) ((ParameterizedType) method
						.getGenericReturnType()).getActualTypeArguments()[0];
				assertThat(StructAccessor.readIterableMapping.keySet(),
						hasItem(innerReturnType));
			}
			else {
				assertThat(
						StructAccessor.singleItemReadMethodMapping.keySet(),
						hasItem(returnType));
			}
		}
	}
}
