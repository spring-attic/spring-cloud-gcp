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

/**
 * @author João André Martins
 */
public enum DatabaseType {
	MYSQL("mysql", "com.mysql.jdbc.Driver", "jdbc:mysql://google/%s?cloudSqlInstance=%s&"
			+ "socketFactory=com.google.cloud.sql.mysql.SocketFactory"),
	POSTGRESQL("postgresql", "org.postgresql.Driver", "jdbc:postgresql://google/%s?"
			+ "socketFactory=com.google.cloud.sql.postgres.SocketFactory&socketFactoryArg=%s");

	private String name;

	private String jdbcDriverName;

	private String jdbcUrlTemplate;

	DatabaseType(String name, String jdbcDriverName, String jdbcUrlTemplate) {
		this.name = name;
		this.jdbcDriverName = jdbcDriverName;
		this.jdbcUrlTemplate = jdbcUrlTemplate;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getJdbcDriverName() {
		return this.jdbcDriverName;
	}

	public void setJdbcDriverName(String jdbcDriverName) {
		this.jdbcDriverName = jdbcDriverName;
	}

	public String getJdbcUrlTemplate() {
		return this.jdbcUrlTemplate;
	}

	public void setJdbcUrlTemplate(String jdbcUrlTemplate) {
		this.jdbcUrlTemplate = jdbcUrlTemplate;
	}
}
