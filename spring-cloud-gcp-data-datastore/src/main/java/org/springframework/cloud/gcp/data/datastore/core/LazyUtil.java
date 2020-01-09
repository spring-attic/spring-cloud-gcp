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

package org.springframework.cloud.gcp.data.datastore.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.function.Function;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.util.Assert;

/**
 * Utilities used to support lazy loaded properties.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.3
 */
final class LazyUtil {

	private LazyUtil() {
	}

	public static <T> T wrapSimpleLazyProxy(Function<List<Value<Key>>, T> supplierFunc, Class<T> type, List keys) {
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type},
				new SimpleLazyDynamicInvocationHandler<T>(supplierFunc, keys));
	}

	/**
	 * Check if the object is a lazy loaded proxy that hasn't been evaluated.
	 * @param object an object
	 * @return true if the object is a proxy that was not evaluated
	 */
	public static boolean isLazyAndNotLoaded(Object object) {
		SimpleLazyDynamicInvocationHandler handler = getProxy(object);
		if (handler != null) {
			return !handler.isEvaluated() && handler.getKeys() != null;
		}
		return false;
	}

	/**
	 * Extract keys from a proxy object.
	 * @param object a proxy object
	 * @return list of keys if the object is a proxy, null otherwise
	 */
	public static List getKeys(Object object) {
		SimpleLazyDynamicInvocationHandler handler = getProxy(object);
		if (handler != null) {
			if (!handler.isEvaluated()) {
				return handler.getKeys();
			}
		}
		return null;
	}

	private static SimpleLazyDynamicInvocationHandler getProxy(Object object) {
		if (Proxy.isProxyClass(object.getClass())
				&& (Proxy.getInvocationHandler(object) instanceof SimpleLazyDynamicInvocationHandler)) {
			return (SimpleLazyDynamicInvocationHandler) Proxy
					.getInvocationHandler(object);
		}
		return null;
	}

	/**
	 * Proxy class used for lazy loading.
	 */
	public static final class SimpleLazyDynamicInvocationHandler<T> implements InvocationHandler {

		private final Function<List<Value<Key>>, T> supplierFunc;

		private final List<Value<Key>> keys;

		private boolean isEvaluated = false;

		private T value;

		private SimpleLazyDynamicInvocationHandler(Function<List<Value<Key>>, T> supplierFunc, List<Value<Key>> keys) {
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
				T value = this.supplierFunc.apply(this.keys);
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
