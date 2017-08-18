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

package org.springframework.cloud.gcp.sql;

import org.hibernate.validator.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Google Cloud SQL properties.
 *
 * @author João André Martins
 */
@ConfigurationProperties("spring.cloud.gcp.sql")
@Validated
public class GcpCloudSqlProperties {

	private String instanceName;

	@NotEmpty
	private String databaseName;

	private String region;

	private String instanceConnectionName;

	private String userName = "root";

	private String password = "";

	private boolean initFailFast;

	public String getInstanceName() {
		return this.instanceName;
	}

	public void setInstanceName(String instanceName) {
		this.instanceName = instanceName;
	}

	public String getDatabaseName() {
		return this.databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getRegion() {
		return this.region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getInstanceConnectionName() {
		return this.instanceConnectionName;
	}

	public void setInstanceConnectionName(String instanceConnectionName) {
		this.instanceConnectionName = instanceConnectionName;
	}

	public String getUserName() {
		return this.userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean getInitFailFast() {
		return this.initFailFast;
	}

	public void setInitFailFast(boolean initFailFast) {
		this.initFailFast = initFailFast;
	}
}
