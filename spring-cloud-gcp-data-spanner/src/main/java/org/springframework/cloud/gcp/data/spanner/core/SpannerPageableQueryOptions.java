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

import java.util.OptionalLong;

import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Encapsulates Cloud Spanner query options with sort and paging.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public class SpannerPageableQueryOptions extends SpannerQueryOptions {

	private OptionalLong limit = OptionalLong.empty();

	private OptionalLong offset = OptionalLong.empty();

	private Sort sort = Sort.unsorted();

	public SpannerPageableQueryOptions unsetLimit() {
		this.limit = OptionalLong.empty();
		return this;
	}

	public SpannerPageableQueryOptions unsetOffset() {
		this.offset = OptionalLong.empty();
		return this;
	}

	public SpannerPageableQueryOptions unsetSort() {
		this.sort = Sort.unsorted();
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

	public SpannerPageableQueryOptions setLimit(long limit) {
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

	public SpannerPageableQueryOptions setOffset(long offset) {
		this.offset = OptionalLong.of(offset);
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
	public SpannerPageableQueryOptions setAllowPartialRead(boolean allowPartialRead) {
		super.setAllowPartialRead(allowPartialRead);
		return this;
	}
}
