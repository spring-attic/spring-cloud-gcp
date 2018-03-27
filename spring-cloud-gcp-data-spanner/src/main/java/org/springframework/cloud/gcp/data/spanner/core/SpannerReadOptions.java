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
import com.google.cloud.spanner.Options.ReadOption;

import org.springframework.util.Assert;

/**
 * Encapsulates Spanner read options.
 * @author Chengyuan Zhao
 */
public class SpannerReadOptions {

	private List<ReadOption> readOptions = new ArrayList<>();

	private Optional<Timestamp> timestamp = Optional.empty();

	private Optional<String> index = Optional.empty();

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

	public SpannerReadOptions unsetTimestamp() {
		this.timestamp = Optional.empty();
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

	public SpannerReadOptions setTimestamp(Timestamp timestamp) {
		Assert.notNull(timestamp, "A valid timestamp is required!");
		this.timestamp = Optional.of(timestamp);
		return this;
	}

	public SpannerReadOptions unsetIndex() {
		this.index = Optional.empty();
		return this;
	}

	public boolean hasIndex() {
		return this.index.isPresent();
	}

	public String getIndex() {
		if (!hasIndex()) {
			throw new UnsupportedOperationException(
					"Cannot get index because it hasn't been set.");
		}
		return this.index.get();
	}

	public SpannerReadOptions setIndex(String index) {
		Assert.notNull(index, "A valid index is required!");
		this.index = Optional.of(index);
		return this;
	}

	public ReadOption[] getReadOptions() {
		return this.readOptions.toArray(new ReadOption[this.readOptions.size()]);
	}
}
