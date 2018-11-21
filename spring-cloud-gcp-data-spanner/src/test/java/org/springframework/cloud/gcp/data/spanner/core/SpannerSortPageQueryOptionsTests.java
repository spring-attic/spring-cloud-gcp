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

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import com.google.cloud.spanner.Options.QueryOption;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
public class SpannerSortPageQueryOptionsTests {

	@Test(expected = IllegalArgumentException.class)
	public void addNullQueryOptionTest() {
		new SpannerQueryOptions().addQueryOption(null);
	}

	@Test
	public void includePropertiesTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		Set<String> includeProperties = Collections.emptySet();
		assertNull(spannerQueryOptions.getIncludeProperties());
		spannerQueryOptions.setIncludeProperties(includeProperties);
		assertNotNull(spannerQueryOptions.getIncludeProperties());
	}

	@Test
	public void addQueryOptionTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		QueryOption r1 = mock(QueryOption.class);
		QueryOption r2 = mock(QueryOption.class);
		spannerQueryOptions.addQueryOption(r1).addQueryOption(r2);
		assertThat(Arrays.asList(spannerQueryOptions.getQueryOptions()),
				containsInAnyOrder(r1, r2));
	}
}
