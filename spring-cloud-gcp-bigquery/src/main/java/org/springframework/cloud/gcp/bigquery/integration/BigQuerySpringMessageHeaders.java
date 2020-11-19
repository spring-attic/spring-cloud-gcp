/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.bigquery.integration;

/**
 * Spring Integration {@link org.springframework.messaging.Message} headers used with
 * Spring Cloud GCP BigQuery integration.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public final class BigQuerySpringMessageHeaders {

	/**
	 * BigQuery Spring Cloud GCP message header prefix.
	 */
	public static final String PREFIX = "gcp_bigquery_";

	/**
	 * BigQuery table name message header.
	 */
	public static final String TABLE_NAME = PREFIX + "table_name";

	/**
	 * Input data file format message header.
	 */
	public static final String FORMAT_OPTIONS = PREFIX + "format_options";

	/**
	 * The schema of the table to load. Not needed if relying on auto-detecting the schema.
	 */
	public static final String TABLE_SCHEMA = PREFIX + "table_schema";

	private BigQuerySpringMessageHeaders() {
	}
}
