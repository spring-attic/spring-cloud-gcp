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

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.Options.ReadOption;

import org.springframework.util.Assert;

/**
 * Encapsulates Spanner read and query options.
 * @author Chengyuan Zhao
 */
public class SpannerReadQueryOptions {

	private List<ReadOption> readOptions = new ArrayList<>();

	private List<QueryOption> queryOptions = new ArrayList<>();

	private Optional<Timestamp> readQueryTimestamp = Optional.empty();

	/**
	 * Constructor to create an instance. Use the extension-style add/set functions to add
	 * options and settings.
	 */
	public SpannerReadQueryOptions() {
	}

	public SpannerReadQueryOptions addReadOption(ReadOption readOption) {
		Assert.notNull(readOption, "Valid read option is required!");
		this.readOptions.add(readOption);
		return this;
	}

	public SpannerReadQueryOptions addQueryOption(QueryOption queryOption) {
		Assert.notNull(queryOption, "Valid query option is required!");
		this.queryOptions.add(queryOption);
		return this;
	}

	public SpannerReadQueryOptions unsetReadQueryTimestamp() {
		this.readQueryTimestamp = Optional.empty();
		return this;
	}

	public boolean hasReadQueryTimestamp() {
		return this.readQueryTimestamp.isPresent();
	}

	public Timestamp getReadQueryTimestamp() {
		if (!hasReadQueryTimestamp()) {
			throw new UnsupportedOperationException(
					"Cannot get timestamp because it hasn't been set.");
		}
		return this.readQueryTimestamp.get();
	}

	public SpannerReadQueryOptions setReadQueryTimestamp(Timestamp timestamp) {
		Assert.notNull(timestamp, "A valid timestamp is required!");
		this.readQueryTimestamp = Optional.of(timestamp);
		return this;
	}

	public ReadOption[] getReadOptions() {
		return this.readOptions.toArray(new ReadOption[this.readOptions.size()]);
	}

	public QueryOption[] getQueryOptions() {
		return this.queryOptions.toArray(new QueryOption[this.queryOptions.size()]);
	}
}
