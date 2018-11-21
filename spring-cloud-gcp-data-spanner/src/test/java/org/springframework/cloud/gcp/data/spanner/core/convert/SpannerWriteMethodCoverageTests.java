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

import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.ValueBinder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Chengyuan Zhao
 */
public class SpannerWriteMethodCoverageTests {

	// Checks that the converter is aware of all Cloud Spanner mutation binder types
	@Test
	public void allKnownMappingTypesTest() throws NoSuchFieldException {
		for (Method method : ValueBinder.class.getMethods()) {

			String methodName = method.getName();

			// ignoring non-public and non "to" named binder methods
			if (!Modifier.isPublic(method.getModifiers()) || !methodName.startsWith("to")
					|| method.getParameterCount() != 1) {
				continue;
			}

			Class<?> paramType = ConversionUtils.boxIfNeeded(method.getParameterTypes()[0]);
			if (paramType.equals(Struct.class) || paramType.equals(Value.class)) {
				/*
				 * 1. there is a method for binding a Struct value, but because Struct
				 * values cannot be written to table columns we will ignore it. 2. there
				 * is a method for binding a Value value. However, the purpose of the
				 * converters is to wrap java types into the Value for the user.
				 * Furthermore, the Cloud Spanner client lib does not give a way to read a
				 * Value back from a Struct, so we will ignore this method.
				 */
				continue;
			}
			else if (ConversionUtils.isIterableNonByteArrayType(paramType)) {
				Class<?> innerParamType = (Class) ((ParameterizedType) method
						.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
				assertThat(ConverterAwareMappingSpannerEntityWriter.iterablePropertyType2ToMethodMap
						.keySet(), hasItem(innerParamType));
			}
			else {
				assertThat(
						ConverterAwareMappingSpannerEntityWriter.singleItemType2ToMethodMap.keySet(),
						hasItem(paramType));
			}
		}
	}
}
