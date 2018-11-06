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

package org.springframework.cloud.gcp.stream.binder.pubsub.provisioning;

import com.google.pubsub.v1.Subscription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubChannelProvisionerTests {

	@Mock
	PubSubAdmin pubSubAdminMock;

	@Mock
	ExtendedConsumerProperties<PubSubConsumerProperties> properties;

	@Mock
	PubSubConsumerProperties pubSubConsumerProperties;

	// class under test
	PubSubChannelProvisioner pubSubChannelProvisioner;

	@Before
	public void setup() {
		Subscription subscription = Subscription.newBuilder().build();
		when(this.pubSubAdminMock.getSubscription(any())).thenReturn(null);
		when(this.pubSubAdminMock.createSubscription(any(), any())).thenReturn(subscription);
		when(this.properties.getExtension()).thenReturn(this.pubSubConsumerProperties);
		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(true);

		this.pubSubChannelProvisioner = new PubSubChannelProvisioner(this.pubSubAdminMock);
	}

	@Test
	public void testProvisionConsumerDestination_specifiedGroup() {
		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", "group_A", this.properties);

		assertThat(result.getName()).isEqualTo("topic_A.group_A");

		verify(this.pubSubAdminMock).createSubscription("topic_A.group_A", "topic_A");
	}

	@Test
	public void testProvisionConsumerDestination_anonymousGroup() {
		String subscriptionNameRegex = "anonymous\\.topic_A\\.[a-f0-9\\-]{36}";

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", null, this.properties);

		assertThat(result.getName()).matches(subscriptionNameRegex);

		verify(this.pubSubAdminMock).createSubscription(matches(subscriptionNameRegex), eq("topic_A"));
	}
}
