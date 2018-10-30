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
import com.google.cloud.spanner.Options.ReadOption;

import org.springframework.util.Assert;

/**
 * Encapsulates Cloud Spanner read options.
 *
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
public class SpannerReadOptions {

	private List<ReadOption> readOptions = new ArrayList<>();

	private Timestamp timestamp;

	private String index;

	private Set<String> includeProperties;

	private boolean allowPartialRead;

	/**
	 * Constructor to create an instance. Use the extension-style add/set functions to add
	 * options and settings.
	 */
	public SpannerReadOptions() {
	}

	public SpannerReadOptions addReadOption(ReadOption readOption) {
		Assert.notNull(readOption, "Valid read option is required!");
		this.readOptions.add(readOption);
		return this;
	}

	public Set<String> getIncludeProperties() {
		return this.includeProperties;
	}

	public SpannerReadOptions setIncludeProperties(Set<String> includeProperties) {
		this.includeProperties = includeProperties;
		return this;
	}

	public Timestamp getTimestamp() {
		return this.timestamp;
	}

	public SpannerReadOptions setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
		return this;
	}

	public String getIndex() {
		return this.index;
	}

	public SpannerReadOptions setIndex(String index) {
		this.index = index;
		return this;
	}

	public ReadOption[] getReadOptions() {
		return this.readOptions.toArray(new ReadOption[this.readOptions.size()]);
	}

	public boolean isAllowPartialRead() {
		return this.allowPartialRead;
	}

	public SpannerReadOptions setAllowPartialRead(boolean allowPartialRead) {
		this.allowPartialRead = allowPartialRead;
		return this;
	}
}
