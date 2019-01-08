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

package org.springframework.cloud.gcp.autoconfigure.core;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.core.DefaultGcpEnvironmentProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpEnvironmentProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the top-level context auto-configuration.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
public class GcpContextAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpContextAutoConfiguration.class));

	@Test
	public void testGetProjectIdProvider_withGcpProperties() {
		this.contextRunner.withPropertyValues("spring.cloud.gcp.projectId=tonberry")
				.run((context) -> {
					GcpProjectIdProvider projectIdProvider =
							context.getBean(GcpProjectIdProvider.class);
					assertThat(projectIdProvider.getProjectId()).isEqualTo("tonberry");
				});
	}

	@Test
	public void testGetProjectIdProvider_withoutGcpProperties() {
		this.contextRunner.run((context) -> {
			GcpProjectIdProvider projectIdProvider =
					context.getBean(GcpProjectIdProvider.class);
			assertThat(projectIdProvider).isInstanceOf(DefaultGcpProjectIdProvider.class);
		});
	}

	@Test
	public void testEnvironmentProvider() {
		this.contextRunner
				.run((context) -> {
					GcpEnvironmentProvider environmentProvider = context.getBean(GcpEnvironmentProvider.class);
					assertThat(environmentProvider).isNotNull();
					assertThat(environmentProvider).isInstanceOf(DefaultGcpEnvironmentProvider.class);
				});
	}
}
