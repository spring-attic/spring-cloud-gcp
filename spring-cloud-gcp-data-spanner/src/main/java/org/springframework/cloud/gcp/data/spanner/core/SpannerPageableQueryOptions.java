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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.Set;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.TimestampBound;

import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Encapsulates Cloud Spanner query options with sort and paging.
 *
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
public class SpannerPageableQueryOptions extends SpannerQueryOptions {

	private Integer limit;

	private Long offset;

	private Sort sort = Sort.unsorted();

	public Integer getLimit() {
		return this.limit;
	}

	public SpannerPageableQueryOptions setLimit(Integer limit) {
		this.limit = limit;
		return this;
	}

	public Long getOffset() {
		return this.offset;
	}

	public SpannerPageableQueryOptions setOffset(Long offset) {
		this.offset = offset;
		return this;
	}

	public Sort getSort() {
		return this.sort;
	}

	public SpannerPageableQueryOptions setSort(Sort sort) {
		Assert.notNull(sort, "A valid sort is required.");
		this.sort = sort;
		return this;
	}

	@Override
	public SpannerPageableQueryOptions addQueryOption(Options.QueryOption queryOption) {
		super.addQueryOption(queryOption);
		return this;
	}

	@Override
	public SpannerPageableQueryOptions setIncludeProperties(Set<String> includeProperties) {
		super.setIncludeProperties(includeProperties);
		return this;
	}

	@Override
	public SpannerPageableQueryOptions setTimestampBound(TimestampBound timestampBound) {
		super.setTimestampBound(timestampBound);
		return this;
	}

	@Override
	public SpannerPageableQueryOptions setTimestamp(Timestamp timestamp) {
		super.setTimestamp(timestamp);
		return this;
	}

	@Override
	public SpannerPageableQueryOptions setAllowPartialRead(boolean allowPartialRead) {
		super.setAllowPartialRead(allowPartialRead);
		return this;
	}

}
