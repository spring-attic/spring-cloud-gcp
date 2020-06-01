/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.metrics;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import io.micrometer.stackdriver.StackdriverConfig;
import io.micrometer.stackdriver.StackdriverMeterRegistry;
import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.stackdriver.StackdriverMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for auto-config.
 *
 * @author Eddú Meléndez
 */
public class GcpStackdriverMetricsAutoConfigurationTest {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpContextAutoConfiguration.class, GcpStackdriverMetricsAutoConfiguration.class,
					StackdriverMetricsExportAutoConfiguration.class, MetricsAutoConfiguration.class))
			.withUserConfiguration(Config.class);

	@Test
	public void testProjectIdIsSet() {
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.project-id=demo-project")
				.run(context -> assertThat(context).hasSingleBean(StackdriverConfig.class)
						.hasSingleBean(StackdriverMeterRegistry.class));
	}

	@Test
	public void testMetricsProjectIdIsSet() {
		this.contextRunner
				.withPropertyValues("spring.cloud.gcp.metrics.project-id=demo-project")
				.run(context -> assertThat(context).hasSingleBean(StackdriverConfig.class)
						.hasSingleBean(StackdriverMeterRegistry.class));
	}

	@Configuration
	static class Config {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return NoCredentialsProvider.create();
		}

	}

}
