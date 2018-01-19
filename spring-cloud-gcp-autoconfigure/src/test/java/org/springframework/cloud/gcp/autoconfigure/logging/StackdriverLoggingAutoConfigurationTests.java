/*
 *  Copyright 2017 original author or authors.
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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.logging.CompositeTraceIdExtractor;
import org.springframework.cloud.gcp.logging.TraceIdExtractor;
import org.springframework.cloud.gcp.logging.TraceIdLoggingWebMvcInterceptor;
import org.springframework.cloud.gcp.logging.XCloudTraceIdExtractor;
import org.springframework.cloud.gcp.logging.ZipkinTraceIdExtractor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Eltsufin
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { StackdriverLoggingAutoConfiguration.class },
		properties = {
		"debug",
		"spring.cloud.gcp.config.enabled=false"
		},
		webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public abstract class StackdriverLoggingAutoConfigurationTests {

	@Autowired
	private Object[] interceptors;

	public abstract void test();

	public int countTraceIdInterceptors() {
		int count = 0;
		for (int i = 0; i < this.interceptors.length; i++) {
			if (this.interceptors[i] instanceof TraceIdLoggingWebMvcInterceptor) {
				count++;
			}
		}
		return count;
	}

	@Test
	public void testGetTraceIdExtractorsDefault() {
		StackdriverLoggingProperties propertires = new StackdriverLoggingProperties();
		propertires.setTraceIdExtractor(null);
		List<TraceIdExtractor> extractors = ((CompositeTraceIdExtractor) new StackdriverLoggingAutoConfiguration()
				.traceIdExtractor(propertires)).getExtractors();

		assertEquals(2, extractors.size());
		assertTrue(extractors.get(0) instanceof XCloudTraceIdExtractor);
		assertTrue(extractors.get(1) instanceof ZipkinTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsPrioritizeXCloudTrace() {
		StackdriverLoggingProperties propertires = new StackdriverLoggingProperties();
		propertires.setTraceIdExtractor(TraceIdExtractorType.XCLOUD_ZIPKIN);
		List<TraceIdExtractor> extractors = ((CompositeTraceIdExtractor) new StackdriverLoggingAutoConfiguration()
				.traceIdExtractor(propertires)).getExtractors();

		assertEquals(2, extractors.size());
		assertTrue(extractors.get(0) instanceof XCloudTraceIdExtractor);
		assertTrue(extractors.get(1) instanceof ZipkinTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsPrioritizeZipkinTrace() {
		StackdriverLoggingProperties propertires = new StackdriverLoggingProperties();
		propertires.setTraceIdExtractor(TraceIdExtractorType.ZIPKIN_XCLOUD);
		List<TraceIdExtractor> extractors = ((CompositeTraceIdExtractor) new StackdriverLoggingAutoConfiguration()
				.traceIdExtractor(propertires)).getExtractors();

		assertEquals(2, extractors.size());
		assertTrue(extractors.get(0) instanceof ZipkinTraceIdExtractor);
		assertTrue(extractors.get(1) instanceof XCloudTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsOnlyXCloud() {
		StackdriverLoggingProperties propertires = new StackdriverLoggingProperties();
		propertires.setTraceIdExtractor(TraceIdExtractorType.XCLOUD);

		assertTrue(new StackdriverLoggingAutoConfiguration()
				.traceIdExtractor(propertires) instanceof XCloudTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsOnlyZipkin() {
		StackdriverLoggingProperties propertires = new StackdriverLoggingProperties();
		propertires.setTraceIdExtractor(TraceIdExtractorType.ZIPKIN);

		assertTrue(new StackdriverLoggingAutoConfiguration()
				.traceIdExtractor(propertires) instanceof ZipkinTraceIdExtractor);
	}

	@TestPropertySource(properties = { "spring.cloud.gcp.logging.enabled=false" })
	public static class StackdriverLoggingAutoConfigurationDisabledTests
			extends StackdriverLoggingAutoConfigurationTests {

		@Test
		public void test() {
			assertThat(countTraceIdInterceptors(), is(0));
		}
	}

	public static class StackdriverLoggingAutoConfigurationDefaultTests
			extends StackdriverLoggingAutoConfigurationTests {

		@Test
		public void test() {
			assertThat(countTraceIdInterceptors(), is(1));
		}
	}
}
