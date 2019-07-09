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
public class SpannerReadOptions extends AbstractSpannerRequestOptions<ReadOption, SpannerReadOptions>
		implements Serializable {

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

}
