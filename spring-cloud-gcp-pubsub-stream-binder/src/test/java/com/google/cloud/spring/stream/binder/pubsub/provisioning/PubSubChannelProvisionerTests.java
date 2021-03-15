/*
 * Copyright 2017-2018 the original author or authors.
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

package com.google.cloud.spring.stream.binder.pubsub.provisioning;

import com.google.api.gax.rpc.AlreadyExistsException;
import com.google.cloud.spring.pubsub.PubSubAdmin;
import com.google.cloud.spring.pubsub.support.PubSubSubscriptionUtils;
import com.google.cloud.spring.pubsub.support.PubSubTopicUtils;
import com.google.cloud.spring.stream.binder.pubsub.properties.PubSubConsumerProperties;
import com.google.pubsub.v1.DeadLetterPolicy;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.stream.binder.ExtendedConsumerProperties;
import org.springframework.cloud.stream.provisioning.ProvisioningException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for Pub/Sub provisioner.
 *
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
		when(this.pubSubAdminMock.getSubscription(any())).thenReturn(null);
		doAnswer(invocation -> {
			Subscription.Builder arg = invocation.getArgument(0, Subscription.Builder.class);
			return Subscription.newBuilder()
					.setName(PubSubSubscriptionUtils.toProjectSubscriptionName(arg.getName(), "test-project").toString())
					.setTopic(PubSubTopicUtils.toTopicName(arg.getTopic(), "test-project").toString())
					.build();
		}).when(this.pubSubAdminMock).createSubscription(any());
		doAnswer(invocation ->
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

		ArgumentCaptor<Subscription.Builder> argCaptor = ArgumentCaptor.forClass(Subscription.Builder.class);
		verify(this.pubSubAdminMock).createSubscription(argCaptor.capture());
		assertThat(argCaptor.getValue().getName()).isEqualTo("topic_A.group_A");
		assertThat(argCaptor.getValue().getTopic()).isEqualTo("topic_A");
	}

	@Test
	public void testProvisionConsumerDestination_specifiedGroupTopicInDifferentProject() {
		String fullTopicName = "projects/differentProject/topics/topic_A";
		when(this.pubSubAdminMock.getTopic(fullTopicName)).thenReturn(
				Topic.newBuilder().setName(fullTopicName).build());

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination(fullTopicName, "group_A", this.properties);

		assertThat(result.getName()).isEqualTo("topic_A.group_A");

		ArgumentCaptor<Subscription.Builder> argCaptor = ArgumentCaptor.forClass(Subscription.Builder.class);
		verify(this.pubSubAdminMock).createSubscription(argCaptor.capture());
		assertThat(argCaptor.getValue().getName()).isEqualTo("topic_A.group_A");
		assertThat(argCaptor.getValue().getTopic()).isEqualTo("projects/differentProject/topics/topic_A");
	}

	@Test
	public void testProvisionConsumerDestination_customSubscription() {
		when(this.properties.getExtension()).thenReturn(this.pubSubConsumerProperties);
		when(this.pubSubConsumerProperties.getSubscriptionName()).thenReturn("my-custom-subscription");

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", "group_A", this.properties);

		assertThat(result.getName()).isEqualTo("my-custom-subscription");
	}

	@Test
	public void testProvisionConsumerDestination_noTopicException() {
		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);
		when(this.pubSubAdminMock.getTopic("topic_A")).thenReturn(null);

		assertThatExceptionOfType(ProvisioningException.class)
				.isThrownBy(() -> this.pubSubChannelProvisioner
						.provisionConsumerDestination("topic_A", "group_A", this.properties))
				.withMessage("Non-existing 'topic_A' topic.");
	}

	@Test
	public void testProvisionConsumerDestination_noSubscriptionException() {
		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);

		assertThatExceptionOfType(ProvisioningException.class)
				.isThrownBy(() -> this.pubSubChannelProvisioner
						.provisionConsumerDestination("topic_A", "group_A", this.properties))
				.withMessage("Non-existing 'topic_A.group_A' subscription.");
	}

	@Test
	public void testProvisionConsumerDestination_wrongTopicException() {
		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);
		when(this.pubSubAdminMock.getSubscription("topic_A.group_A")).thenReturn(
				Subscription.newBuilder().setTopic("topic_B").build());

		assertThatExceptionOfType(ProvisioningException.class)
				.isThrownBy(() -> this.pubSubChannelProvisioner
						.provisionConsumerDestination("topic_A", "group_A", this.properties))
				.withMessage("Existing 'topic_A.group_A' subscription is for a different topic 'topic_B'.");
	}

	@Test
	public void testProvisionConsumerDestination_anonymousGroup() {
		// should work with auto-create = false
		when(this.pubSubConsumerProperties.isAutoCreateResources()).thenReturn(false);

		String subscriptionNameRegex = "anonymous\\.topic_A\\.[a-f0-9\\-]{36}";

		PubSubConsumerDestination result = (PubSubConsumerDestination) this.pubSubChannelProvisioner
				.provisionConsumerDestination("topic_A", null, this.properties);

		assertThat(result.getName()).matches(subscriptionNameRegex);

		ArgumentCaptor<Subscription.Builder> argCaptor = ArgumentCaptor.forClass(Subscription.Builder.class);
		verify(this.pubSubAdminMock).createSubscription(argCaptor.capture());
		assertThat(argCaptor.getValue().getName()).matches(subscriptionNameRegex);
		assertThat(argCaptor.getValue().getTopic()).isEqualTo("topic_A");
	}

	@Test
	public void testProvisionConsumerDestination_deadLetterQueue() {
		PubSubConsumerProperties.DeadLetterPolicy dlp = new PubSubConsumerProperties.DeadLetterPolicy();
		dlp.setDeadLetterTopic("deadLetterTopic");
		dlp.setMaxDeliveryAttempts(12);
		when(this.pubSubConsumerProperties.getDeadLetterPolicy()).thenReturn(dlp);

		when(this.pubSubAdminMock.getTopic("deadLetterTopic")).thenReturn(null);
		when(this.pubSubAdminMock.createTopic("deadLetterTopic"))
				.thenReturn(Topic.newBuilder().setName("projects/test-project/topics/deadLetterTopic").build());

		this.pubSubChannelProvisioner.provisionConsumerDestination("topic_A", "group_A", this.properties);

		ArgumentCaptor<Subscription.Builder> argCaptor = ArgumentCaptor.forClass(Subscription.Builder.class);
		verify(this.pubSubAdminMock).createSubscription(argCaptor.capture());
		Subscription.Builder sb = argCaptor.getValue();
		assertThat(sb.getName()).isEqualTo("topic_A.group_A");
		assertThat(sb.getTopic()).isEqualTo("topic_A");
		assertThat(sb.getDeadLetterPolicy()).isNotNull();
		DeadLetterPolicy policy = sb.getDeadLetterPolicy();
		assertThat(policy.getDeadLetterTopic()).isEqualTo("projects/test-project/topics/deadLetterTopic");
		assertThat(policy.getMaxDeliveryAttempts()).isEqualTo(12);

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

	@Test
	public void testProvisionConsumerDestination_concurrentTopicCreation() {
		when(this.pubSubAdminMock.createTopic(any())).thenThrow(AlreadyExistsException.class);
		when(this.pubSubAdminMock.getTopic("already_existing_topic"))
				.thenReturn(null)
				.thenReturn(Topic.newBuilder().setName("already_existing_topic").build());

		// Ensure no exceptions occur if topic already exists on create call
		assertThat(this.pubSubChannelProvisioner
				.provisionConsumerDestination("already_existing_topic", "group1", this.properties)).isNotNull();
	}

	@Test
	public void testProvisionConsumerDestination_recursiveExistCalls() {
		when(this.pubSubAdminMock.getTopic("new_topic")).thenReturn(null);
		when(this.pubSubAdminMock.createTopic(any())).thenThrow(AlreadyExistsException.class);

		// Ensure no infinite loop on recursive call
		assertThatExceptionOfType(ProvisioningException.class)
				.isThrownBy(() -> this.pubSubChannelProvisioner.ensureTopicExists("new_topic", true));
	}
}
