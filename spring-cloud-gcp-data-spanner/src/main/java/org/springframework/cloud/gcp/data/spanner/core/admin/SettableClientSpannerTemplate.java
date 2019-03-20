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

import java.util.function.Supplier;

import com.google.cloud.spanner.DatabaseClient;

import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.util.Assert;

/**
 * This class is intended for users to override with a bean to configure per-method
 * param-based database clients. It cannot cannot execute real operations.
 *
 * @author Chengyuan Zhao
 */
public class SettableClientSpannerTemplate extends SpannerTemplate {

	/**
	 * Set the database client to use for operations.
	 * @param databaseClient the client.
	 */
	public void setDatabaseClient(DatabaseClient databaseClient) {
		Assert.notNull(databaseClient, "The database client must not be null.");
		setDatabaseClientProvider(() -> databaseClient);
	}

	/**
	 * Get the database client provider this template will use for operations and queries.
	 * @return the database client provider that will be used.
	 */
	public Supplier<DatabaseClient> getDatabaseClientProvider() {
		return this.databaseClientProvider;
	}
}