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

package org.springframework.cloud.gcp.data.spanner.core.admin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.cloud.spanner.DatabaseClient;

/**
 * A supplier of database clients that relies on another supplier and caches provided
 * results.
 *
 * @param <U> the type of objects this supplier bases its products on.
 * @author Chengyuan Zhao
 */
public class CachingComposingDatabaseClientSupplier<U> implements Supplier<DatabaseClient> {

	private final Map<U, DatabaseClient> products = new ConcurrentHashMap<>();

	private final Supplier<U> inputProvider;

	private final Function<U, DatabaseClient> producer;

	private final ThreadLocal<DatabaseClient> currentClient = ThreadLocal.withInitial(() -> null);

	/**
	 * Constructor.
	 * @param inputProvider the provider that gives inputs for each product of this provider.
	 * @param producer the function that returns products of this provider given inputs.
	 */
	public CachingComposingDatabaseClientSupplier(Supplier<U> inputProvider, Function<U, DatabaseClient> producer) {
		this.inputProvider = inputProvider;
		this.producer = producer;
	}

	@Override
	public DatabaseClient get() {
		return this.products.computeIfAbsent(this.inputProvider.get(), this.producer);
	}

	/**
	 * Set a database client that will be returned by
	 * {@code getComputedOrCurrentThreadLocalDatabaseClient}.
	 * @param databaseClient a database client.
	 */
	public void setCurrentThreadLocalDatabaseClient(DatabaseClient databaseClient) {
		this.currentClient.set(databaseClient);
	}

	/**
	 * Return the current thread-local database client if there is one set. Otherwise, return
	 * the computed or cached database client.
	 * @return a database client.
	 */
	public DatabaseClient getComputedOrCurrentThreadLocalDatabaseClient() {
		if (this.currentClient.get() != null) {
			return this.currentClient.get();
		}
		return get();
	}

	/**
	 * Unset the current thread local database client.
	 */
	public void clearCurrentThreadLocalDatabaseClient() {
		this.currentClient.remove();
	}
}
