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

package com.google.cloud.spring.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.status.Status;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.slf4j.MDCContextMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Stackdriver Json Layout Logger.
 *
 * @author Andreas Berger
 * @author Mike Eltsufin
 * @author Stefan Dieringer
 */
public class StackdriverJsonLayoutLoggerTests {

	private static final Gson GSON = new Gson();

	private static final PrintStream CONSOLE_OUTPUT = System.out;

	private static final Log LOGGER = LogFactory.getLog("StackdriverJsonLayoutLoggerTests");

	private ByteArrayOutputStream logOutput;

	private MDCContextMap mdc;

	@Before
	public void setLoggingContext() {
		logOutput = new ByteArrayOutputStream();
		System.setOut(new java.io.PrintStream(logOutput));

		mdc = new MDCContextMap();
		mdc.put(StackdriverTraceConstants.MDC_FIELD_TRACE_ID, "12345678901234561234567890123456");
		mdc.put(StackdriverTraceConstants.MDC_FIELD_SPAN_ID, "span123");
		mdc.put(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT, "true");
		mdc.put("foo", "bar");
	}

	@After
	public void cleanupLoggingContext() {
		// Reset the System output to System.out
		System.setOut(CONSOLE_OUTPUT);
		mdc.clear();
	}

	@Test
	public void testEmulatorConfig() {
		LOGGER.warn("test");
		Map<String, String> data = getLogMetadata();

		assertThat(data)
				.isNotNull()
				.containsEntry("foo", "bar")
				.containsEntry(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, "test")
				.containsEntry(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, "WARNING")
				.containsEntry(StackdriverJsonLayout.LOGGER_ATTR_NAME, "StackdriverJsonLayoutLoggerTests")
				.containsEntry(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE, "projects/test-project/traces/12345678901234561234567890123456")
				.containsEntry(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, "span123")
				.containsKey(StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE)
				.containsKey(StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE)
				.doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_TRACE_ID)
				.doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_ID)
				.doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT)
				.doesNotContainKey(JsonLayout.TIMESTAMP_ATTR_NAME);
	}

	@Test
	public void testServiceContext() {
		Logger logger = LoggerFactory.getLogger("StackdriverJsonLayoutServiceCtxLoggerTests");

		logger.warn("test");
		Map<String, String> data = getLogMetadata();

		assertThat(data)
				.isNotNull()
				.containsEntry(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, "test")
				.containsEntry(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, "WARNING")
				.containsEntry(StackdriverJsonLayout.LOGGER_ATTR_NAME, "StackdriverJsonLayoutServiceCtxLoggerTests")
				.containsEntry(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE, "projects/test-project/traces/12345678901234561234567890123456")
				.containsEntry(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, "span123")
				.containsEntry("foo", "bar")
				.containsEntry("custom-key", "custom-value")
				.containsKey(StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE)
				.containsKey(StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE)
				.containsKey(StackdriverTraceConstants.SERVICE_CONTEXT_ATTRIBUTE)
				.doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_TRACE_ID)
				.doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_ID)
				.doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT)
				.doesNotContainKey(JsonLayout.TIMESTAMP_ATTR_NAME);

		// test service context
		Object serviceCtxObject = data.get(StackdriverTraceConstants.SERVICE_CONTEXT_ATTRIBUTE);
		assertThat(serviceCtxObject).isInstanceOf(Map.class);
		Map<String, String> serviceContextMap = (Map) serviceCtxObject;
		assertThat(serviceContextMap)
				.containsEntry("service", "service")
				.containsEntry("version", "version");
	}

	@Test
	public void test64BitTraceId() {
		mdc.put(StackdriverTraceConstants.MDC_FIELD_TRACE_ID, "1234567890123456");

		LOGGER.warn("test");
		Map<String, String> data = getLogMetadata();

		assertThat(data).containsEntry(
				StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
				"projects/test-project/traces/00000000000000001234567890123456");
	}

	@Test
	public void testEnhancerNoMdc() {
		// Test if no MDC is set.
		mdc.clear();
		String traceId = "1234567890123456";
		TraceIdLoggingEnhancer.setCurrentTraceId(traceId);

		LOGGER.warn("test");

		Map<String, String> data = getLogMetadata();
		assertThat(data).containsEntry(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
				"projects/test-project/traces/0000000000000000" + traceId);
	}

	@Test
	public void testJsonLayoutEnhancer() {
		Logger logger = LoggerFactory.getLogger("StackdriverJsonLayoutServiceCtxLoggerTests");

		Marker marker = MarkerFactory.getMarker("testMarker");
		logger.warn(marker, "test");

		Map<String, String> data = getLogMetadata();
		assertThat(data).containsEntry("marker", "testMarker");
	}

	@Test
	public void testJsonSeverityLevelMapping() {
		// Tests that Logback levels are mapped to correct com.google.cloud logging severities.
		LOGGER.trace("test");
		LOGGER.debug("test");
		LOGGER.info("test");
		LOGGER.warn("test");
		LOGGER.error("test");
		LOGGER.fatal("test");

		List<String> jsonLogRecords = Arrays.asList(new String(logOutput.toByteArray()).split("\n"));

		List<String> logSeverities =
				jsonLogRecords.stream()
						.map(record -> GSON.fromJson(record, Map.class))
						.map(data -> (String) data.get(StackdriverTraceConstants.SEVERITY_ATTRIBUTE))
						.collect(Collectors.toList());

		assertThat(logSeverities)
				.containsExactly("DEBUG", "DEBUG", "INFO", "WARNING", "ERROR", "ERROR");
	}

	@Test
	public void testJsonLayoutEnhancer_missing() {
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		assertThat(lc.getStatusManager().getCopyOfStatusList()
				.stream()
				.filter(s -> s.getLevel() == Status.ERROR)
				.map(s -> s.getThrowable().getCause())
				.filter(t -> t instanceof IllegalArgumentException)
				.findFirst()
		).isPresent();

	}

	private Map<String, String> getLogMetadata() {
		return GSON.fromJson(new String(logOutput.toByteArray()), Map.class);
	}

	static class JsonLayoutTestEnhancer implements JsonLoggingEventEnhancer {

		@Override
		public void enhanceJsonLogEntry(Map<String, Object> jsonMap, ILoggingEvent event) {
			if (event.getMarker() != null) {
				String name = event.getMarker().getName();
				jsonMap.put("marker", name);
			}
		}
	}
}
