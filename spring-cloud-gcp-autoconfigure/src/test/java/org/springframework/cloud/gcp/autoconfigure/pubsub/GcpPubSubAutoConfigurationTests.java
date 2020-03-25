/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.auth.Credentials;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.pubsub.health.PubSubHealthIndicator;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for Pub/Sub autoconfiguration.
 *
 * @author Elena Felder
 */
public class GcpPubSubAutoConfigurationTests {

	@Test
	public void keepAliveValue_default() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpPubSubAutoConfiguration.class))
				.withUserConfiguration(TestConfig.class);

		contextRunner.run(ctx -> {
			GcpPubSubProperties props = ctx.getBean(GcpPubSubProperties.class);
			assertThat(props.getKeepAliveIntervalMinutes()).isEqualTo(5);

			TransportChannelProvider tcp = ctx.getBean(TransportChannelProvider.class);
			assertThat(((InstantiatingGrpcChannelProvider) tcp).getKeepAliveTime().toMinutes())
					.isEqualTo(5);
		});
	}

	@Test
	public void keepAliveValue_custom() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpPubSubAutoConfiguration.class))
				.withUserConfiguration(TestConfig.class)
				.withPropertyValues("spring.cloud.gcp.pubsub.keepAliveIntervalMinutes=2");

		contextRunner.run(ctx -> {
			GcpPubSubProperties props = ctx.getBean(GcpPubSubProperties.class);
			assertThat(props.getKeepAliveIntervalMinutes()).isEqualTo(2);

			TransportChannelProvider tcp = ctx.getBean(TransportChannelProvider.class);
			assertThat(((InstantiatingGrpcChannelProvider) tcp).getKeepAliveTime().toMinutes())
					.isEqualTo(2);
		});
	}

	@Test
	public void healthIndicatorPresent() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpPubSubAutoConfiguration.class))
				.withUserConfiguration(TestConfig.class)
				.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
						"management.health.pubsub.enabled=true");
		contextRunner.run(ctx -> {
			PubSubHealthIndicator healthIndicator = ctx.getBean(PubSubHealthIndicator.class);
			assertThat(healthIndicator).isNotNull();
		});
	}

	@Test
	public void healthIndicatorDisabledWhenPubSubTurnedOff() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpPubSubAutoConfiguration.class))
				.withUserConfiguration(TestConfig.class)
				.withPropertyValues("spring.cloud.gcp.datastore.project-id=test-project",
						"management.health.pubsub.enabled=true",
						"spring.cloud.gcp.pubsub.enabled=false");
		contextRunner.run(ctx -> {
			assertThat(ctx.getBeansOfType(PubSubHealthIndicator.class)).isEmpty();
		});
	}

	static class TestConfig {

		@Bean
		public GcpProjectIdProvider projectIdProvider() {
			return () -> "fake project";
		}

		@Bean
		public CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

	}
}
