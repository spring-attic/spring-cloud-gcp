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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Mike Eltsufin
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggingWebMvcConfigurerTests {

	@Mock
	private TraceIdLoggingWebMvcInterceptor interceptor;

	@Test
	public void testAddInterceptors() {
		LoggingWebMvcConfigurer adapter = new LoggingWebMvcConfigurer(this.interceptor,
				new LoggingWebMvcConfigurerSettings());
		TestInterceptorRegistry registry = new TestInterceptorRegistry();

		adapter.addInterceptors(registry);

		assertThat(registry.doGetInterceptors().size(), is(1));
		assertThat(registry.doGetInterceptors().get(0), is(this.interceptor));
	}

	@Test
	public void testGetTraceIdExtractorsDefault() {
		TraceIdExtractor[] extractors = LoggingWebMvcConfigurer
				.getCompositeExtractor(null).getExtractors();

		assertEquals(2, extractors.length);
		assertTrue(extractors[0] instanceof XCloudTraceIdExtractor);
		assertTrue(extractors[1] instanceof ZipkinTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsPrioritizeXCloudTrace() {
		TraceIdExtractor[] extractors = LoggingWebMvcConfigurer
				.getCompositeExtractor(TraceIdExtractorType.XCLOUD_ZIPKIN).getExtractors();

		assertEquals(2, extractors.length);
		assertTrue(extractors[0] instanceof XCloudTraceIdExtractor);
		assertTrue(extractors[1] instanceof ZipkinTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsPrioritizeZipkinTrace() {
		TraceIdExtractor[] extractors = LoggingWebMvcConfigurer
				.getCompositeExtractor(TraceIdExtractorType.ZIPKIN_XCLOUD).getExtractors();

		assertEquals(2, extractors.length);
		assertTrue(extractors[0] instanceof ZipkinTraceIdExtractor);
		assertTrue(extractors[1] instanceof XCloudTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsOnlyXCloud() {
		TraceIdExtractor[] extractors = LoggingWebMvcConfigurer
				.getCompositeExtractor(TraceIdExtractorType.XCLOUD).getExtractors();

		assertEquals(1, extractors.length);
		assertTrue(extractors[0] instanceof XCloudTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsOnlyZipkin() {
		TraceIdExtractor[] extractors = LoggingWebMvcConfigurer
				.getCompositeExtractor(TraceIdExtractorType.ZIPKIN).getExtractors();

		assertEquals(1, extractors.length);
		assertTrue(extractors[0] instanceof ZipkinTraceIdExtractor);
	}

	/**
	 * Test interceptor registry that makes interceptors list accessible.
	 */
	private static class TestInterceptorRegistry extends InterceptorRegistry {
		public List<Object> doGetInterceptors() {
			return super.getInterceptors();
		}
	}

}
