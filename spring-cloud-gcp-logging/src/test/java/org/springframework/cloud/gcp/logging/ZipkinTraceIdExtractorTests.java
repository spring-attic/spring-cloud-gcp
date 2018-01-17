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

package org.springframework.cloud.gcp.logging;

import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Chengyuan Zhao
 */

public class ZipkinTraceIdExtractorTests {

	private static final String TEST_TRACE_ID = "105445aa7843bc8bf206b120001000";

	private static final String B3_TRACE_ID_HEADER = "X-B3-TraceId";

	private ZipkinTraceIdExtractor extractor = new ZipkinTraceIdExtractor();

	@Test
	public void testExtractTraceIdFromRequest_missing() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		String traceId = this.extractor.extractTraceIdFromRequest(request);

		assertThat(traceId, nullValue());
	}

	@Test
	public void testExtractTraceIdFromRequest() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(B3_TRACE_ID_HEADER, TEST_TRACE_ID);

		String traceId = this.extractor.extractTraceIdFromRequest(request);

		assertThat(traceId, is(TEST_TRACE_ID));
	}
}
