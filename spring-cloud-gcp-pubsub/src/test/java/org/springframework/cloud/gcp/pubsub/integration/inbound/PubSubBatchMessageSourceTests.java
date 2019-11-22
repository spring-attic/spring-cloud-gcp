/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.pubsub.integration.inbound;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.endpoint.MessageSourcePollingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PubSubBatchMessageSource}.
 *
 * @author Eric Ngeo
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubBatchMessageSourceTests {

	private PubSubSubscriberOperations mockPubSubSubscriberOperations;

	@Mock
	private AcknowledgeablePubsubMessage msg1;

	@Mock
	private AcknowledgeablePubsubMessage msg2;

	@Mock
	private AcknowledgeablePubsubMessage msg3;

	@Before
	public void setUp() {
		this.mockPubSubSubscriberOperations = mock(PubSubSubscriberOperations.class);

		when(this.mockPubSubSubscriberOperations.pull("sub1", 1, true))
				.thenReturn(Collections.singletonList(this.msg1));
	}

	@Test
	public void doReceive_returnsNullWhenNoMessagesAvailable() {
		when(this.mockPubSubSubscriberOperations.pull("sub1", 1, true))
				.thenReturn(Collections.emptyList());

		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);

		Object message = pubSubMessageSource.doReceive(1);

		assertThat(message).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void doReceive_callsPubsubAndBatchesCorrectly() {
		when(this.mockPubSubSubscriberOperations.pull("sub1", 3, true))
				.thenReturn(Arrays.asList(this.msg1, this.msg2, this.msg3));
		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(3);
		Message<Object> message = pubSubMessageSource.receive();
		List<AcknowledgeablePubsubMessage> payload = (List<AcknowledgeablePubsubMessage>) message.getPayload();
		assertThat(payload.size()).isEqualTo(3);
		assertThat(payload.containsAll(Arrays.asList(msg1, msg2, msg3))).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void doReceive_pullsOneAtATimeWhenMaxFetchSizeNegative() {
		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");

		MessageBuilder<AcknowledgeablePubsubMessage> message = (MessageBuilder<AcknowledgeablePubsubMessage>) pubSubMessageSource
				.doReceive(-1);

		assertThat(message).isNotNull();

		verify(this.mockPubSubSubscriberOperations, times(1))
				.pull("sub1", 1, true);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void doReceive_manualAckModeDoesNotAck() {

		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setAckMode(AckMode.MANUAL);

		Message<Object> message = pubSubMessageSource.receive();
		assertThat(message).isNotNull();
		List<AcknowledgeablePubsubMessage> payload = (List<AcknowledgeablePubsubMessage>) message.getPayload();

		assertThat(payload).containsOnly(msg1);

		PubSubBatchAcknowledgmentCallback callback = (PubSubBatchAcknowledgmentCallback) message.getHeaders()
				.get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK);
		assertThat(callback).isNotNull();
		assertThat(callback.isAcknowledged()).isFalse();
		verify(this.mockPubSubSubscriberOperations, never()).ack(Mockito.any());

		callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		verify(this.mockPubSubSubscriberOperations).ack(Mockito.any());
		assertThat(callback.isAcknowledged()).isTrue();
	}

	@Test
	public void doReceive_autoModeAcks() {

		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setAckMode(AckMode.AUTO);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();
			List<AcknowledgeablePubsubMessage> payload = (List<AcknowledgeablePubsubMessage>) message.getPayload();
			assertThat(payload).containsOnly(msg1);
			assertThat(message.getHeaders()).doesNotContainKey(GcpPubSubHeaders.ORIGINAL_MESSAGE);
		});
		verify(this.mockPubSubSubscriberOperations).ack(Mockito.any());
	}

	@Test
	public void doReceive_autoAckModeAcks() {

		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setAckMode(AckMode.AUTO_ACK);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();
			List<AcknowledgeablePubsubMessage> payload = (List<AcknowledgeablePubsubMessage>) message.getPayload();
			assertThat(payload).containsOnly(msg1);
		});

		verify(this.mockPubSubSubscriberOperations).ack(Mockito.anyList());
	}

	@Test
	public void doReceive_autoAckModeIsDefault() {
		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);

		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();
			List<AcknowledgeablePubsubMessage> payload = (List<AcknowledgeablePubsubMessage>) message.getPayload();
			assertThat(payload).containsOnly(msg1);
		});
		verify(this.mockPubSubSubscriberOperations).ack(Mockito.any());
	}

	@Test
	public void doReceive_autoModeNacksAutomatically() {

		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setAckMode(AckMode.AUTO);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		assertThatThrownBy(() -> {
			poller.poll((message) -> {
				throw new RuntimeException("Nope.");
			});
		}).isInstanceOf(MessageHandlingException.class).hasMessageContaining("Nope.");

		verify(this.mockPubSubSubscriberOperations).nack(Mockito.any());
	}

	@Test
	public void doReceive_autoAckModeDoesNotNackAutomatically() {

		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setAckMode(AckMode.AUTO_ACK);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		assertThatThrownBy(() -> {
			poller.poll((message) -> {
				throw new RuntimeException("Nope.");
			});
		}).isInstanceOf(MessageHandlingException.class).hasMessageContaining("Nope.");

		verify(this.msg1, times(0)).nack();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void blockOnPullSetsReturnImmediatelyToFalse() {

		when(this.mockPubSubSubscriberOperations.pull("sub1", 1, false))
				.thenReturn(Collections.singletonList(msg1));

		PubSubBatchMessageSource pubSubMessageSource = new PubSubBatchMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setBlockOnPull(true);

		Message<Object> message = pubSubMessageSource.receive();
		assertThat(message).isNotNull();
		List<AcknowledgeablePubsubMessage> payload = (List<AcknowledgeablePubsubMessage>) message.getPayload();
		assertThat(payload).containsOnly(msg1);

		verify(this.mockPubSubSubscriberOperations)
				.pull("sub1", 1, false);
	}

}
