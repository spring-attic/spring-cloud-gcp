/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.data.spanner.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.google.cloud.spanner.Options.QueryOption;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the Spanner sort and page query options.
 *
 * @author Chengyuan Zhao
 */
public class SpannerSortPageQueryOptionsTests {

	/**
	 * checks the exception for messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Test
	public void addNullQueryOptionTest() {

		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("Valid query option is required!");

		new SpannerQueryOptions().addQueryOption(null);
	}

	@Test
	public void includePropertiesTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		Set<String> includeProperties = Collections.emptySet();
		assertThat(spannerQueryOptions.getIncludeProperties()).isNull();
		spannerQueryOptions.setIncludeProperties(includeProperties);
		assertThat(spannerQueryOptions.getIncludeProperties()).isNotNull();
	}

	@Test
	public void addQueryOptionTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		QueryOption r1 = mock(QueryOption.class);
		QueryOption r2 = mock(QueryOption.class);
		spannerQueryOptions.addQueryOption(r1).addQueryOption(r2);
		assertThat(Arrays.asList(spannerQueryOptions.getQueryOptions()))
				.containsExactlyInAnyOrder(r1, r2);
	}
}
