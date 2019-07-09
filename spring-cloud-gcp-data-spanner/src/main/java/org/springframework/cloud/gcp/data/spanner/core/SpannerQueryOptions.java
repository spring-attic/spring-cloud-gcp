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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.Set;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;
import com.google.cloud.spanner.TimestampBound;

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
public class SpannerQueryOptions extends AbstractSpannerRequestOptions<QueryOption> {

	/**
	 * Constructor to create an instance. Use the extension-style add/set functions to add
	 * options and settings.
	 */
	public SpannerQueryOptions() {
		this.requestOptionType = QueryOption.class;
	}

	public SpannerQueryOptions addQueryOption(QueryOption queryOption) {
		Assert.notNull(queryOption, "Valid query option is required!");
		this.requestOptions.add(queryOption);
		return this;
	}

	@Override
	public SpannerQueryOptions setIncludeProperties(Set<String> includeProperties) {
		super.setIncludeProperties(includeProperties);
		return this;
	}

	@Override
	public SpannerQueryOptions setTimestampBound(TimestampBound timestampBound) {
		super.setTimestampBound(timestampBound);
		return this;
	}

	@Override
	public SpannerQueryOptions setTimestamp(Timestamp timestamp) {
		super.setTimestamp(timestamp);
		return this;
	}

	@Override
	public SpannerQueryOptions setAllowPartialRead(boolean allowPartialRead) {
		super.setAllowPartialRead(allowPartialRead);
		return this;
	}

	/**
	 * @deprecated  as of 1.2. Please use {@code getOptions}.
	 * @return the array of query request options.
	 */
	@Deprecated
	public QueryOption[] getQueryOptions() {
		return this.getOptions();
	}

}
