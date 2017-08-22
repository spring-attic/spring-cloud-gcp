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

import java.io.IOException;

import com.google.api.services.sqladmin.SQLAdmin;

import org.springframework.cloud.gcp.sql.GcpCloudSqlProperties;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility class that resolves the JDBC URL from a number of parameters.
 */
class CloudSqlJdbcUrlResolver {

	private final String projectId;

	private final String templateUrl;

	private final GcpCloudSqlProperties properties;

	private final SQLAdmin sqlAdmin;

	CloudSqlJdbcUrlResolver(String templateUrl, String projectId,
			GcpCloudSqlProperties properties, SQLAdmin sqlAdmin) {
		this.templateUrl = templateUrl;
		this.projectId = projectId;
		this.properties = properties;
		this.sqlAdmin = sqlAdmin;
	}

	String resolveJdbcUrl() {
		Assert.hasText(this.projectId, "A project ID must be provided.");
		Assert.hasText(this.properties.getDatabaseName(), "A database name must be provided.");

		String instanceConnectionName = this.properties.getInstanceConnectionName();
		if (StringUtils.isEmpty(instanceConnectionName)) {
			Assert.hasText(this.properties.getInstanceName(),
					"An instance name is required, or specify an instance connection name explicitly");
			String region = this.properties.getRegion();
			if (StringUtils.isEmpty(region)) {
				try {
					region = determineRegion(this.projectId, this.properties.getInstanceName());
				}
				catch (IOException ioe) {
					throw new IllegalArgumentException(
							"Unable to determine Cloud SQL region. Specify the region explicitly, "
									+ "or specify Instance Connection Name explicitly ", ioe);
				}
			}

			instanceConnectionName = String.format(
					"%s:%s:%s", this.projectId, region, this.properties.getInstanceName());
		}

		return String.format(this.templateUrl,
				this.properties.getDatabaseName(),
				instanceConnectionName);
	}

	private String determineRegion(String projectId, String instanceName) throws IOException {
		return this.sqlAdmin.instances().get(projectId, instanceName).execute().getRegion();
	}
}
