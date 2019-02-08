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

import org.springframework.cloud.gcp.data.spanner.core.SpannerReadOptions;

/**
 * This event read operation on Cloud Spanner.
 *
 * @author Chengyuan Zhao
 */
public class AfterReadEvent extends LoadEvent {

	private final SpannerReadOptions spannerReadOptions;

	private final KeySet keySet;

	/**
	 * Constructor.
	 * @param source The entities that were read from Cloud Spanner.This is never
	 *     {@code null}.
	 * @param keySet the keys that were read.
	 * @param spannerReadOptions the options that were used to conduct the read. This may be
	 *     {@code null} if the read operation wasn't a key-based read.
	 */
	public AfterReadEvent(Iterable source,
			KeySet keySet, SpannerReadOptions spannerReadOptions) {
		super(source);
		this.spannerReadOptions = spannerReadOptions;
		this.keySet = keySet;
	}

	/**
	 * Get the options that were used to conduct the read.
	 * @return This may be {@code null} if the read operation wasn't a key-based read.
	 */
	public SpannerReadOptions getSpannerReadOptions() {
		return this.spannerReadOptions;
	}

	/**
	 * Get the keys that were read.
	 * @return the key set.
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
		AfterReadEvent that = (AfterReadEvent) o;
		return Objects.equals(getRetrievedEntities(), that.getRetrievedEntities())
				&& Objects.equals(getKeySet(), that.getKeySet())
				&& Objects.equals(getSpannerReadOptions(), that.getSpannerReadOptions());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getRetrievedEntities(), getSpannerReadOptions(), getKeySet());
	}
}
