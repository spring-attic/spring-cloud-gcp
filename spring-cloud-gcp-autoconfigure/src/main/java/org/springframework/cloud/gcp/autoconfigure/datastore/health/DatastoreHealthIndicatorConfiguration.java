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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthIndicatorConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.repository.support.DatastoreRepositoryFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for {@link DatastoreHealthIndicator}.
 *
 * @author Raghavan N S
 * @author Srinivasa Meenavalli
 */
@Configuration
@ConditionalOnClass({DatastoreRepositoryFactory.class, DatastoreTemplate.class, Datastore.class})
@ConditionalOnBean({DatastoreRepositoryFactory.class, DatastoreTemplate.class, Datastore.class})
class DatastoreHealthIndicatorConfiguration extends CompositeHealthIndicatorConfiguration<DatastoreHealthIndicator, Datastore>	{
	@Autowired
	Datastore datastore;
	DatastoreHealthIndicatorConfiguration(final Datastore datastore) {
		this.datastore = datastore;
	}
	@Bean
	@ConditionalOnMissingBean(name = "datastoreHealthIndicator")
	public HealthIndicator datastoreHealthIndicator() {
		return createHealthIndicator(this.datastore);
	}
}
