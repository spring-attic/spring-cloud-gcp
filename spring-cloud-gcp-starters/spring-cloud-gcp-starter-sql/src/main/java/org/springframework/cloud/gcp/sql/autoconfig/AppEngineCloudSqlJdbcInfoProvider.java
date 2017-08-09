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

package org.springframework.cloud.gcp.sql.autoconfig;

import org.springframework.cloud.gcp.sql.CloudSqlJdbcInfoProvider;
import org.springframework.cloud.gcp.sql.GcpCloudSqlProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provides default JDBC driver class name and constructs the JDBC URL for Cloud SQL v2
 * when running inside of Google App Engine Standard environment.
 *
 * @author Ray Tsang
 */
public class AppEngineCloudSqlJdbcInfoProvider implements CloudSqlJdbcInfoProvider {
	private final String projectId;

	private final GcpCloudSqlProperties properties;

	public AppEngineCloudSqlJdbcInfoProvider(String projectId, GcpCloudSqlProperties properties) {
		this.projectId = projectId;
		this.properties = properties;
		Assert.hasText(projectId,
				"A project ID must be provided.");
		Assert.hasText(properties.getDatabaseName(), "Database Name is required");
		if (StringUtils.isEmpty(properties.getInstanceConnectionName())) {
			Assert.hasText(properties.getInstanceName(),
					"Instance Name is required, or specify Instance Connection Name directly");
			Assert.hasText(properties.getRegion(), "Region is required, or specify Instance Connection Name directly");
			properties.setInstanceConnectionName(
					String.format("%s:%s:%s", projectId, properties.getRegion(), properties.getInstanceName()));
		}
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
