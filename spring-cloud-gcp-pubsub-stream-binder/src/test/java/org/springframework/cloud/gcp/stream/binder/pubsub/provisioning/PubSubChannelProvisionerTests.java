/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.stream.binder.pubsub.provisioning;

import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.stream.binder.pubsub.properties.PubSubConsumerProperties;
import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Before
	public void setup() {
		when(this.pubSubAdminMock.getSubscription(any())).thenReturn(null);
		doAnswer((invocation) ->
			Subscription.newBuilder()
					.setName("projects/test-project/subscriptions/" + invocation.getArgument(0))
					.setTopic("projects/test-project/topics/" + invocation.getArgument(1)).build()
		).when(this.pubSubAdminMock).createSubscription(any(), any());
		doAnswer((invocation) ->
				Topic.newBuilder().setName("projects/test-project/topics/" + invocation.getArgument(0)).build()
		).when(this.pubSubAdminMock).getTopic(any());
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
	public void testProvisionConsumerDestination_noTopicException() {
		this.expectedEx.expect(ProvisioningException.class);
		this.expectedEx.expectMessage("Non-existing 'topic_A' topic.");

		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);
		when(this.pubSubAdminMock.getTopic("topic_A")).thenReturn(null);

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", "group_A", this.properties);
	}

	@Test
	public void testProvisionConsumerDestination_noSubscriptionException() {
		this.expectedEx.expect(ProvisioningException.class);
		this.expectedEx.expectMessage("Non-existing 'topic_A.group_A' subscription.");

		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", "group_A", this.properties);
	}

	@Test
	public void testProvisionConsumerDestination_wrongTopicException() {
		this.expectedEx.expect(ProvisioningException.class);
		this.expectedEx.expectMessage("Existing 'topic_A.group_A' subscription is for a different topic 'topic_B'.");

		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);
		when(this.pubSubAdminMock.getSubscription("topic_A.group_A")).thenReturn(Subscription.newBuilder().setTopic("topic_B").build());

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", "group_A", this.properties);
	}

	@Test
	public void testProvisionConsumerDestination_anonymousGroup() {
		// should work with auto-create = false
		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);

		String subscriptionNameRegex = "anonymous\\.topic_A\\.[a-f0-9\\-]{36}";

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", null, this.properties);

		assertThat(result.getName()).matches(subscriptionNameRegex);

		verify(this.pubSubAdminMock).createSubscription(matches(subscriptionNameRegex), eq("topic_A"));
	}

	@Test
	public void testAfterUnbindConsumer_anonymousGroup() {
		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", null, this.properties);

		this.pubSubChannelProvisioner.afterUnbindConsumer(result);

		verify(this.pubSubAdminMock).deleteSubscription(result.getName());
	}

	@Test
	public void testAfterUnbindConsumer_twice() {
		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", null, this.properties);

		this.pubSubChannelProvisioner.afterUnbindConsumer(result);
		this.pubSubChannelProvisioner.afterUnbindConsumer(result);

		verify(this.pubSubAdminMock, times(1)).deleteSubscription(result.getName());
	}

	@Test
	public void testAfterUnbindConsumer_nonAnonymous() {
		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", "group1", this.properties);

		this.pubSubChannelProvisioner.afterUnbindConsumer(result);

		verify(this.pubSubAdminMock, never()).deleteSubscription(result.getName());
	}
}
