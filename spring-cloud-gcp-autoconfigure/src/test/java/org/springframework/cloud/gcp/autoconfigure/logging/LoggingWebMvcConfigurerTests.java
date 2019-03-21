/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.logging;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the logging-web-mvc configurer.
 *
 * @author Mike Eltsufin
 */
@RunWith(MockitoJUnitRunner.class)
public class LoggingWebMvcConfigurerTests {

	@Mock
	private TraceIdLoggingWebMvcInterceptor interceptor;

	@Test
	public void testAddInterceptors() {
		LoggingWebMvcConfigurer adapter = new LoggingWebMvcConfigurer(this.interceptor,
				() -> "remission");
		TestInterceptorRegistry registry = new TestInterceptorRegistry();

		adapter.addInterceptors(registry);

		assertThat(registry.doGetInterceptors()).hasSize(1);
		assertThat(registry.doGetInterceptors().get(0)).isEqualTo(this.interceptor);
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
