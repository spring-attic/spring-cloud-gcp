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

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Cloud SQL properties.
 *
 * @author João André Martins
 */
@ConfigurationProperties("spring.cloud.gcp.sql")
public class GcpCloudSqlProperties {

	private String databaseName;

	private String instanceConnectionName;

	private String jdbcUrl;

	private String jdbcDriver;

	private String userName = "root";

	private String password = "";

	private boolean initFailFast;

	private Path credentialsLocation;

	private DatabaseType databaseType = DatabaseType.MYSQL;

	public String getDatabaseName() {
		return this.databaseName;
	}

	public void setDatabaseName(String databaseName) {
		this.databaseName = databaseName;
	}

	public String getInstanceConnectionName() {
		return this.instanceConnectionName;
	}

	public void setInstanceConnectionName(String instanceConnectionName) {
		this.instanceConnectionName = instanceConnectionName;
	}

	public String getJdbcUrl() {
		return this.jdbcUrl;
	}

	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	public String getJdbcDriver() {
		return this.jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
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

	public boolean isInitFailFast() {
		return this.initFailFast;
	}

	public void setInitFailFast(boolean initFailFast) {
		this.initFailFast = initFailFast;
	}

	public Path getCredentialsLocation() {
		return this.credentialsLocation;
	}

	public void setCredentialsLocation(Path credentialsLocation) {
		this.credentialsLocation = credentialsLocation;
	}

	public DatabaseType getDatabaseType() {
		return this.databaseType;
	}

	public void setDatabaseType(DatabaseType databaseType) {
		this.databaseType = databaseType;
	}
}
