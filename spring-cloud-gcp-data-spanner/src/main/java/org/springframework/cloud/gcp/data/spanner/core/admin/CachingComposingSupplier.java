/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A supplier of objects that relies on another supplier and caches
 * provided results.
 *
 * @param <T> the type of objects this supplier produces.
 * @param <U> the type of objects this supplier bases its products on.
 * @author Chengyuan Zhao
 */
public class CachingComposingSupplier<T, U> implements Supplier<T> {

	private final Map<U, T> products = new ConcurrentHashMap<>();

	private final Supplier<U> inputProvider;

	private final Function<U, T> producer;

	/**
	 * Constructor.
	 * @param inputProvider the provider that gives inputs for each product of this provider.
	 * @param producer the function that returns products of this provider given inputs.
	 */
	public CachingComposingSupplier(Supplier<U> inputProvider, Function<U, T> producer) {
		this.inputProvider = inputProvider;
		this.producer = producer;
	}

	@Override
	public T get() {
		return this.products.computeIfAbsent(inputProvider.get(), this.producer);
	}
}
