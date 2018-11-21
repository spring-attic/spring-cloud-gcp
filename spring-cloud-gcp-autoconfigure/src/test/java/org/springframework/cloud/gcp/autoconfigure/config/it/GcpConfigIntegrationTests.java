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

package org.springframework.cloud.gcp.autoconfigure.config.it;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.gcp.autoconfigure.config.GcpConfigBootstrapConfiguration;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author João André Martins
 */
public class GcpConfigIntegrationTests {

	private ConfigurableApplicationContext context;

	@BeforeClass
	public static void enableTests() {
		assumeThat(System.getProperty("it.config")).isEqualTo("true");
	}

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testConfiguration() {
		this.context = new SpringApplicationBuilder()
				.sources(GcpContextAutoConfiguration.class, GcpConfigBootstrapConfiguration.class)
				.web(WebApplicationType.NONE)
				.properties("spring.cloud.gcp.config.enabled=true",
						"spring.application.name=myapp",
						"spring.profiles.active=dontexist,prod")
				.run();

		assertThat(this.context.getEnvironment().getProperty("myapp.queue-size"))
				.isEqualTo("200");
		assertThat(this.context.getEnvironment().getProperty("myapp.feature-x-enabled"))
				.isEqualTo("true");
	}
}
