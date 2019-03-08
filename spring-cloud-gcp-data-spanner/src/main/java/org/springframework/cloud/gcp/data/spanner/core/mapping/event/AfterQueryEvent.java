/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.spanner.core.mapping.event;

import java.util.Objects;

import com.google.cloud.spanner.Statement;

import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;

/**
 * This event is published immediately following a read-by-query operation on Cloud
 * Spanner.
 *
 * @author Chengyuan Zhao
 */
public class AfterQueryEvent extends LoadEvent {

	private final Statement query;

	private final SpannerQueryOptions spannerQueryOptions;

	/**
	 * Constructor.
	 * @param source The entities that were read from Cloud Spanner.This is never
	 *     {@code null}.
	 * @param query the read query that was run.
	 * @param spannerQueryOptions the options that were used to conduct the query. This may be
	 *     {@code null} if the operation was a key-based read.
	 */
	public AfterQueryEvent(Iterable source, Statement query,
			SpannerQueryOptions spannerQueryOptions) {
		super(source);
		this.query = query;
		this.spannerQueryOptions = spannerQueryOptions;
	}

	/**
	 * Get the read query that was run.
	 * @return the query statement.
	 */
	public Statement getQuery() {
		return this.query;
	}

	/**
	 * Get the options that were used to conduct the query.
	 * @return This may be {@code null} if the operation was a key-based read.
	 */
	public SpannerQueryOptions getSpannerQueryOptions() {
		return this.spannerQueryOptions;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AfterQueryEvent that = (AfterQueryEvent) o;
		return Objects.equals(getRetrievedEntities(), that.getRetrievedEntities())
				&& Objects.equals(getQuery(), that.getQuery())
				&& Objects.equals(getSpannerQueryOptions(), that.getSpannerQueryOptions());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getRetrievedEntities(), getQuery(), getSpannerQueryOptions());
	}
}
