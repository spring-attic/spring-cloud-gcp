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

package org.springframework.cloud.gcp.autoconfigure.datastore.health;


import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Query;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * Simple implementation of a {@link HealthIndicator} returning status information for Google cloud
 * data store.
 *
 * @author Raghavan N S
 * @author Srinivasa Meenavalli
 */
@Component
public class DatastoreHealthIndicator extends AbstractHealthIndicator {

	private static final Status DATASTORE_HEALTH = new Status("DATASTORE_HEALTH");

	private Datastore datastore;

	/**
	 * DatastoreHealthIndicator constructor.
	 *
	 * @param datastore Datastore
	 */
	public DatastoreHealthIndicator(final Datastore datastore) {
		super("Datastore health check failed");
		this.datastore = datastore;
		Assert.notNull(datastore, "Datastore must not be null");
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		try {
			datastore.run(Query.newKeyQueryBuilder().setKind("__Stat_Total__").build());
			builder.status(DATASTORE_HEALTH);
			builder.up();
		}
		catch (Exception ex) {
			builder.down();
			throw ex;
		}
	}
}
