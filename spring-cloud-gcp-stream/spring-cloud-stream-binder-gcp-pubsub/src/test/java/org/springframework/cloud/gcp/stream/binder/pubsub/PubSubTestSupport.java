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

package org.springframework.cloud.gcp.stream.binder.pubsub;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.grpc.FixedChannelProvider;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.util.StringUtils;

/**
 * @author Andreas Berger
 */
public class PubSubTestSupport extends AbstractExternalResourceTestSupport<PubSubSupport> {
	protected PubSubTestSupport() {
		super("PUBSUB");
	}

	@Override
	protected void cleanupResource() throws Exception {

	}

	@Override
	protected void obtainResource() throws Exception {
		String emulatorHost = System.getenv("PUBSUB_EMULATOR_HOST");
		if (!StringUtils.hasText(emulatorHost)) {
			emulatorHost = System.getProperty("PUBSUB_EMULATOR_HOST");
		}
		TopicAdminSettings.Builder topicBuilder = TopicAdminSettings.newBuilder();
		SubscriptionAdminSettings.Builder subscriptionBuilder = SubscriptionAdminSettings.newBuilder();
		FixedCredentialsProvider credentialsProvider = null;
		FixedChannelProvider channelProvider = null;
		if (StringUtils.hasText(emulatorHost)) {

			ManagedChannel channel = ManagedChannelBuilder
					// .forAddress("localhost",8085)
					.forTarget(emulatorHost)
					.usePlaintext(true)
					.build();

			credentialsProvider = FixedCredentialsProvider.create(new OAuth2Credentials(null) {
				@Override
				public Map<String, List<String>> getRequestMetadata(URI uri) throws IOException {
					return null;
				}
			});
			channelProvider = FixedChannelProvider.create(channel);
			topicBuilder.setCredentialsProvider(credentialsProvider);
			topicBuilder.setTransportProvider(TopicAdminSettings
					.defaultGrpcTransportProviderBuilder()
					.setChannelProvider(channelProvider)
					.build());

			subscriptionBuilder.setCredentialsProvider(credentialsProvider);
			subscriptionBuilder.setTransportProvider(SubscriptionAdminSettings
					.defaultGrpcTransportProviderBuilder()
					.setChannelProvider(channelProvider)
					.build());
		}
		resource = new PubSubSupport()
				.setSubscriptionAdminClient(SubscriptionAdminClient.create(subscriptionBuilder.build()))
				.setTopicAdminClient(TopicAdminClient.create(topicBuilder.build()))
				.setCredentialsProvider(credentialsProvider)
				.setChannelProvider(channelProvider);
	}
}
