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

package org.springframework.cloud.gcp.bigquery.core;

import java.io.InputStream;

import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.Job;

import org.springframework.util.concurrent.ListenableFuture;

/**
 * Defines operations for use with BigQuery.
 *
 * @author Daniel Zou
 * @since 1.2
 */
public interface BigQueryOperations {

	/**
	 * Writes data to a specified BigQuery table.
	 *
	 * @param tableName name of the table to write to
	 * @param inputStream input stream of the table data to write
	 * @param dataFormatOptions the format of the data to write
	 * @return {@link ListenableFuture} containing the BigQuery Job indicating completion of
	 * operation
	 *
	 * @throws BigQueryException if errors occur when loading data to the BigQuery table
	 */
	ListenableFuture<Job> writeDataToTable(
			String tableName, InputStream inputStream, FormatOptions dataFormatOptions);
}
