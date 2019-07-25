/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.datastore.health;

import com.google.cloud.datastore.Query;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.gcp.autoconfigure.datastore.DatastoreProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * A simple implementation of a {@link HealthIndicator} returning status information for
 * Google Cloud Datastore.
 *
 * @author Raghavan N S
 * @author Srinivasa Meenavalli
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
@Component
public class DatastoreHealthIndicator extends AbstractHealthIndicator {

	private final DatastoreProvider datastore;

	/**
	 * DatastoreHealthIndicator constructor.
	 *
	 * @param datastore Datastore supplier
	 */
	public DatastoreHealthIndicator(final DatastoreProvider datastore) {
		super("Datastore health check failed");
		Assert.notNull(datastore, "Datastore supplier must not be null");
		this.datastore = datastore;
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) {
		datastore.get().run(Query.newKeyQueryBuilder().setKind("__Stat_Total__").build());
		builder.up();
	}
}
