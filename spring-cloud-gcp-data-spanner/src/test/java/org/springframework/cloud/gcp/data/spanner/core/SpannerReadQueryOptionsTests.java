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
import com.google.cloud.spanner.Options.ReadOption;
import org.junit.Test;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
public class SpannerReadQueryOptionsTests {

	@Test(expected = IllegalArgumentException.class)
	public void addNullReadOptionTest() {
		new SpannerReadQueryOptions().addReadOption(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addNullQueryOptionTest() {
		new SpannerReadQueryOptions().addQueryOption(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void timestampErrorTest() {
		SpannerReadQueryOptions spannerReadQueryOptions = new SpannerReadQueryOptions();
		assertFalse(spannerReadQueryOptions.hasReadQueryTimestamp());
		spannerReadQueryOptions.getReadQueryTimestamp();
	}

	@Test
	public void timestampTest() {
		SpannerReadQueryOptions spannerReadQueryOptions = new SpannerReadQueryOptions();
		Timestamp timestamp = Timestamp.now();
		assertFalse(spannerReadQueryOptions.hasReadQueryTimestamp());
		spannerReadQueryOptions.setReadQueryTimestamp(timestamp);
		assertTrue(spannerReadQueryOptions.hasReadQueryTimestamp());
		assertEquals(timestamp, spannerReadQueryOptions.getReadQueryTimestamp());
		spannerReadQueryOptions.unsetReadQueryTimestamp();
		assertFalse(spannerReadQueryOptions.hasReadQueryTimestamp());
	}

	@Test
	public void addReadOptionTest() {
		SpannerReadQueryOptions spannerReadQueryOptions = new SpannerReadQueryOptions();
		ReadOption r1 = mock(ReadOption.class);
		ReadOption r2 = mock(ReadOption.class);
		spannerReadQueryOptions.addReadOption(r1).addReadOption(r2);
		assertThat(Arrays.asList(spannerReadQueryOptions.getReadOptions()),
				containsInAnyOrder(r1, r2));
	}

	@Test
	public void addQueryOptionTest() {
		SpannerReadQueryOptions spannerReadQueryOptions = new SpannerReadQueryOptions();
		QueryOption r1 = mock(QueryOption.class);
		QueryOption r2 = mock(QueryOption.class);
		spannerReadQueryOptions.addQueryOption(r1).addQueryOption(r2);
		assertThat(Arrays.asList(spannerReadQueryOptions.getQueryOptions()),
				containsInAnyOrder(r1, r2));
	}
}
