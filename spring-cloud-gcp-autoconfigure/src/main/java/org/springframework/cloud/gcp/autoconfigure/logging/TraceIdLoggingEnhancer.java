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

import com.google.cloud.logging.LogEntry;
import com.google.cloud.logging.LoggingEnhancer;

import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;

/**
 * Adds the trace ID from the Mapped Diagnostic Context (MDC) to the logging entry, in its correct
 * format to be displayed in the Google Cloud Console Logs viewer.
 *
 * @author João André Martins
 */
public class TraceIdLoggingEnhancer implements LoggingEnhancer {

	private GcpProjectIdProvider projectIdProvider = new DefaultGcpProjectIdProvider();

	@Override
	public void enhanceLogEntry(LogEntry.Builder builder) {
		// In order not to duplicate the whole google-cloud-logging-logback LoggingAppender to
		// add the trace ID there, we're getting it here from the MDC. This requires a call to the
		// org.slf4j package.
		String traceId = org.slf4j.MDC.get(StackdriverTraceConstants.MDC_FIELD_TRACE_ID);
		if (traceId != null) {
			builder.setTrace(
					"projects/" + this.projectIdProvider.getProjectId() + "/traces/" + traceId);
		}
	}
}
