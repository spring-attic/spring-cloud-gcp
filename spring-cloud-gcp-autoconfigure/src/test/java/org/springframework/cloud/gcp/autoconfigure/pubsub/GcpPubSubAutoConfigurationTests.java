/*
 * Copyright 2019-2020 the original author or authors.
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
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for Pub/Sub autoconfiguration.
 *
 * @author Elena Felder
 * @author Mike Eltsufin
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

			TransportChannelProvider subscriberTcp = ctx.getBean("subscriberTransportChannelProvider", TransportChannelProvider.class);
			TransportChannelProvider publisherTcp = ctx.getBean("publisherTransportChannelProvider", TransportChannelProvider.class);
			assertThat(((InstantiatingGrpcChannelProvider) subscriberTcp).getKeepAliveTime().toMinutes())
					.isEqualTo(5);
			assertThat(((InstantiatingGrpcChannelProvider) publisherTcp).getKeepAliveTime().toMinutes())
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

			TransportChannelProvider subscriberTcp = ctx.getBean("subscriberTransportChannelProvider", TransportChannelProvider.class);
			TransportChannelProvider publisherTcp = ctx.getBean("publisherTransportChannelProvider", TransportChannelProvider.class);
			assertThat(((InstantiatingGrpcChannelProvider) subscriberTcp).getKeepAliveTime().toMinutes())
					.isEqualTo(2);
			assertThat(((InstantiatingGrpcChannelProvider) publisherTcp).getKeepAliveTime().toMinutes())
					.isEqualTo(2);
		});
	}

	@Test
	public void maxInboundMessageSize_default() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(GcpPubSubAutoConfiguration.class))
				.withUserConfiguration(TestConfig.class);

		contextRunner.run(ctx -> {

			TransportChannelProvider subscriberTcp = ctx.getBean("subscriberTransportChannelProvider", TransportChannelProvider.class);
			assertThat(FieldUtils.readField(subscriberTcp, "maxInboundMessageSize", true))
					.isEqualTo(20 << 20);

			TransportChannelProvider publisherTcp = ctx.getBean("publisherTransportChannelProvider", TransportChannelProvider.class);
			assertThat(FieldUtils.readField(publisherTcp, "maxInboundMessageSize", true))
					.isEqualTo(Integer.MAX_VALUE);
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
