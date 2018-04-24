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

import com.google.cloud.spanner.ValueBinder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

/**
 * @author Chengyuan Zhao
 */
public class MappingSpannerWriteConverterTests {

	// Checks that the converter is aware of all Spanner mutation binder types
	@Test
	public void allKnownMappingTypesTest() throws NoSuchFieldException {
		for (Method method : ValueBinder.class.getMethods()) {

			String methodName = method.getName();

			// ignoring non-public and non "to" named binder methods
			if (!Modifier.isPublic(method.getModifiers()) || !methodName.startsWith("to")
					|| method.getParameterCount() != 1) {
				continue;
			}

			Class paramType = ConversionUtils.boxIfNeeded(method.getParameterTypes()[0]);
			if (ConversionUtils.isIterableNonByteArrayType(paramType)) {
				Class innerParamType = (Class) ((ParameterizedType) method
						.getGenericParameterTypes()[0]).getActualTypeArguments()[0];
				assertThat(MappingSpannerWriteConverter.iterablePropertyType2ToMethodMap
						.keySet(), hasItem(innerParamType));
			}
			else {
				assertThat(
						MappingSpannerWriteConverter.singleItemType2ToMethodMap.keySet(),
						hasItem(paramType));
			}
		}
	}
}
