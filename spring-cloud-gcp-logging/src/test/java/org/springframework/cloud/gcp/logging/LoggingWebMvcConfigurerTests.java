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
	public void testGetTraceIdExtractorsPrioritizeXCloudTrace() {
		LoggingWebMvcConfigurerSettings settings = new LoggingWebMvcConfigurerSettings();
		settings.setPrioritizeXCloudTrace(true);
		settings.setxCloudTrace(true);
		settings.setZipkinTrace(true);

		List<TraceIdExtractor> extractors =
				LoggingWebMvcConfigurer.getTraceIdExtractors(settings);

		assertEquals(2, extractors.size());
		assertTrue(extractors.get(0) instanceof XCloudTraceIdExtractor);
		assertTrue(extractors.get(1) instanceof ZipkinTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsPrioritizeZipkinTrace() {
		LoggingWebMvcConfigurerSettings settings = new LoggingWebMvcConfigurerSettings();
		settings.setPrioritizeXCloudTrace(false);
		settings.setxCloudTrace(true);
		settings.setZipkinTrace(true);

		List<TraceIdExtractor> extractors =
				LoggingWebMvcConfigurer.getTraceIdExtractors(settings);

		assertEquals(2, extractors.size());
		assertTrue(extractors.get(0) instanceof ZipkinTraceIdExtractor);
		assertTrue(extractors.get(1) instanceof XCloudTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsOnlyXCloud() {
		LoggingWebMvcConfigurerSettings settings = new LoggingWebMvcConfigurerSettings();
		settings.setPrioritizeXCloudTrace(false);
		settings.setxCloudTrace(true);
		settings.setZipkinTrace(false);

		List<TraceIdExtractor> extractors =
				LoggingWebMvcConfigurer.getTraceIdExtractors(settings);

		assertEquals(1, extractors.size());
		assertTrue(extractors.get(0) instanceof XCloudTraceIdExtractor);
	}

	@Test
	public void testGetTraceIdExtractorsOnlyZipkin() {
		LoggingWebMvcConfigurerSettings settings = new LoggingWebMvcConfigurerSettings();
		settings.setPrioritizeXCloudTrace(true);
		settings.setxCloudTrace(false);
		settings.setZipkinTrace(true);

		List<TraceIdExtractor> extractors =
				LoggingWebMvcConfigurer.getTraceIdExtractors(settings);

		assertEquals(1, extractors.size());
		assertTrue(extractors.get(0) instanceof ZipkinTraceIdExtractor);
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
