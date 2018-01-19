/*
 *  Copyright 2017 original author or authors.
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

import org.springframework.util.Assert;

/**
 * Provides default JDBC driver class name and constructs the JDBC URL for Cloud SQL v2
 * when running inside of Google App Engine Standard environment.
 *
 * @author Ray Tsang
 */
public class AppEngineCloudSqlJdbcInfoProvider implements CloudSqlJdbcInfoProvider {

	private final GcpCloudSqlProperties properties;

	public AppEngineCloudSqlJdbcInfoProvider(GcpCloudSqlProperties properties) {
		this.properties = properties;
		Assert.hasText(properties.getDatabaseName(), "A database name is required.");
		Assert.hasText(properties.getInstanceConnectionName(),
				"An instance connection name must be provided. Refer to "
						+ GcpCloudSqlAutoConfiguration.INSTANCE_CONNECTION_NAME_HELP_URL
						+ " for more information.");
	}

	@Override
	public String getJdbcDriverClass() {
		return "com.mysql.jdbc.GoogleDriver";
	}

	@Override
	public String getJdbcUrl() {
		return String.format("jdbc:google:mysql://%s/%s",
				this.properties.getInstanceConnectionName(), this.properties.getDatabaseName());
	}
}
