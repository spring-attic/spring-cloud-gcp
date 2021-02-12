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
import java.lang.reflect.Type;
import java.util.Map;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import ch.qos.logback.core.status.Status;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.slf4j.MDCContextMap;
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

	private static final Log LOGGER = LogFactory.getLog("StackdriverJsonLayoutLoggerTests");

	@Test
	public void testEmulatorConfig() {
		PrintStream oldOut = System.out;
		MDCContextMap mdc = new MDCContextMap();
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new java.io.PrintStream(out));

			mdc.put(StackdriverTraceConstants.MDC_FIELD_TRACE_ID, "12345678901234561234567890123456");
			mdc.put(StackdriverTraceConstants.MDC_FIELD_SPAN_ID, "span123");
			mdc.put(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT, "true");
			mdc.put("foo", "bar");

			LOGGER.warn("test");

			Type stringStringType = new TypeToken<Map<String, String>>() { }.getType();
			Map<String, String> data = new Gson().fromJson(new String(out.toByteArray()), stringStringType);

			assertThat(data)
					.isNotNull()
					.containsEntry("foo", "bar")
					.containsEntry(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, "test")
					.containsEntry(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, "WARN")
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
		finally {
			System.setOut(oldOut);
			mdc.clear();
		}
	}

	@Test
	public void testServiceContext() {
		PrintStream oldOut = System.out;
		MDCContextMap mdc = new MDCContextMap();
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new java.io.PrintStream(out));

			mdc.put(StackdriverTraceConstants.MDC_FIELD_TRACE_ID, "12345678901234561234567890123456");
			mdc.put(StackdriverTraceConstants.MDC_FIELD_SPAN_ID, "span123");
			mdc.put(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT, "true");
			mdc.put("foo", "bar");

			Logger logger = LoggerFactory.getLogger("StackdriverJsonLayoutServiceCtxLoggerTests");
			logger.warn("test");

			Type stringObjType = new TypeToken<Map<String, Object>>() { }.getType();
			Map<String, String> data = new Gson().fromJson(new String(out.toByteArray()), stringObjType);

			assertThat(data)
					.isNotNull()
					.containsEntry(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, "test")
					.containsEntry(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, "WARN")
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
		finally {
			System.setOut(oldOut);
			mdc.clear();
		}
	}

	@Test
	public void test64BitTraceId() {
		PrintStream oldOut = System.out;
		MDCContextMap mdc = new MDCContextMap();
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new java.io.PrintStream(out));

			mdc.put(StackdriverTraceConstants.MDC_FIELD_TRACE_ID, "1234567890123456");

			LOGGER.warn("test");

			Type stringStringType = new TypeToken<Map<String, String>>() { }.getType();
			Map<String, String> data = new Gson().fromJson(new String(out.toByteArray()), stringStringType);
			assertThat(data).containsEntry(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
					"projects/test-project/traces/00000000000000001234567890123456");
		}
		finally {
			System.setOut(oldOut);
			mdc.clear();
		}
	}

	@Test
	public void testEnhancerNoMdc() {
		PrintStream oldOut = System.out;

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new java.io.PrintStream(out));

			String traceId = "1234567890123456";
			TraceIdLoggingEnhancer.setCurrentTraceId(traceId);

			LOGGER.warn("test");

			Type stringStringType = new TypeToken<Map<String, String>>() { }.getType();
			Map<String, String> data = new Gson().fromJson(new String(out.toByteArray()), stringStringType);

			assertThat(data).containsEntry(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
					"projects/test-project/traces/0000000000000000" + traceId);
		}
		finally {
			System.setOut(oldOut);
			TraceIdLoggingEnhancer.setCurrentTraceId(null);
		}
	}

	@Test
	public void testJsonLayoutEnhancer() {
		PrintStream oldOut = System.out;

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new java.io.PrintStream(out));

			Logger logger = LoggerFactory.getLogger("StackdriverJsonLayoutServiceCtxLoggerTests");

			Marker marker = MarkerFactory.getMarker("testMarker");
			logger.warn(marker, "test");

			Type stringObjectType = new TypeToken<Map<String, Object>>() { }.getType();
			Map<String, String> data = new Gson().fromJson(new String(out.toByteArray()), stringObjectType);
			assertThat(data).containsEntry("marker", "testMarker");
		}
		finally {
			System.setOut(oldOut);
		}
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
