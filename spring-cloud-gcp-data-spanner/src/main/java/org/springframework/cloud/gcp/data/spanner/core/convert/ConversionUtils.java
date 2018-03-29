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

import java.lang.reflect.Array;
import java.util.Collection;

import com.google.cloud.ByteArray;
import com.google.common.collect.ImmutableSet;

import org.springframework.core.convert.converter.Converter;


/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
class ConversionUtils {

	/**
	 * Converters from common types to those used by Spanner.
	 */
	public static final Collection<Converter> DEFAULT_SPANNER_WRITE_CONVERTERS = ImmutableSet
			.of();

	/**
	 * Converters from common types to those used by Spanner.
	 */
	public static final Collection<Converter> DEFAULT_SPANNER_READ_CONVERTERS = ImmutableSet
			.of();

	static Class boxIfNeeded(Class propertyType) {
		if (propertyType == null) {
			return null;
		}
		return propertyType.isPrimitive()
				? Array.get(Array.newInstance(propertyType, 1), 0).getClass()
				: propertyType;
	}

	static boolean isIterableNonByteArrayType(Class propType) {
		return Iterable.class.isAssignableFrom(propType)
				&& !ByteArray.class.isAssignableFrom(propType);
	}

}
