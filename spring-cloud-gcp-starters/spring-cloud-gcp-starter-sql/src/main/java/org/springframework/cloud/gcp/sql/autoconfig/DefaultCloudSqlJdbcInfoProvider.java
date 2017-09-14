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
import org.springframework.util.StringUtils;

/**
 * Provides default JDBC driver class name and constructs the JDBC URL for Cloud SQL v2
 * when running on local laptop, or in a VM-based environment (e.g., Google Compute
 * Engine, Google Container Engine).
 *
 * @author Ray Tsang
 * @author João André Martins
 */
public class DefaultCloudSqlJdbcInfoProvider implements CloudSqlJdbcInfoProvider {

	private final CloudSqlJdbcUrlResolver jdbcUrlResolver;

	private final GcpCloudSqlProperties properties;

	public DefaultCloudSqlJdbcInfoProvider(String projectId,
			GcpCloudSqlProperties properties) {
		this.jdbcUrlResolver = new CloudSqlJdbcUrlResolver(
				properties.getDatabaseType().getJdbcUrlTemplate(),
				projectId, properties);
		this.properties = properties;
	}

	@Override
	public String getJdbcDriverClass() {
		return StringUtils.isEmpty(this.properties.getJdbcDriver())
				? this.properties.getDatabaseType().getJdbcDriverName()
				: this.properties.getJdbcDriver();
	}

	@Override
	public String getJdbcUrl() {
		return StringUtils.isEmpty(this.properties.getJdbcUrl())
				? this.jdbcUrlResolver.resolveJdbcUrl() : this.properties.getJdbcUrl();
	}
}
