/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.autoconfigure.core.AppEngineCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link CloudSqlJdbcInfoProvider} for apps running on Google App Engine.
 *
 * @author João André Martins
 */
@Configuration
@ConditionalOnClass({com.google.cloud.sql.mysql.SocketFactory.class})
@Conditional(AppEngineCondition.class)
class AppEngineJdbcInfoProviderAutoConfiguration {

	private static final Log LOGGER =
			LogFactory.getLog(AppEngineJdbcInfoProviderAutoConfiguration.class);

	@Bean
	@ConditionalOnMissingBean(CloudSqlJdbcInfoProvider.class)
	public CloudSqlJdbcInfoProvider appengineCloudSqlJdbcInfoProvider(
			GcpCloudSqlProperties gcpCloudSqlProperties) {
		CloudSqlJdbcInfoProvider appEngineProvider =
				new AppEngineCloudSqlJdbcInfoProvider(gcpCloudSqlProperties);

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("App Engine JdbcUrl provider. Connecting to "
					+ appEngineProvider.getJdbcUrl() + " with driver "
					+ appEngineProvider.getJdbcDriverClass());
		}

		return appEngineProvider;
	}
}
