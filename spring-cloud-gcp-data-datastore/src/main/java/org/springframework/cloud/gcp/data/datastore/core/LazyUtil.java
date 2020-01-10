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
import java.util.function.Supplier;

import com.google.cloud.datastore.Value;

import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.util.Assert;

/**
 * Utilities used to support lazy loaded properties.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.3
 */
final class LazyUtil {

	static private final ObjenesisStd objenesis = new ObjenesisStd();

	private LazyUtil() {
	}

	/**
	 * Returns a proxy that lazily loads the value provided by a supplier. The proxy also
	 * stores the key(s) that can be used in case the value was not loaded. If the type of the
	 * value is interface, {@link java.lang.reflect.Proxy} is used, otherwise cglib proxy is
	 * used (creates a sub-class of the original type; the original class can't be final and
	 * can't have final methods).
	 * @param supplierFunc a function that provides the value
	 * @param type the type of the value
	 * @param keys Datastore key(s) that can be used when the parent entity is saved
	 * @return true if the object is a proxy that was not evaluated
	 */
	static <T> T wrapSimpleLazyProxy(Supplier<T> supplierFunc, Class<T> type, Value keys) {
		if (type.isInterface()) {
			return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] {type},
				new SimpleLazyDynamicInvocationHandler<T>(supplierFunc, keys));
		}
		Factory factory = (Factory) objenesis.newInstance(getEnhancedTypeFor(type));
		factory.setCallbacks(new Callback[] { new SimpleLazyDynamicInvocationHandler<T>(supplierFunc, keys) });

		return (T) factory;
	}

	private static Class<?> getEnhancedTypeFor(Class<?> type) {
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallbackType(org.springframework.cglib.proxy.MethodInterceptor.class);

		return enhancer.createClass();
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
	public static Value getKeys(Object object) {
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
		else if (object instanceof Factory) {
			Callback[] callbacks = ((Factory) object).getCallbacks();
			if (callbacks != null && callbacks.length == 1
					&& callbacks[0] instanceof SimpleLazyDynamicInvocationHandler) {
				return (SimpleLazyDynamicInvocationHandler) callbacks[0];
			}
		}
		return null;
	}

	/**
	 * Proxy class used for lazy loading.
	 */
	public static final class SimpleLazyDynamicInvocationHandler<T> implements InvocationHandler, MethodInterceptor {

		private final Supplier<T> supplierFunc;

		private final Value keys;

		private boolean isEvaluated = false;

		private T value;

		private SimpleLazyDynamicInvocationHandler(Supplier<T> supplierFunc, Value keys) {
			Assert.notNull(supplierFunc, "A non-null supplier function is required for a lazy proxy.");
			this.supplierFunc = supplierFunc;
			this.keys = keys;
		}

		private boolean isEvaluated() {
			return this.isEvaluated;
		}

		public Value getKeys() {
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

		@Override
		public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
			return invoke(o, method, objects);
		}
	}
}
