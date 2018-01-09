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

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.cloud.logging.TraceLoggingEnhancer;
import com.google.common.collect.ImmutableList;

import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/**
 * {@link org.springframework.web.servlet.HandlerInterceptor} that extracts the request
 * trace ID from the "x-cloud-trace-context" HTTP header and stores it in a thread-local
 * using {@link TraceLoggingEnhancer#setCurrentTraceId}.
 *
 * The {@link TraceLoggingEnhancer} can then be used in a logging appender to add the
 * trace ID metadata to log messages.
 *
 * @author Mike Eltsufin
 */
public class TraceIdLoggingWebMvcInterceptor extends HandlerInterceptorAdapter {

	private static final String X_CLOUD_TRACE = "x-cloud-trace-context";

	private static final String X_B3_TRACE = "X-B3-TraceId";

	private static final List<String> TRACE_HEADERS = ImmutableList.of(X_CLOUD_TRACE,
			X_B3_TRACE);

	@Override
	public boolean preHandle(HttpServletRequest req,
			HttpServletResponse resp, Object handler) throws Exception {
		String traceId = extractTraceIdFromRequest(req);
		if (traceId != null) {
			TraceLoggingEnhancer.setCurrentTraceId(extractTraceIdFromRequest(req));
		}
		return true;
	}

	@Override
	public void afterCompletion(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, Object handler, Exception e) throws Exception {
		// Note: the thread-local is currently not fully cleared, but just set to null
		// See: https://github.com/GoogleCloudPlatform/google-cloud-java/issues/2746
		TraceLoggingEnhancer.setCurrentTraceId(null);
	}

	/**
	 * Extracts trace ID from the HTTP request by checking request headers.
	 *
	 * @param req The HTTP servlet request.
	 * @return The trace ID or null, if none found.
	 */
	public String extractTraceIdFromRequest(HttpServletRequest req) {
		String traceId;
		for (String header : TRACE_HEADERS) {
			traceId = extractTraceIdFromRequestWithHeader(req, header);
			if (traceId != null) {
				return traceId;
			}
		}
		return null;
	}

	private String extractTraceIdFromRequestWithHeader(HttpServletRequest req,
			String header) {
		String traceId = req.getHeader(header);

		if (traceId != null) {
			int slash = traceId.indexOf('/');
			if (slash >= 0) {
				traceId = traceId.substring(0, slash);
			}
		}

		return traceId;
	}
}
