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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.HashMap;
import java.util.Map;

import com.google.cloud.spanner.Value;

/**
 * The factory method {@link #of(Class)} should be used to create a specific value of a pointed class
 * that will be converted then to the {@link Value#COMMIT_TIMESTAMP} by {@link SpannerConverters}.
 * It should be used when the database column has a Timestamp type with option "allow_commit_timestamp"
 * and the business logic needs to manage when the PENDING_COMMIT_TIMESTAMP should be stored into this field.
 */
public final class CommitTimestamp {
	private static final Map<Object, Object> VALUES = new HashMap<>();

	static {
		VALUES.put(com.google.cloud.Timestamp.class, Value.COMMIT_TIMESTAMP);
		VALUES.put(java.sql.Timestamp.class, Value.COMMIT_TIMESTAMP.toSqlTimestamp());
		VALUES.put(java.time.LocalDateTime.class, Value.COMMIT_TIMESTAMP.toSqlTimestamp().toLocalDateTime());
		VALUES.put(java.time.Instant.class, java.time.Instant.ofEpochSecond(Value.COMMIT_TIMESTAMP.getSeconds(), Value.COMMIT_TIMESTAMP.getNanos()));
		VALUES.put(java.util.Date.class, Value.COMMIT_TIMESTAMP.toDate());
		VALUES.put(java.sql.Date.class, new java.sql.Date(Value.COMMIT_TIMESTAMP.toDate().getTime()));
	}

	private CommitTimestamp() {
	}

	/**
	 * Creates a value for {@link Value#COMMIT_TIMESTAMP}.
	 * @param timestampClass a class to be presented as Timestamp in the database column.
	 * @param <T> type of the class. Supported types are:
	 * 	{@link com.google.cloud.Timestamp}, {@link java.sql.Timestamp}, {@link java.time.LocalDateTime},
	 * 	{@link java.time.Instant}, {@link java.util.Date}, {@link java.sql.Date}.
	 * 	An {@link IllegalArgumentException} will be thrown when the type is not in this list.
	 * @return a value that will be converted to {@link Value#COMMIT_TIMESTAMP} by {@link SpannerConverters}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T of(Class<T> timestampClass) {
		return (T) VALUES.computeIfAbsent(timestampClass, key -> {
			throw new IllegalArgumentException(
					String.format("CommitTimestamp is not supported for the %s", key));
		});
	}

}
