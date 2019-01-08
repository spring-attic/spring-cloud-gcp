/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.autoconfigure.logging;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;

import ch.qos.logback.contrib.json.classic.JsonLayout;
import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.slf4j.MDCContextMap;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the Stackdriver Json Layout Logger.
 *
 * @author Andreas Berger
 * @author Mike Eltsufin
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
			assertThat(data.containsKey(StackdriverTraceConstants.MDC_FIELD_TRACE_ID)).isFalse();
			assertThat(data.containsKey(StackdriverTraceConstants.MDC_FIELD_SPAN_ID)).isFalse();
			assertThat(data.containsKey(StackdriverTraceConstants.MDC_FIELD_SPAN_EXPORT)).isFalse();
			assertThat(data.containsKey(JsonLayout.TIMESTAMP_ATTR_NAME)).isFalse();
			assertThat(data.containsKey(StackdriverTraceConstants.TIMESTAMP_SECONDS_ATTRIBUTE)).isTrue();
			assertThat(data.containsKey(StackdriverTraceConstants.TIMESTAMP_NANOS_ATTRIBUTE)).isTrue();
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


	private void checkData(String attribute, Object value, Map data) {
		Object actual = data.get(attribute);
		assertThat(actual).isNotNull();
		assertThat(actual).isEqualTo(value);
	}
}
