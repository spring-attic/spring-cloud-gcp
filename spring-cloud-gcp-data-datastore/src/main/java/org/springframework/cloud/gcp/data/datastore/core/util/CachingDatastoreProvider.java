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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.cloud.datastore.Datastore;

/**
 * A provider of Datastore client objects that chain from another provider of config
 * inputs. For example, a supplier of namespace strings can be used to create a provider
 * of clients each with a different namespace.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
public class CachingDatastoreProvider<A> implements Supplier<Datastore> {

	private final Map<A, Datastore> clients = new ConcurrentHashMap<>();

	private final Supplier<A> inputProvider;

	private final Function<A, Datastore> producer;

	/**
	 * Constructor.
	 * @param inputProvider the provider that gives inputs for each Datstore client .
	 * @param producer the function that returns products of this provider given inputs.
	 */
	public CachingDatastoreProvider(Supplier<A> inputProvider, Function<A, Datastore> producer) {
		this.inputProvider = inputProvider;
		this.producer = producer;
	}

	@Override
	public Datastore get() {
		return this.clients.computeIfAbsent(inputProvider.get(), this.producer);
	}
}
