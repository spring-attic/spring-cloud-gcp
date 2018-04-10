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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.io.Serializable;
import java.util.StringJoiner;

import com.google.cloud.spanner.Key;

public final class WrappedKey implements Serializable {
	private Key spannerKey;

	WrappedKey(Key spannerKey) {
		this.spannerKey = spannerKey;
	}

	@Override
	public String toString() {
		StringJoiner stringJoiner = new StringJoiner(",");
		this.spannerKey.getParts().forEach(part -> stringJoiner.add(String.valueOf(part)));
		return stringJoiner.toString();
	}

	public Key getSpannerKey() {
		return this.spannerKey;
	}
}
