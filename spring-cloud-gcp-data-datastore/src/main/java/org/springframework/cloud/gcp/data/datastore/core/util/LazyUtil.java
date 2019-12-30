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

package org.springframework.cloud.gcp.data.datastore.core.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Supplier;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.util.Assert;

/**
 * @author Dmitry Solomakha
 */
final public class LazyUtil {

	private LazyUtil() {
	}

	public static <T> T wrapSimpleLazyProxy(Supplier<T> supplierFunc, Class<T> type, List keys) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type},
				new SimpleLazyDynamicInvocationHandler<T>(supplierFunc, keys));
	}

	public static boolean hasUsableKeys(Object object) {
		if (Proxy.isProxyClass(object.getClass())
				&& (Proxy.getInvocationHandler(object) instanceof SimpleLazyDynamicInvocationHandler)) {
			SimpleLazyDynamicInvocationHandler handler = (SimpleLazyDynamicInvocationHandler) Proxy
					.getInvocationHandler(object);
			return !handler.isEvaluated() && handler.getKeys() != null;
		}
		return false;
	}

	public static List getKeys(Object object) {
		if (Proxy.isProxyClass(object.getClass())
				&& (Proxy.getInvocationHandler(object) instanceof SimpleLazyDynamicInvocationHandler)) {
			SimpleLazyDynamicInvocationHandler handler = (SimpleLazyDynamicInvocationHandler) Proxy
					.getInvocationHandler(object);
			if (!handler.isEvaluated()) {
				return handler.getKeys();
			}
		}
		return null;
	}

	public static final class SimpleLazyDynamicInvocationHandler<T> implements InvocationHandler {

		private final Supplier<T> supplierFunc;

		private final List keys;

		private boolean isEvaluated = false;

		private T value;

		private SimpleLazyDynamicInvocationHandler(Supplier<T> supplierFunc, List keys) {
			Assert.notNull(supplierFunc, "A non-null supplier function is required for a lazy proxy.");
			this.supplierFunc = supplierFunc;
			this.keys = keys;
		}

		private boolean isEvaluated() {
			return this.isEvaluated;
		}

		public List getKeys() {
			return this.keys;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (!this.isEvaluated) {
				T value = this.supplierFunc.get();
				if (value == null) {
					throw new DatastoreDataException("Can't load referenced entity");
				}
				this.value = value;

				this.isEvaluated = true;
			}
			return method.invoke(this.value, args);
		}
	}

}
