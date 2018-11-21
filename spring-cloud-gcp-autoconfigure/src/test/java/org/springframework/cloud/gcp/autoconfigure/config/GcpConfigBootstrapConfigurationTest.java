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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jisha Abubaker
 * @author João André Martins
 */
public class GcpConfigBootstrapConfigurationTest {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpConfigBootstrapConfiguration.class));

	@Test
	public void testConfigurationValueDefaultsAreAsExpected() {
		this.contextRunner.run(context -> {
			GcpConfigProperties config = context.getBean(GcpConfigProperties.class);
			assertThat(config.getName()).isEqualTo("application");
			assertThat(config.getProfile()).isEqualTo("default");
			assertThat(config.getTimeoutMillis()).isEqualTo(60000);
			assertThat(config.isEnabled()).isFalse();
		});
	}

	@Test
	public void testConfigurationValuesAreCorrectlyLoaded() {
		this.contextRunner.withPropertyValues("spring.application.name=myapp",
				"spring.profiles.active=prod",
				"spring.cloud.gcp.config.timeoutMillis=120000",
				"spring.cloud.gcp.config.enabled=false",
				"spring.cloud.gcp.config.project-id=pariah")
				.run(context -> {
					GcpConfigProperties config = context.getBean(GcpConfigProperties.class);
					assertThat(config.getName()).isEqualTo("myapp");
					assertThat(config.getProfile()).isEqualTo("prod");
					assertThat(config.getTimeoutMillis()).isEqualTo(120000);
					assertThat(config.isEnabled()).isFalse();
					assertThat(config.getProjectId()).isEqualTo("pariah");
				});
	}
}
