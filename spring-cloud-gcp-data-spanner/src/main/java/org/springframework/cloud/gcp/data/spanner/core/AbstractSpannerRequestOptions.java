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

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.TimestampBound;

/**
 * Abstract class of common Read and Query request settings.
 *
 * @author Chengyuan Zhao
 */
public class AbstractSpannerRequestOptions<A> implements Serializable {

	protected transient List<A> requestOptions = new ArrayList<>();

	protected Class<A> requestOptionType;

	private TimestampBound timestampBound;

	private Set<String> includeProperties;

	private boolean allowPartialRead;

	public Set<String> getIncludeProperties() {
		return this.includeProperties;
	}

	public AbstractSpannerRequestOptions setIncludeProperties(Set<String> includeProperties) {
		this.includeProperties = includeProperties;
		return this;
	}

	public TimestampBound getTimestampBound() {
		return this.timestampBound;
	}

	/**
	 * Set if this query should be executed with bounded staleness.
	 * @param timestampBound the timestamp bound. Can be exact or bounded staleness.
	 * @return this options object.
	 */
	public AbstractSpannerRequestOptions setTimestampBound(TimestampBound timestampBound) {
		this.timestampBound = timestampBound;
		return this;
	}

	public Timestamp getTimestamp() {
		return this.timestampBound.getMode() == TimestampBound.Mode.READ_TIMESTAMP
				? this.timestampBound.getReadTimestamp()
				: this.timestampBound.getMinReadTimestamp();
	}

	public AbstractSpannerRequestOptions setTimestamp(Timestamp timestamp) {
		this.timestampBound = TimestampBound.ofReadTimestamp(timestamp);
		return this;
	}

	public A[] getOptions() {
		return this.requestOptions.toArray((A[]) Array.newInstance(this.requestOptionType, 0));
	}

	public boolean isAllowPartialRead() {
		return this.allowPartialRead;
	}

	public AbstractSpannerRequestOptions setAllowPartialRead(boolean allowPartialRead) {
		this.allowPartialRead = allowPartialRead;
		return this;
	}

}
