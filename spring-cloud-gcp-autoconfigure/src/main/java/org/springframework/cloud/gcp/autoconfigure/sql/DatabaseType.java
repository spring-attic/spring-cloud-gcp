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

/**
 * @author João André Martins
 */
public enum DatabaseType {
	MYSQL("com.mysql.jdbc.Driver", "jdbc:mysql://google/%s?cloudSqlInstance=%s&"
			+ "socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false"),

	POSTGRESQL("org.postgresql.Driver", "jdbc:postgresql://google/%s?"
			+ "socketFactory=com.google.cloud.sql.postgres.SocketFactory&socketFactoryArg=%s"
			+ "&useSSL=false");

	private final String jdbcDriverName;

	private final String jdbcUrlTemplate;

	DatabaseType(String jdbcDriverName, String jdbcUrlTemplate) {
		this.jdbcDriverName = jdbcDriverName;
		this.jdbcUrlTemplate = jdbcUrlTemplate;
	}

	public String getJdbcDriverName() {
		return this.jdbcDriverName;
	}

	public String getJdbcUrlTemplate() {
		return this.jdbcUrlTemplate;
	}
}
