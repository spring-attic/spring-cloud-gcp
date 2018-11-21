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
import java.util.Set;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;

import org.springframework.util.Assert;

/**
 * Encapsulates Cloud Spanner query options. These are options that are independent of the
 * SQL being run.
 *
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
public class SpannerQueryOptions {

	private List<QueryOption> queryOptions = new ArrayList<>();

	private Timestamp timestamp;

	private Set<String> includeProperties;

	private boolean allowPartialRead;

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

	public Set<String> getIncludeProperties() {
		return this.includeProperties;
	}

	public SpannerQueryOptions setIncludeProperties(Set<String> includeProperties) {
		this.includeProperties = includeProperties;
		return this;
	}

	public Timestamp getTimestamp() {
		return this.timestamp;
	}

	public SpannerQueryOptions setTimestamp(Timestamp timestamp) {
		Assert.notNull(timestamp, "A valid timestamp is required!");
		this.timestamp = timestamp;
		return this;
	}

	public QueryOption[] getQueryOptions() {
		return this.queryOptions.toArray(new QueryOption[this.queryOptions.size()]);
	}

	public boolean isAllowPartialRead() {
		return this.allowPartialRead;
	}

	public SpannerQueryOptions setAllowPartialRead(
			boolean allowPartialRead) {
		this.allowPartialRead = allowPartialRead;
		return this;
	}
}
