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

package org.springframework.cloud.gcp.pubsub.support;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.pubsub.spi.v1.MessageReceiver;
import com.google.cloud.pubsub.spi.v1.Subscriber;
import com.google.cloud.pubsub.spi.v1.SubscriptionAdminSettings;
import com.google.pubsub.v1.SubscriptionName;

/**
 *
 * The default {@link SubscriberFactory} implementation.
 *
 * @author João André Martins
 */
public class DefaultSubscriberFactory implements SubscriberFactory {

	private String projectId;

	private GoogleCredentials credentials;

	public DefaultSubscriberFactory(String projectId, GoogleCredentials credentials) {
		this.projectId = projectId;
		this.credentials = credentials;
	}

	@Override
	public Subscriber getSubscriber(String subscriptionName, MessageReceiver receiver) {
		return Subscriber.defaultBuilder(
				SubscriptionName.create(this.projectId, subscriptionName), receiver)
				.setChannelProvider(
						SubscriptionAdminSettings.defaultChannelProviderBuilder()
								.setCredentialsProvider(() -> this.credentials)
								.build())
				.build();
	}
}
