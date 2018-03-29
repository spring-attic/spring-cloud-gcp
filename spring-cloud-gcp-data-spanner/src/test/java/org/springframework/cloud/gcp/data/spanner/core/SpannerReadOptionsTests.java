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
public class SpannerReadOptionsTests {

	@Test(expected = IllegalArgumentException.class)
	public void addNullReadOptionTest() {
		new SpannerReadOptions().addReadOption(null);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void timestampErrorTest() {
		SpannerReadOptions spannerReadOptions = new SpannerReadOptions();
		assertFalse(spannerReadOptions.hasTimestamp());
		spannerReadOptions.getTimestamp();
	}

	@Test
	public void timestampTest() {
		SpannerReadOptions spannerReadOptions = new SpannerReadOptions();
		Timestamp timestamp = Timestamp.now();
		assertFalse(spannerReadOptions.hasTimestamp());
		spannerReadOptions.setTimestamp(timestamp);
		assertTrue(spannerReadOptions.hasTimestamp());
		assertEquals(timestamp, spannerReadOptions.getTimestamp());
		spannerReadOptions.unsetTimestamp();
		assertFalse(spannerReadOptions.hasTimestamp());
	}

	@Test
	public void addReadOptionTest() {
		SpannerReadOptions spannerReadOptions = new SpannerReadOptions();
		ReadOption r1 = mock(ReadOption.class);
		ReadOption r2 = mock(ReadOption.class);
		spannerReadOptions.addReadOption(r1).addReadOption(r2);
		assertThat(Arrays.asList(spannerReadOptions.getReadOptions()),
				containsInAnyOrder(r1, r2));
	}

	@Test
	public void indexTest() {
		SpannerReadOptions spannerReadOptions = new SpannerReadOptions();
		String index = "index";
		assertFalse(spannerReadOptions.hasIndex());
		spannerReadOptions.setIndex(index);
		assertTrue(spannerReadOptions.hasIndex());
		assertEquals(index, spannerReadOptions.getIndex());
		spannerReadOptions.unsetIndex();
		assertFalse(spannerReadOptions.hasIndex());
	}
}
