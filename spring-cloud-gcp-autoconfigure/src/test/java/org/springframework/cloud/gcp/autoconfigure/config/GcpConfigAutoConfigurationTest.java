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

package org.springframework.cloud.gcp.autoconfigure.config;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Jisha Abubaker
 */
public class GcpConfigAutoConfigurationTest {
	private AnnotationConfigApplicationContext context;

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testConfigurationValueDefaultsAreAsExpected() {
		loadEnvironment();
		GcpConfigProperties config = this.context.getBean(GcpConfigProperties.class);
		assertEquals(config.getName(), null);
		assertEquals(config.getProfile(), "default");
		assertEquals(config.getTimeoutMillis(), 60000);
		assertEquals(config.isEnabled(), true);
	}

	@Test
	public void testConfigurationValuesAreCorrectlyLoaded() {
		loadEnvironment("spring.cloud.gcp.config.name=myapp",
				"spring.cloud.gcp.config.profile=prod",
				"spring.cloud.gcp.config.timeout=120000",
				"spring.cloud.gcp.config.enabled=false",
				"spring.cloud.gcp.config.project-id=pariah");
		GcpConfigProperties config = this.context.getBean(GcpConfigProperties.class);
		assertEquals(config.getName(), "myapp");
		assertEquals(config.getProfile(), "prod");
		assertEquals(config.getTimeoutMillis(), 120000);
		assertFalse(config.isEnabled());
		assertEquals(config.getProjectId(), "pariah");
	}

	private void loadEnvironment(String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, environment);
		context.register(GcpConfigAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}
}
