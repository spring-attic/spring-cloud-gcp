/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.core.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Utility for building an immutable, type-safe {@link Map}.
 * <p>Underlying implementation uses a {@link HashMap}, but disallows {@code null} keys,
 * {@code null} values, and duplicate keys. This is consistent with Java 9 {@code Map.of()}
 * static factory method, which will eventually replace the uses of this utility.
 *
 * @param <K> map key type
 * @param <V> map value type
 *
 * @author Elena Felder
 *
 * @since 1.1
 */
public final class MapBuilder<K, V> {

	private final Map<K, V> map = new HashMap<>();

	public MapBuilder<K, V> put(K key, V value) {
		Assert.notNull(key, "Map key cannot be null.");
		Assert.notNull(value, "Map value cannot be null.");
		Assert.isNull(this.map.get(key), "Duplicate keys not allowed.");
		this.map.put(key, value);
		return this;
	}

	public Map<K, V> build() {
		return Collections.unmodifiableMap(this.map);
	}

}
