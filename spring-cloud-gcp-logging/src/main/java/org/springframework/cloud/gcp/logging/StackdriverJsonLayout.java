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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import com.google.gson.Gson;

import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.util.StringUtils;

/**
 * This class provides a JSON layout for a Logback appender compatible to the Stackdriver
 * log format.
 *
 * Reference: https://cloud.google.com/logging/docs/agent/configuration#process-payload
 *
 * @author Andreas Berger
 * @author Chengyuan Zhao
 * @author Stefan Dieringer
 */
public class StackdriverJsonLayout extends JsonLayout {

	private static final Set<String> FILTERED_MDC_FIELDS = new HashSet<>(Arrays.asList(
			StackdriverTraceConstants.MDC_FIELD_TRACE_ID,
			StackdriverTraceConstants.MDC_FIELD_SPAN_ID,
			StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT));

	private String projectId;

	private boolean includeTraceId;

	private boolean includeSpanId;

	private boolean includeExceptionInMessage;

	private StackdriverErrorReportingServiceContext serviceContext;

	private Map<String, Object> customJson;

	/**
	 * creates a layout for a Logback appender compatible to the Stackdriver log format.
	 */
	public StackdriverJsonLayout() {
		this.appendLineSeparator = true;
		this.includeExceptionInMessage = true;
		this.includeException = false;
		this.includeTraceId = true;
		this.includeSpanId = true;
		Gson formatter = new Gson();
		setJsonFormatter(formatter::toJson);
	}

	/**
	 * Get the project id.
	 * @return the Google Cloud project id relevant for logging the traceId
	 */
	public String getProjectId() {
		return this.projectId;
	}

	/**
	 * set the project id.
	 * @param projectId the Google Cloud project id relevant for logging the traceId
	 */
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	/**
	 * check if the trace id is included.
	 * @return true if the traceId should be included into the JSON
	 */
	public boolean isIncludeTraceId() {
		return this.includeTraceId;
	}

	/**
	 * set whether the trace id is included.
	 * @param includeTraceId true if the traceId should be included into the JSON
	 */
	public void setIncludeTraceId(boolean includeTraceId) {
		this.includeTraceId = includeTraceId;
	}

	/**
	 * check if the span id is included.
	 * @return true if the spanId should be included into the JSON
	 */
	public boolean isIncludeSpanId() {
		return this.includeSpanId;
	}

	/**
	 * set whether the span id is included.
	 * @param includeSpanId true if the spanId should be included into the JSON
	 */
	public void setIncludeSpanId(boolean includeSpanId) {
		this.includeSpanId = includeSpanId;
	}

	/**
	 * check if there is an included exception in the message.
	 * @return true if the exception should be added to the message
	 */
	public boolean isIncludeExceptionInMessage() {
		return this.includeExceptionInMessage;
	}

	/**
	 * set whether the exception is included in the message.
	 * @param includeExceptionInMessage true if the exception should be added to the message
	 */
	public void setIncludeExceptionInMessage(boolean includeExceptionInMessage) {
		this.includeExceptionInMessage = includeExceptionInMessage;
	}

	/**
	 * set the service context for stackdriver.
	 * @param serviceContext the service context
	 * @since 1.2
	 */
	public void setServiceContext(StackdriverErrorReportingServiceContext serviceContext) {
		this.serviceContext = serviceContext;
	}

	/**
	 * set custom json data to include in log output.
	 * @param json json string
	 * @since 1.2
	 */
	public void setCustomJson(String json) {
		Gson gson = new Gson();
		this.customJson = gson.fromJson(json, Map.class);
	}


	@Override
	public void start() {
		super.start();

		// If no Project ID set, then attempt to resolve it with the default project ID provider
		if (StringUtils.isEmpty(this.projectId) || this.projectId.endsWith("_IS_UNDEFINED")) {
			GcpProjectIdProvider projectIdProvider = new DefaultGcpProjectIdProvider();
			this.projectId = projectIdProvider.getProjectId();
		}
	}

	/**
	 * Convert a logging event into a Map.
	 * @param event the logging event
	 * @return the map which should get rendered as JSON
	 */
	@Override
	protected Map<String, Object> toJsonMap(ILoggingEvent event) {

		Map<String, Object> map = new LinkedHashMap<>();

		if (this.includeMDC) {
			event.getMDCPropertyMap().forEach((key, value) -> {
				if (!FILTERED_MDC_FIELDS.contains(key)) {
					map.put(key, value);
				}
			});
		}
		if (this.includeTimestamp) {
			map.put(StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE,
					TimeUnit.MILLISECONDS.toSeconds(event.getTimeStamp()));
			map.put(StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE,
					TimeUnit.MILLISECONDS.toNanos(event.getTimeStamp() % 1_000));
		}

		add(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, this.includeLevel,
				String.valueOf(event.getLevel()), map);
		add(JsonLayout.THREAD_ATTR_NAME, this.includeThreadName, event.getThreadName(), map);
		add(JsonLayout.LOGGER_ATTR_NAME, this.includeLoggerName, event.getLoggerName(), map);

		if (this.includeFormattedMessage) {
			String message = event.getFormattedMessage();
			if (this.includeExceptionInMessage) {
				IThrowableProxy throwableProxy = event.getThrowableProxy();
				if (throwableProxy != null) {
					String stackTrace = getThrowableProxyConverter().convert(event);
					if (stackTrace != null && !stackTrace.equals("")) {
						message += "\n" + stackTrace;
					}
				}
			}
			map.put(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, message);
		}
		add(JsonLayout.MESSAGE_ATTR_NAME, this.includeMessage, event.getMessage(), map);
		add(JsonLayout.CONTEXT_ATTR_NAME, this.includeContextName, event.getLoggerContextVO().getName(), map);
		addThrowableInfo(JsonLayout.EXCEPTION_ATTR_NAME, this.includeException, event, map);
		addTraceId(event, map);
		add(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, this.includeSpanId,
				event.getMDCPropertyMap().get(StackdriverTraceConstants.MDC_FIELD_SPAN_ID), map);
		if (this.serviceContext != null) {
			map.put(StackdriverTraceConstants.SERVICE_CONTEXT_ATTRIBUTE, this.serviceContext);
		}
		if (this.customJson != null && !this.customJson.isEmpty()) {
			for (Map.Entry<String, Object> entry : this.customJson.entrySet()) {
				map.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		addCustomDataToJsonMap(map, event);
		return map;
	}

	protected String formatTraceId(final String traceId) {
		// Trace IDs are either 64-bit or 128-bit, which is 16-digit hex, or 32-digit hex.
		// If traceId is 64-bit (16-digit hex), then we need to prepend 0's to make a 32-digit hex.
		if (traceId != null && traceId.length() == 16) {
			return "0000000000000000" + traceId;
		}
		return traceId;
	}

	private void addTraceId(ILoggingEvent event, Map<String, Object> map) {
		if (!this.includeTraceId) {
			return;
		}

		String traceId =
				event.getMDCPropertyMap().get(StackdriverTraceConstants.MDC_FIELD_TRACE_ID);
		if (traceId == null) {
			traceId = TraceIdLoggingEnhancer.getCurrentTraceId();
		}
		if (!StringUtils.isEmpty(traceId)
				&& !StringUtils.isEmpty(this.projectId)
				&& !this.projectId.endsWith("_IS_UNDEFINED")) {
			traceId = StackdriverTraceConstants.composeFullTraceName(
					this.projectId, formatTraceId(traceId));
		}

		add(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE, this.includeTraceId, traceId, map);
	}
}
