/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core.util;

import org.springframework.util.CollectionUtils;

/**
 * Utility class containing functions for working with types related to Cloud Datastore.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public final class ValueUtil {

	private ValueUtil() {
	}

	public static Object toListIfArray(Object val) {
		if (val != null && isArrayOfItems(val.getClass())) {
			//if a property is an array, convert it to list
			return CollectionUtils.arrayToList(val);
		}
		return val;
	}

	public static boolean isCollectionLike(Class type) {
		return Iterable.class.isAssignableFrom(type) || isArrayOfItems(type);
	}

	private static boolean isArrayOfItems(Class type) {
		return type.isArray() && type != byte[].class;
	}
}
