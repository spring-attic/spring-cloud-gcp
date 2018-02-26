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

import java.util.Map;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import com.google.cloud.logging.TraceLoggingEnhancer;
import com.google.gson.Gson;

/**
 * @author Andreas Berger
 */
public class StackdriverJsonLayout extends ch.qos.logback.contrib.json.classic.JsonLayout {

	public static final String SEVERITY_ATTR_NAME = "severity";

	public static final String ATTR_NAME_TIMESTAMP_SECONDS = "timestampSeconds";

	public static final String ATTR_NAME_TIMESTAMP_NANOS = "timestampNanos";

	private final String projectId;

	public StackdriverJsonLayout() {
		setAppendLineSeparator(true);
		setIncludeMessage(false);
		setIncludeException(false);
		setIncludeFormattedMessage(false);
		Gson formatter = new Gson();
		setJsonFormatter(formatter::toJson);
		this.projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
	}

	@Override
	protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
		if (this.projectId != null) {
			String traceId = TraceLoggingEnhancer.getCurrentTraceId();
			if (traceId != null) {
				map.put("logging.googleapis.com/trace", "projects/" + this.projectId + "/traces/" + traceId);
			}
		}
		String message = event.getFormattedMessage();
		IThrowableProxy throwableProxy = event.getThrowableProxy();
		if (throwableProxy != null) {
			String stackTrace = getThrowableProxyConverter().convert(event);
			if (stackTrace != null && !stackTrace.equals("")) {
				message += "\n" + stackTrace;
			}
		}
		map.put(FORMATTED_MESSAGE_ATTR_NAME, message);
	}

	@Override
	public void add(String fieldName, boolean field, String value, Map<String, Object> map) {
		String overriddenFieldName = fieldName;
		if (fieldName.equals(LEVEL_ATTR_NAME)) {
			overriddenFieldName = SEVERITY_ATTR_NAME;
		}
		super.add(overriddenFieldName, field, value, map);
	}

	@Override
	public void addTimestamp(String key, boolean field, long timeStamp, Map<String, Object> map) {
		map.put(ATTR_NAME_TIMESTAMP_SECONDS, TimeUnit.MILLISECONDS.toSeconds(timeStamp));
		map.put(ATTR_NAME_TIMESTAMP_NANOS, TimeUnit.MILLISECONDS.toNanos(timeStamp % 1_000));
	}
}
