/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.test.domain;

import java.time.Instant;
import java.time.LocalDateTime;

import org.springframework.cloud.gcp.data.spanner.core.convert.CommitTimestamp;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;

/**
 * A test object for testing {@link CommitTimestamp} features.
 *
 * @author Roman Solodovnichenko
 */
@Table(name = "#{'commit_timestamps_'.concat(tableNameSuffix)}")
public class CommitTimestamps {
	/**
	 * A primary key.
	 */
	@PrimaryKey
	public String id;

	/**
	 * A cloud Timestamp field.
	 */
	public com.google.cloud.Timestamp cloudTimestamp;

	/**
	 * An sql Timestamp field.
	 */
	public java.sql.Timestamp sqlTimestamp;

	/**
	 * A LocalDateTime field.
	 */
	public LocalDateTime localDateTime;

	/**
	 * An Instant field.
	 */
	public Instant instant;

	/**
	 * A java Date field.
	 */
	public java.util.Date utilDate;

}
