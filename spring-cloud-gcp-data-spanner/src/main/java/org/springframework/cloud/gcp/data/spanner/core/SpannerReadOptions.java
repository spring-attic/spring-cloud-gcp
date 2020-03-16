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
import com.google.cloud.spanner.Options;
import com.google.cloud.spanner.Options.ReadOption;
import com.google.cloud.spanner.TimestampBound;

import org.springframework.util.Assert;

/**
 * Encapsulates Cloud Spanner read options.
 *
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
public class SpannerReadOptions extends AbstractSpannerRequestOptions<ReadOption> {

	private String index;

	/**
	 * Constructor to create an instance. Use the extension-style add/set functions to add
	 * options and settings.
	 */
	public SpannerReadOptions() {
		this.requestOptionType = ReadOption.class;
	}

	public SpannerReadOptions addReadOption(ReadOption readOption) {
		Assert.notNull(readOption, "Valid read option is required!");
		this.requestOptions.add(readOption);
		return this;
	}

	public String getIndex() {
		return this.index;
	}

	public SpannerReadOptions setIndex(String index) {
		this.index = index;
		return this;
	}

	@Override
	public SpannerReadOptions setIncludeProperties(Set<String> includeProperties) {
		super.setIncludeProperties(includeProperties);
		return this;
	}

	@Override
	public SpannerReadOptions setTimestampBound(TimestampBound timestampBound) {
		super.setTimestampBound(timestampBound);
		return this;
	}

	@Override
	public SpannerReadOptions setTimestamp(Timestamp timestamp) {
		super.setTimestamp(timestamp);
		return this;
	}

	@Override
	public SpannerReadOptions setAllowPartialRead(boolean allowPartialRead) {
		super.setAllowPartialRead(allowPartialRead);
		return this;
	}

	/**
	 * @deprecated  as of 1.2. Please use {@code getOptions}.
	 * @return the array of read request options.
	 */
	@Deprecated
	public ReadOption[] getReadOptions() {
		return this.getOptions();
	}

	/**
	 * In many cases a {@link SpannerReadOptions} class instance could be compatible with {@link SpannerQueryOptions}.
	 * The method executes such conversion or throws an exception if it's impossible.
	 * @return query-parameters
	 * @throws IllegalArgumentException when {@link SpannerQueryOptions} can't be converted to {@link SpannerQueryOptions}.
	 */
	public SpannerQueryOptions toQueryOptions() {
		SpannerQueryOptions query = new SpannerQueryOptions();
		query.setAllowPartialRead(this.isAllowPartialRead());
		query.setIncludeProperties(this.getIncludeProperties());
		query.setTimestampBound(this.getTimestampBound());

		for (ReadOption ro : this.getOptions()) {
			if (ro instanceof Options.ReadAndQueryOption) {
				query.addQueryOption((Options.ReadAndQueryOption) ro);
			}
			else {
				throw new IllegalArgumentException(String.format("Can't convert %s to SpannerQueryOptions ", this));
			}
		}
		return query;
	}

}
