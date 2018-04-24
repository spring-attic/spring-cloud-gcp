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

package org.springframework.cloud.gcp.autoconfigure.logging.extractors;

import javax.servlet.http.HttpServletRequest;

/**
 * An extractor that can provide a trace ID from an HTTP request.
 *
 * @author Chengyuan Zhao
 */
public interface TraceIdExtractor {

	/**
	 * Extract trace ID from the HTTP request.
	 *
	 * @param req The HTTP servlet request.
	 * @return The trace ID or null, if none found.
	 */
	String extractTraceIdFromRequest(HttpServletRequest req);

	/**
	 * Enum values represent the order and combination of predefined trace ID extractors that
	 * can be used.
	 *
	 * @see XCloudTraceIdExtractor
	 * @see ZipkinTraceIdExtractor
	 * @see CompositeTraceIdExtractor
	 */
	enum Type {
		/**
		 * Uses only {@link XCloudTraceIdExtractor}
		 */
		XCLOUD,
		/**
		 * Uses only {@link ZipkinTraceIdExtractor}
		 */
		ZIPKIN,
		/**
		 * Uses the XCloud extractor followed by the Zipkin extractor if no trace ID was found
		 * by the former.
		 */
		XCLOUD_ZIPKIN,
		/**
		 * Uses the Zipkin extractor followed by the XCloud extractor if no trace ID was found
		 * by the former.
		 */
		ZIPKIN_XCLOUD
	}
}
