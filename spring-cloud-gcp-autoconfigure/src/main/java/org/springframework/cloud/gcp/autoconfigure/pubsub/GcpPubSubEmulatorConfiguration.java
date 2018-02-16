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

package org.springframework.cloud.gcp.autoconfigure.pubsub;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * If <code>spring.cloud.gcp.pubsub.emulatorHost</code> is set, spring stream will connect
 * to a running pub/sub emulator.
 *
 * @author Andreas Berger
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.cloud.gcp.pubsub", name = "emulatorHost")
@AutoConfigureBefore(GcpContextAutoConfiguration.class)
@EnableConfigurationProperties(GcpPubSubProperties.class)
public class GcpPubSubEmulatorConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TransportChannelProvider transportChannelProvider(GcpPubSubProperties gcpPubSubProperties) {
		ManagedChannel channel = ManagedChannelBuilder
				.forTarget(gcpPubSubProperties.getEmulatorHost())
				.usePlaintext(true)
				.build();
		return FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
	}

	@Bean
	@ConditionalOnMissingBean
	public CredentialsProvider credentialsProvider() {
		return NoCredentialsProvider.create();
	}
}
