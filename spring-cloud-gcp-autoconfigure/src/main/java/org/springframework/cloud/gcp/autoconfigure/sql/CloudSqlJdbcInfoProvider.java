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
 * Implementations of this interface typically construct a JDBC URL for Cloud SQL from a GCP
 * project ID and an instance connection name.
 *
 * @author João André Martins
 */
public interface CloudSqlJdbcInfoProvider {
	String getJdbcDriverClass();
	String getJdbcUrl();
}
