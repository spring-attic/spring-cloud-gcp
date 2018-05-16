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

/**
 * Adds the trace ID to the logging entry, in its correct format to be displayed in the Logs viewer.
 *
 * @author João André Martins
 */
public class TraceIdLoggingEnhancer implements LoggingEnhancer {

	private static final ThreadLocal<String> traceId = new ThreadLocal<>();

	public static void setCurrentTraceId(String id) {
		if (id == null) {
			traceId.remove();
		}
		else {
			traceId.set(id);
		}
	}

	public static String getTraceId() {
		return traceId.get();
	}

	@Override
	public void enhanceLogEntry(LogEntry.Builder builder) {
		String projectId = new DefaultGcpProjectIdProvider().getProjectId();
		String traceId = getTraceId();
		if (traceId != null) {
			builder.setTrace("projects/" + projectId + "/traces/" + traceId);
		}
	}
}
