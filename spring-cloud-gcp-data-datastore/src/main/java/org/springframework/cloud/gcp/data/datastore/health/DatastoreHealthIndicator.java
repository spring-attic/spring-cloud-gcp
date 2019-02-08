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

package org.springframework.cloud.gcp.data.datastore.health;

import java.util.UUID;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.repository.support.SimpleDatastoreRepository;
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

	private final SimpleDatastoreRepository<HealthCheckEntity, String> simpleDatastoreRepository;

	/**
	 * DatastoreHealthIndicator constructor.
	 *
	 * @param datastoreTemplate DatastoreTemplate
	 */
	public DatastoreHealthIndicator(DatastoreTemplate datastoreTemplate) {
		super("Datastore health check failed");
		Assert.notNull(datastoreTemplate, "DatastoreTemplate must not be null");
		this.simpleDatastoreRepository = new SimpleDatastoreRepository<>(datastoreTemplate,	HealthCheckEntity.class);
	}

	@Override
	protected void doHealthCheck(Health.Builder builder) throws Exception {
		String id = UUID.randomUUID().toString();
		try {
			HealthCheckEntity entity = new HealthCheckEntity(id, "Data store Health Check");
			simpleDatastoreRepository.save(entity);
			long count = simpleDatastoreRepository.count();
			Assert.isTrue(count == 1, "Data store health check validated");
			builder.status(DATASTORE_HEALTH);
			if (count > 0) {
				builder.up();
			}
			else {
				builder.down();
			}
		}
		catch (Exception ex) {
			builder.down();
			throw ex;
		}
		finally {
			simpleDatastoreRepository.deleteById(id);
		}
	}
}
