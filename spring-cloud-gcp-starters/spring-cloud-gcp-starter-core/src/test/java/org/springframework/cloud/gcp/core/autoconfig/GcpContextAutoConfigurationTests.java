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

package org.springframework.cloud.gcp.core.autoconfig;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author João André Martins
 */
public class GcpContextAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testGetProjectIdProvider_withGcpProperties() {
		loadEnvironment("spring.cloud.gcp.projectId=test-project");
		GcpProjectIdProvider provider = this.context.getBean(GcpProjectIdProvider.class);

		assertEquals("test-project", provider.getProjectId().get());
	}

	@Test
	public void testGetProjectIdProvider_withoutGcpProperties() {
		loadEnvironment();
		assertTrue(this.context.getBean(GcpProjectIdProvider.class)
				instanceof DefaultGcpProjectIdProvider);
	}

	private void loadEnvironment(String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(GcpContextAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(context, environment);
		context.refresh();
		this.context = context;
	}
}
