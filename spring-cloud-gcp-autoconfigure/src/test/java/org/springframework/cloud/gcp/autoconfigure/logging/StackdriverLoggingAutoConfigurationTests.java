/*
 *  Copyright 2017-2018 original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.logging;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.trace.StackdriverTraceAutoConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Mike Eltsufin
 * @author João André Martins
 * @author Chengyuan Zhao
 */
public class StackdriverLoggingAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner =
			new WebApplicationContextRunner()
					.withConfiguration(
							AutoConfigurations.of(
									StackdriverLoggingAutoConfiguration.class,
									GcpContextAutoConfiguration.class));

	@Test
	public void testDisabledConfiguration() {
		this.contextRunner.withPropertyValues("spring.cloud.gcp.logging.enabled=false")
				.run(context -> assertThat(context
						.getBeansOfType(TraceIdLoggingWebMvcInterceptor.class).size())
								.isEqualTo(0));
	}

	@Test
	public void testNonWebAppConfiguration() {
		new ApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(
						StackdriverLoggingAutoConfiguration.class,
						GcpContextAutoConfiguration.class))
				.run(context -> assertThat(context
						.getBeansOfType(TraceIdLoggingWebMvcInterceptor.class).size())
								.isEqualTo(0));
	}

	@Test
	public void testNonServletConfiguration() {
		new ReactiveWebApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(
						StackdriverLoggingAutoConfiguration.class,
						GcpContextAutoConfiguration.class))
				.run(context -> assertThat(context
						.getBeansOfType(TraceIdLoggingWebMvcInterceptor.class).size())
						.isEqualTo(0));
	}

	@Test
	public void testRegularConfiguration() {
		this.contextRunner.run(context -> assertThat(
				context.getBeansOfType(TraceIdLoggingWebMvcInterceptor.class).size())
						.isEqualTo(1));
	}

	@Test
	public void testWithSleuth() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(Configuration.class,
						StackdriverTraceAutoConfiguration.class))
				.withPropertyValues("spring.cloud.gcp.project-id=pop-1")
				.run(context -> assertThat(context
						.getBeansOfType(TraceIdLoggingWebMvcInterceptor.class).size())
								.isEqualTo(0));
	}

	private static class Configuration {

		@Bean
		public CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

	}

}
