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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.cloud.ByteArray;

import org.springframework.util.Assert;

/**
 * Utility functions used in conversion operations.
 *
 * @author Balint Pato
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public final class ConversionUtils {

	private ConversionUtils() {
	}

	static Class boxIfNeeded(Class propertyType) {
		if (propertyType == null) {
			return null;
		}
		return propertyType.isPrimitive()
				? Array.get(Array.newInstance(propertyType, 1), 0).getClass()
				: propertyType;
	}

	public static boolean isIterableNonByteArrayType(Class propType) {
		return Iterable.class.isAssignableFrom(propType) && !ByteArray.class.isAssignableFrom(propType);
	}

	static <T> Iterable<T> convertIterable(
			Iterable<Object> source, Class<T> targetType, SpannerCustomConverter converter) {
		List<T> result = new ArrayList<>();
		source.forEach((item) -> result.add(converter.convert(item, targetType)));
		return result;
	}

	public static <T> T wrapSimpleLazyProxy(Supplier<T> supplierFunc, Class<T> type) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type },
				new SimpleLazyDynamicInvocationHandler<>(supplierFunc));
	}

	public static boolean ignoreForWriteLazyProxy(Object object) {
		if (Proxy.isProxyClass(object.getClass())
				&& (Proxy.getInvocationHandler(object) instanceof SimpleLazyDynamicInvocationHandler)) {
			SimpleLazyDynamicInvocationHandler handler = (SimpleLazyDynamicInvocationHandler) Proxy
					.getInvocationHandler(object);
			return !handler.isEvaluated();
		}
		return false;
	}

	private static final class SimpleLazyDynamicInvocationHandler<T> implements InvocationHandler {

		private final Supplier<T> supplierFunc;

		private boolean isEvaluated = false;

		private T value;

		private SimpleLazyDynamicInvocationHandler(Supplier<T> supplierFunc) {
			Assert.notNull(supplierFunc, "A non-null supplier function is required for a lazy proxy.");
			this.supplierFunc = supplierFunc;
		}

		private boolean isEvaluated() {
			return this.isEvaluated;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (!this.isEvaluated) {
				this.value = this.supplierFunc.get();
				this.isEvaluated = true;
			}
			return method.invoke(this.value, args);
		}
	}
}
