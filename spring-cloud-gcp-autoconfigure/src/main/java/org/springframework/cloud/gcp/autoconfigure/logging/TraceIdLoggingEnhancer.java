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
 * Adds the trace ID to the logging entry, in its correct format to be displayed in the Logs viewer.
 *
 * @author João André Martins
 */
public class TraceIdLoggingEnhancer implements LoggingEnhancer {

	private static final ThreadLocal<String> traceId = new ThreadLocal<>();

	private static final String APP_ENGINE_LABEL_NAME = "appengine.googleapis.com/trace_id";

	private GcpProjectIdProvider projectIdProvider = new DefaultGcpProjectIdProvider();

	private boolean runningOnAppEngine = System.getenv("GAE_INSTANCE") != null;

	public static void setCurrentTraceId(String id) {
		if (id == null) {
			traceId.remove();
		}
		else {
			traceId.set(id);
		}
	}

	/**
	 * @return the trace ID stored through {@link #setCurrentTraceId(String)}
	 */
	public static String getCurrentTraceId() {
		return traceId.get();
	}

	/**
	 * Set the trace field of the log entry to the current trace ID.
	 * <p>The current trace ID is either the trace ID stored in the Mapped Diagnostic Context (MDC)
	 * under the "X-B3-TraceId" key or, if none set, the current trace ID set by
	 * {@link #setCurrentTraceId(String)}.
	 * <p>The trace ID is set in the log entry in the "projects/[GCP_PROJECT_ID]/traces/[TRACE_ID]"
	 * format, in order to be associated to traces by the Google Cloud Console.
	 * <p>If an application is running on Google App Engine, the trace ID is also stored in the
	 * "appengine.googleapis.com/trace_id" field, in order to enable log correlation on the logs
	 * viewer.
	 * @param builder log entry builder
	 */
	@Override
	public void enhanceLogEntry(LogEntry.Builder builder) {
		// In order not to duplicate the whole google-cloud-logging-logback LoggingAppender to add
		// the trace ID from the MDC there, we're doing it here.
		// This requires a call to the org.slf4j package.
		String traceId = org.slf4j.MDC.get(StackdriverTraceConstants.MDC_FIELD_TRACE_ID);
		if (traceId == null) {
			traceId = getCurrentTraceId();
		}

		if (traceId != null) {
			builder.setTrace(StackdriverTraceConstants.composeFullTraceName(
					this.projectIdProvider.getProjectId(), traceId));
			if (this.runningOnAppEngine) {
				builder.addLabel(APP_ENGINE_LABEL_NAME, traceId);
			}
		}
	}
}
