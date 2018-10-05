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

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Options.QueryOption;
import org.junit.Test;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
public class SpannerSortPageQueryOptionsTests {

	@Test(expected = IllegalArgumentException.class)
	public void addNullQueryOptionTest() {
		new SpannerQueryOptions().addQueryOption(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void timestampErrorTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		assertFalse(spannerQueryOptions.hasTimestamp());
		spannerQueryOptions.getTimestamp();
	}

	@Test
	public void timestampTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		Timestamp timestamp = Timestamp.now();
		assertFalse(spannerQueryOptions.hasTimestamp());
		spannerQueryOptions.setTimestamp(timestamp);
		assertTrue(spannerQueryOptions.hasTimestamp());
		assertEquals(timestamp, spannerQueryOptions.getTimestamp());
		spannerQueryOptions.unsetTimestamp();
		assertFalse(spannerQueryOptions.hasTimestamp());
	}

	@Test
	public void limitTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		long limit = 3L;
		assertFalse(spannerQueryOptions.hasLimit());
		spannerQueryOptions.setLimit(limit);
		assertTrue(spannerQueryOptions.hasLimit());
		assertEquals(limit, spannerQueryOptions.getLimit());
		spannerQueryOptions.unsetLimit();
		assertFalse(spannerQueryOptions.hasLimit());
	}

	@Test
	public void offsetTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		long offset = 3L;
		assertFalse(spannerQueryOptions.hasOffset());
		spannerQueryOptions.setOffset(offset);
		assertTrue(spannerQueryOptions.hasOffset());
		assertEquals(offset, spannerQueryOptions.getOffset());
		spannerQueryOptions.unsetOffset();
		assertFalse(spannerQueryOptions.hasOffset());
	}

	@Test
	public void sortTest() {
		SpannerPageableQueryOptions spannerQueryOptions = new SpannerPageableQueryOptions();
		Sort sort = Sort.by(Order.asc("test"));
		assertFalse(spannerQueryOptions.getSort().isSorted());
		spannerQueryOptions.setSort(sort);
		assertTrue(spannerQueryOptions.getSort().getOrderFor("test").isAscending());
		spannerQueryOptions.unsetSort();
		assertFalse(spannerQueryOptions.getSort().isSorted());
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
