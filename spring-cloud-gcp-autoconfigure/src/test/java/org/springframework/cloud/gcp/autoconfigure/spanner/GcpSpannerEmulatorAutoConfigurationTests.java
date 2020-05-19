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

package org.springframework.cloud.gcp.autoconfigure.spanner;

import com.google.cloud.spanner.SpannerOptions;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * @author Eddú Meléndez
 */
public class GcpSpannerEmulatorAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpSpannerEmulatorAutoConfiguration.class,
					GcpSpannerAutoConfiguration.class, GcpContextAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.spanner.project-id=test-project");

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Spanner integration tests are disabled. "
						+ "Please use '-Dit.spanner=true' to enable them. ",
				System.getProperty("it.spanner"), is("true"));
	}

	@Test
	public void testEmulatorAutoConfigurationEnabled() {
		this.contextRunner.withPropertyValues("spring.cloud.gcp.spanner.emulator.enabled=true")
				.run(context -> {
					SpannerOptions spannerOptions = context.getBean(SpannerOptions.class);
					assertThat(spannerOptions.getEndpoint()).isEqualTo("localhost:9010");
				});
	}

	@Test
	public void testEmulatorAutoConfigurationEnabledCustomHostPort() {
		this.contextRunner.withPropertyValues("spring.cloud.gcp.spanner.emulator.enabled=true",
				"spring.cloud.gcp.spanner.emulator-host=localhost:9090")
				.run(context -> {
					SpannerOptions spannerOptions = context.getBean(SpannerOptions.class);
					assertThat(spannerOptions.getEndpoint()).isEqualTo("localhost:9090");
				});
	}

	@Test
	public void testEmulatorAutoConfigurationDisabled() {
		this.contextRunner
				.run(context -> {
					SpannerOptions spannerOptions = context.getBean(SpannerOptions.class);
					assertThat(spannerOptions.getEndpoint()).isEqualTo("spanner.googleapis.com:443");
				});
	}
}
