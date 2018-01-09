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

package org.springframework.cloud.gcp.logging.autoconfig;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.logging.TraceIdLoggingWebMvcInterceptor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mike Eltsufin
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { StackdriverLoggingAutoConfiguration.class }, properties = {
		"debug" }, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
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

	public static class StackdriverLoggingAutoConfigurationDefaultTests
			extends StackdriverLoggingAutoConfigurationTests {

		@Test
		public void test() {
			assertThat(countTraceIdInterceptors(), is(1));
		}
	}

	@TestPropertySource(properties = {
			"spring.cloud.gcp.logging.enabled=false" })
	public static class StackdriverLoggingAutoConfigurationDisabledTests
			extends StackdriverLoggingAutoConfigurationTests {

		@Test
		public void test() {
			assertThat(countTraceIdInterceptors(), is(0));
		}
	}
}
