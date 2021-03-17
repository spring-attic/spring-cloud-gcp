/*
 * Copyright 2021-2021 the original author or authors.
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

package com.google.cloud.spring.stream.binder.pubsub.config;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.stream.binder.pubsub.provisioning.PubSubChannelProvisioner;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for PubSubBinderConfiguration and its interaction with other autoconfiguration classes.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PubSubBinderConfigurationTests {

	@Test
	public void testEverythingEnabled_implicit() {
		ApplicationContextRunner baseContext = new ApplicationContextRunner()
				// no property values specified
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class,
						PubSubBinderConfiguration.class))
				.withUserConfiguration(TestConfiguration.class);
		baseContext.run(ctx -> assertThat(ctx).hasSingleBean(PubSubChannelProvisioner.class));
	}

	@Test
	public void testEverythingEnabled_explicit() {
		ApplicationContextRunner baseContext = new ApplicationContextRunner()
				.withPropertyValues(
						// including 'core' for completeness, but it's not super relevant to the test.
						"spring.cloud.gcp.core.enabled=true",
						"spring.cloud.gcp.pubsub.enabled=true",
						"spring.cloud.gcp.pubsub.binder.enabled=true")
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class,
						PubSubBinderConfiguration.class))
				.withUserConfiguration(TestConfiguration.class);
		baseContext.run(ctx -> assertThat(ctx).hasSingleBean(PubSubChannelProvisioner.class));
	}

	@Test
	public void testBinderDisabled() {
		ApplicationContextRunner baseContext = new ApplicationContextRunner()
				.withPropertyValues(
						"spring.cloud.gcp.pubsub.enabled=true",
						"spring.cloud.gcp.pubsub.binder.enabled=false")
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class,
						PubSubBinderConfiguration.class))
				.withUserConfiguration(TestConfiguration.class);
		baseContext.run(ctx -> assertThat(ctx).doesNotHaveBean(PubSubChannelProvisioner.class));
	}

	@Test
	public void testPubSubDisabled_noReplacements() {
		ApplicationContextRunner baseContext = new ApplicationContextRunner()
				.withPropertyValues(
						"spring.cloud.gcp.pubsub.enabled=false",
						"spring.cloud.gcp.pubsub.binder.enabled=true")
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class,
						PubSubBinderConfiguration.class))
				.withUserConfiguration(TestConfiguration.class);
		baseContext.run(ctx -> assertThat(ctx).doesNotHaveBean(PubSubChannelProvisioner.class));
	}
	@Test
	public void testPubSubDisabled_withReplacements() {
		ApplicationContextRunner baseContext = new ApplicationContextRunner()
				.withPropertyValues(
						"spring.cloud.gcp.pubsub.enabled=false",
						"spring.cloud.gcp.pubsub.binder.enabled=true")
				.withBean(PubSubAdmin.class, () -> mock(PubSubAdmin.class))
				.withBean(PubSubTemplate.class, () -> mock(PubSubTemplate.class))
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class,
						PubSubBinderConfiguration.class))
				.withUserConfiguration(TestConfiguration.class);
		baseContext.run(ctx -> assertThat(ctx).hasSingleBean(PubSubChannelProvisioner.class));
	}

	@Test
	public void testBothDisabled() {
		ApplicationContextRunner baseContext = new ApplicationContextRunner()
				.withPropertyValues(
						"spring.cloud.gcp.pubsub.enabled=false",
						"spring.cloud.gcp.pubsub.binder.enabled=false")
				.withConfiguration(AutoConfigurations.of(
						GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class,
						PubSubBinderConfiguration.class))
				.withUserConfiguration(TestConfiguration.class);
		baseContext.run(ctx -> assertThat(ctx).doesNotHaveBean(PubSubChannelProvisioner.class));
	}

	private static class TestConfiguration {
		@Bean
		public CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}


		@Bean
		public GcpProjectIdProvider gcpProjectIdProvider() {
			return () -> "test-project";
		}
	}
}
