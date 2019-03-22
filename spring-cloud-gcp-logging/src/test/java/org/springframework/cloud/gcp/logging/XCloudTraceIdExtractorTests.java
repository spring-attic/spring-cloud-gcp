/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.logging;

import org.junit.Test;

import org.springframework.cloud.gcp.logging.extractors.XCloudTraceIdExtractor;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the x-cloud trace ID extractor.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */

public class XCloudTraceIdExtractorTests {

	private static final String TEST_TRACE_ID = "105445aa7843bc8bf206b120001000";

	private static final String TEST_TRACE_ID_WITH_SPAN = "105445aa7843bc8bf206b120001000/0;o=1";

	private static final String TRACE_ID_HEADER = "X-CLOUD-TRACE-CONTEXT";

	private XCloudTraceIdExtractor extractor = new XCloudTraceIdExtractor();

	@Test
	public void testExtractTraceIdFromRequest_valid() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TRACE_ID_HEADER, TEST_TRACE_ID_WITH_SPAN);

		String traceId = this.extractor.extractTraceIdFromRequest(request);

		assertThat(traceId).isEqualTo(TEST_TRACE_ID);
	}

	@Test
	public void testExtractTraceIdFromRequest_missing() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		String traceId = this.extractor.extractTraceIdFromRequest(request);

		assertThat(traceId).isNull();
	}

	@Test
	public void testExtractTraceIdFromRequest_missingSpan() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TRACE_ID_HEADER, TEST_TRACE_ID);

		String traceId = this.extractor.extractTraceIdFromRequest(request);

		assertThat(traceId).isEqualTo(TEST_TRACE_ID);
	}

}
