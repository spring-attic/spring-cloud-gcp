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

			Map data = new Gson().fromJson(new String(out.toByteArray()), Map.class);

			checkData(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, "test", data);
			checkData(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, "WARN", data);
			checkData(StackdriverJsonLayout.LOGGER_ATTR_NAME, "StackdriverJsonLayoutLoggerTests", data);
			checkData(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
					"projects/test-project/traces/12345678901234561234567890123456", data);
			checkData(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, "span123", data);
			checkData("foo", "bar", data);
			assertThat(data).doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_TRACE_ID);
			assertThat(data).doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_ID);
			assertThat(data).doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT);
			assertThat(data).doesNotContainKey(JsonLayout.TIMESTAMP_ATTR_NAME);
			assertThat(data).containsKey(StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE);
			assertThat(data).containsKey(StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE);
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
			Map<String, Object> data = new Gson().fromJson(new String(out.toByteArray()), stringObjType);

			checkData(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME, "test", data);
			checkData(StackdriverTraceConstants.SEVERITY_ATTRIBUTE, "WARN", data);
			checkData(StackdriverJsonLayout.LOGGER_ATTR_NAME, "StackdriverJsonLayoutServiceCtxLoggerTests", data);
			checkData(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
					"projects/test-project/traces/12345678901234561234567890123456", data);
			checkData(StackdriverTraceConstants.SPAN_ID_ATTRIBUTE, "span123", data);
			checkData("foo", "bar", data);
			assertThat(data).doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_TRACE_ID);
			assertThat(data).doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_ID);
			assertThat(data).doesNotContainKey(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT);
			assertThat(data).doesNotContainKey(JsonLayout.TIMESTAMP_ATTR_NAME);
			assertThat(data).containsKey(StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE);
			assertThat(data).containsKey(StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE);
			assertThat(data).containsEntry("custom-key", "custom-value");

			// test service context
			assertThat(data).containsKey(StackdriverTraceConstants.SERVICE_CONTEXT_ATTRIBUTE);
			Object serviceCtxObject = data.get(StackdriverTraceConstants.SERVICE_CONTEXT_ATTRIBUTE);
			assertThat(serviceCtxObject).isInstanceOf(Map.class);
			Map<String, String> serviceContextMap = (Map) serviceCtxObject;
			assertThat(serviceContextMap).containsEntry("service", "service");
			assertThat(serviceContextMap).containsEntry("version", "version");
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

			Map data = new Gson().fromJson(new String(out.toByteArray()), Map.class);

			checkData(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
					"projects/test-project/traces/00000000000000001234567890123456", data);
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

			Map data = new Gson().fromJson(new String(out.toByteArray()), Map.class);

			checkData(StackdriverTraceConstants.TRACE_ID_ATTRIBUTE,
					"projects/test-project/traces/0000000000000000" + traceId, data);
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

			Map data = new Gson().fromJson(new String(out.toByteArray()), Map.class);
			checkData("marker", "testMarker", data);
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

	private void checkData(String attribute, Object value, Map data) {
		Object actual = data.get(attribute);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(value);
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
