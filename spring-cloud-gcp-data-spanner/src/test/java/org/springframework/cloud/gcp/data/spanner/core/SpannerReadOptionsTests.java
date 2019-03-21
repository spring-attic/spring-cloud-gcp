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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.google.cloud.spanner.Options.ReadOption;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the Spanner read options.
 *
 * @author Chengyuan Zhao
 */
public class SpannerReadOptionsTests {

	/**
	 * checks the exception for messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Test
	public void addNullReadOptionTest() {

		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("Valid read option is required!");

		new SpannerReadOptions().addReadOption(null);
	}

	@Test
	public void addReadOptionTest() {
		SpannerReadOptions spannerReadOptions = new SpannerReadOptions();
		ReadOption r1 = mock(ReadOption.class);
		ReadOption r2 = mock(ReadOption.class);
		spannerReadOptions.addReadOption(r1).addReadOption(r2);
		assertThat(Arrays.asList(spannerReadOptions.getReadOptions()))
				.containsExactlyInAnyOrder(r1, r2);
	}

	@Test
	public void includePropertiesTest() {
		SpannerReadOptions spannerReadOptions = new SpannerReadOptions();
		Set<String> includeProperties = Collections.emptySet();
		assertThat(spannerReadOptions.getIncludeProperties()).isNull();
		spannerReadOptions.setIncludeProperties(includeProperties);
		assertThat(spannerReadOptions.getIncludeProperties()).isNotNull();
	}

}
