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

import com.google.cloud.spanner.KeySet;
import com.google.cloud.spanner.Statement;

import org.springframework.cloud.gcp.data.spanner.core.SpannerQueryOptions;
import org.springframework.cloud.gcp.data.spanner.core.SpannerReadOptions;
import org.springframework.context.ApplicationEvent;

/**
 * This event is published immediately following a read operation on Cloud Spanner.
 *
 * @author Chengyuan Zhao
 */
public class AfterLoadEvent extends ApplicationEvent {

	private final Statement query;

	private final SpannerReadOptions spannerReadOptions;

	private final SpannerQueryOptions spannerQueryOptions;

	private final KeySet keySet;

	/**
	 * Constructor.
	 * @param source The entities that were read from Cloud Spanner.This is never
	 *     {@code null}.
	 * @param query the read query that was run. This may be {@code null} if the load
	 *     operation was not based on a query.
	 * @param spannerReadOptions the options that were used to conduct the read. This may be
	 *     {@code null} if the read operation wasn't a key-based read.
	 * @param spannerQueryOptions the options that were used to conduct the query. This may be
	 *     {@code null} if the operation was a key-based read.
	 * @param keySet the keys that were read. This may be {@code null} if the operation was a
	 *     query-based read.
	 */
	public AfterLoadEvent(Iterable source, Statement query, SpannerReadOptions spannerReadOptions,
			SpannerQueryOptions spannerQueryOptions, KeySet keySet) {
		super(source);
		this.query = query;
		this.spannerReadOptions = spannerReadOptions;
		this.spannerQueryOptions = spannerQueryOptions;
		this.keySet = keySet;
	}

	/**
	 * Returns the entities that were loaded.
	 * @return the entities that were read from Cloud Spanner.
	 */
	public Iterable getRetrievedEntities() {
		return (Iterable) getSource();
	}

	/**
	 * Get the read query that was run.
	 * @return This may be {@code null} if the load operation was not based on a query.
	 */
	public Statement getQuery() {
		return this.query;
	}

	/**
	 * Get the options that were used to conduct the read.
	 * @return This may be {@code null} if the read operation wasn't a key-based read.
	 */
	public SpannerReadOptions getSpannerReadOptions() {
		return this.spannerReadOptions;
	}

	/**
	 * Get the options that were used to conduct the query.
	 * @return This may be {@code null} if the operation was a key-based read.
	 */
	public SpannerQueryOptions getSpannerQueryOptions() {
		return this.spannerQueryOptions;
	}

	/**
	 * Get the keys that were read.
	 * @return This may be {@code null} if the operation was a query-based read.
	 */
	public KeySet getKeySet() {
		return this.keySet;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		AfterLoadEvent that = (AfterLoadEvent) o;
		return Objects.equals(getRetrievedEntities(), that.getRetrievedEntities())
				&& Objects.equals(getQuery(), that.getQuery())
				&& Objects.equals(getSpannerQueryOptions(), that.getSpannerQueryOptions())
				&& Objects.equals(getSpannerReadOptions(), that.getSpannerReadOptions())
				&& Objects.equals(getKeySet(), that.getKeySet());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getRetrievedEntities(), getQuery(), getSpannerReadOptions(), getSpannerQueryOptions(),
				getKeySet());
	}
}
