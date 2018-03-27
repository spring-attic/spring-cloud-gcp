/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;

import org.springframework.util.Assert;

/**
 * Encapsulates Spanner query options.
 * @author Chengyuan Zhao
 */
public class SpannerQueryOptions {

	private List<QueryOption> queryOptions = new ArrayList<>();

	private Optional<Timestamp> timestamp = Optional.empty();

	private OptionalLong limit = OptionalLong.empty();

	private OptionalLong offset = OptionalLong.empty();

	/**
	 * Constructor to create an instance. Use the extension-style add/set functions to add
	 * options and settings.
	 */
	public SpannerQueryOptions() {
	}

	public SpannerQueryOptions addQueryOption(QueryOption queryOption) {
		Assert.notNull(queryOption, "Valid query option is required!");
		this.queryOptions.add(queryOption);
		return this;
	}

	public SpannerQueryOptions unsetTimestamp() {
		this.timestamp = Optional.empty();
		return this;
	}

	public SpannerQueryOptions unsetLimit() {
		this.limit = OptionalLong.empty();
		return this;
	}

	public SpannerQueryOptions unsetOffset() {
		this.offset = OptionalLong.empty();
		return this;
	}

	public boolean hasTimestamp() {
		return this.timestamp.isPresent();
	}

	public Timestamp getTimestamp() {
		if (!hasTimestamp()) {
			throw new UnsupportedOperationException(
					"Cannot get timestamp because it hasn't been set.");
		}
		return this.timestamp.get();
	}

	public SpannerQueryOptions setTimestamp(Timestamp timestamp) {
		Assert.notNull(timestamp, "A valid timestamp is required!");
		this.timestamp = Optional.of(timestamp);
		return this;
	}

	public boolean hasLimit() {
		return this.limit.isPresent();
	}

	public long getLimit() {
		if (!hasLimit()) {
			throw new UnsupportedOperationException(
					"Cannot get limit because it hasn't been set.");
		}
		return this.limit.getAsLong();
	}

	public SpannerQueryOptions setLimit(long limit) {
		this.limit = OptionalLong.of(limit);
		return this;
	}

	public boolean hasOffset() {
		return this.offset.isPresent();
	}

	public long getOffset() {
		if (!hasOffset()) {
			throw new UnsupportedOperationException(
					"Cannot get offset because it hasn't been set.");
		}
		return this.offset.getAsLong();
	}

	public SpannerQueryOptions setOffset(long offset) {
		this.offset = OptionalLong.of(offset);
		return this;
	}

	public QueryOption[] getQueryOptions() {
		return this.queryOptions.toArray(new QueryOption[this.queryOptions.size()]);
	}
}
