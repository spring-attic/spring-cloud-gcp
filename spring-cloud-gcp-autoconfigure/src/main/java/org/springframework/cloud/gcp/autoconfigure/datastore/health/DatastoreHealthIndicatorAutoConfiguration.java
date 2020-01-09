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

import java.util.function.Supplier;

import com.google.cloud.datastore.Datastore;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.autoconfigure.datastore.DatastoreProvider;
import org.springframework.cloud.gcp.autoconfigure.datastore.GcpDatastoreAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link HealthContributorAutoConfiguration Auto-configuration} for
 * {@link DatastoreHealthIndicator}.
 *
 * @author Raghavan N S
 * @author Srinivasa Meenavalli
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 *
 * @since 1.2
 */
@Configuration
@ConditionalOnClass({ Datastore.class, HealthIndicator.class })
@ConditionalOnBean(value = Datastore.class, parameterizedContainer = Supplier.class)
@ConditionalOnEnabledHealthIndicator("datastore")
@AutoConfigureBefore(HealthContributorAutoConfiguration.class)
@AutoConfigureAfter(GcpDatastoreAutoConfiguration.class)
public class DatastoreHealthIndicatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public HealthIndicator datastoreHealthIndicator(DatastoreProvider datastore) {
		return new DatastoreHealthIndicator(datastore);
	}

}
