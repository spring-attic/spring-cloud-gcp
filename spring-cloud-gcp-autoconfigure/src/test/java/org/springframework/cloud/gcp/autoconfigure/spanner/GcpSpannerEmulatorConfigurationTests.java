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

package org.springframework.cloud.gcp.autoconfigure.spanner;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.spanner.SpannerOptions;
import org.junit.Assert;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

/**
 * @author Olav Loite
 */
public class GcpSpannerEmulatorConfigurationTests {
	private static final String DEFAULT_CLOUDSPANNER_HOST = "https://spanner.googleapis.com";

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.gcp.spanner.emulator-host=https://localhost:8443",
					"spring.cloud.gcp.spanner.project-id=test-project",
					"spring.cloud.gcp.spanner.instance-id=test-instance",
					"spring.cloud.gcp.spanner.database=test-database")
			.withUserConfiguration(TestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(GcpSpannerEmulatorConfiguration.class,
					GcpContextAutoConfiguration.class,
					GcpSpannerAutoConfiguration.class));

	@Test
	public void testEmulatorConfig() {
		this.contextRunner.run(context -> {
			SpannerOptions spannerOptions = context.getBean(SpannerOptions.class);
			Assert.assertEquals("SpannerOptions#host is not correct", "https://localhost:8443",
					spannerOptions.getHost());
		});
	}

	private ApplicationContextRunner contextRunnerWithoutEmulator = new ApplicationContextRunner()
			.withPropertyValues(
					"spring.cloud.gcp.spanner.project-id=test-project",
					"spring.cloud.gcp.spanner.instance-id=test-instance",
					"spring.cloud.gcp.spanner.database=test-database")
			.withUserConfiguration(TestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(GcpSpannerEmulatorConfiguration.class,
					GcpContextAutoConfiguration.class,
					GcpSpannerAutoConfiguration.class));

	@Test
	public void testWithoutEmulatorConfig() {
		this.contextRunnerWithoutEmulator.run(context -> {
			SpannerOptions spannerOptions = context.getBean(SpannerOptions.class);
			Assert.assertEquals("SpannerOptions#host is not correct", DEFAULT_CLOUDSPANNER_HOST,
					spannerOptions.getHost());
		});
	}

	@AutoConfigurationPackage
	static class TestConfiguration {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}
	}

}
