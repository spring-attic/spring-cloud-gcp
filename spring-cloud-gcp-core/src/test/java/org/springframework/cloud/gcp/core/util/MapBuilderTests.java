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

import java.util.Map;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link MapBuilder}.
 *
 * @author Elena Felder
 */
public class MapBuilderTests {

	@Test
	public void mapWithDistinctKeysBuildsAsExpected() {
		Map<String, String> result = new MapBuilder<String, String>()
				.put("a", "alpha")
				.put("b", "beta")
				.put("g", "gamma")
				.build();
		assertThat(result).containsOnlyKeys("a", "b", "g");
		assertThat(result).containsEntry("a", "alpha");
		assertThat(result).containsEntry("b", "beta");
		assertThat(result).containsEntry("g", "gamma");
	}

	@Test
	public void emptyMapIsEmpty() {
		Map<String, String> result = new MapBuilder<String, String>().build();
		assertThat(result).isEmpty();
	}

	@Test
	public void mapWithNullKeyThrowsException() {
		assertThatThrownBy(() -> new MapBuilder<String, String>().put(null, "nope"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Map key cannot be null.");
	}

	@Test
	public void mapWithNullValueThrowsException() {
		assertThatThrownBy(() -> new MapBuilder<String, String>().put("nope", null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Map value cannot be null.");
	}

	@Test
	public void mapWithDuplicateKeysThrowsException() {
		assertThatThrownBy(() -> new MapBuilder<String, String>().put("b", "beta").put("b", "vita"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Duplicate keys not allowed.");
	}

}
