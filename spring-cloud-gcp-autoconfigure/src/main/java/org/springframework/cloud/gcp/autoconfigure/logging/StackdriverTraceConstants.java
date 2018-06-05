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

package org.springframework.cloud.gcp.autoconfigure.logging;

import org.springframework.util.Assert;

/**
 * @author João André Martins
 */
public final class StackdriverTraceConstants {

	private StackdriverTraceConstants() { }

	/**
	 * The JSON field name for the log level (severity)
	 */
	public static final String SEVERITY_ATTRIBUTE = "severity";

	/**
	 * The JSON field name for the seconds of the timestamp
	 */
	public static final String TIMESTAMP_SECONDS_ATTRIBUTE = "timestampSeconds";

	/**
	 * The JSON field name for the nanos of the timestamp
	 */
	public static final String TIMESTAMP_NANOS_ATTRIBUTE = "timestampNanos";

	/**
	 * The JSON field name for the span-id
	 */
	public static final String SPAN_ID_ATTRIBUTE = "logging.googleapis.com/spanId";

	/**
	 * The JSON field name for the trace-id
	 */
	public static final String TRACE_ID_ATTRIBUTE = "logging.googleapis.com/trace";

	/**
	 * The name of the MDC parameter, Spring Sleuth is storing the trace id at
	 */
	public static final String MDC_FIELD_TRACE_ID = "X-B3-TraceId";

	/**
	 * The name of the MDC parameter, Spring Sleuth is storing the span id at
	 */
	public static final String MDC_FIELD_SPAN_ID = "X-B3-SpanId";

	/**
	 * The name of the MDC parameter, Spring Sleuth is storing the span export information at
	 */
	public static final String MDC_FIELD_SPAN_EXPORT = "X-Span-Export";

	/**
	 * Composes the full trace name as expected by the Google Developers Console log viewer, to
	 * enable trace correlation with log entries.
	 * @param projectId The GCP project ID
	 * @param traceId The trace ID
	 * @return the trace name in the "projects/[GCP_PROJECT_ID]/trace/[TRACE_ID]" format
	 */
	public static String composeFullTraceName(String projectId, String traceId) {
		Assert.notNull(projectId, "The project ID can't be null.");
		Assert.notNull(traceId, "The trace ID can't be null.");
		return "projects/" + projectId + "/traces/" + traceId;
	}
}
