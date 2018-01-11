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

import javax.servlet.http.HttpServletRequest;

/**
 * Extracts trace IDs from HTTP requests using the X-B3-TraceId header.
 *
 * @author Chengyuan Zhao
 */
public class ZipkinTraceIdExtractor implements TraceIdExtractor {

	public static final String X_B3_TRACE_HEADER = "X-B3-TraceId";

	@Override
	public String extractTraceIdFromRequest(HttpServletRequest req) {
		return req.getHeader(X_B3_TRACE_HEADER);
	}
}
