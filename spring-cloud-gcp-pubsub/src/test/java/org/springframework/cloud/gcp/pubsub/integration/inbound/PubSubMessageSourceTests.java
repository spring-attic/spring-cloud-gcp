/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.pubsub.integration.inbound;

import java.util.Arrays;
import java.util.Collections;

import com.google.pubsub.v1.PubsubMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.integration.endpoint.MessageSourcePollingTemplate;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHandlingException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PubSubMessageSource}.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubMessageSourceTests {

	private PubSubSubscriberOperations mockPubSubSubscriberOperations;

	@Mock
	private ConvertedAcknowledgeablePubsubMessage<String> msg1;

	@Mock
	private ConvertedAcknowledgeablePubsubMessage<String> msg2;

	@Mock
	private ConvertedAcknowledgeablePubsubMessage<String> msg3;

	@Before
	public void setUp() {
		this.mockPubSubSubscriberOperations = mock(PubSubSubscriberOperations.class);

		when(this.msg1.getPayload()).thenReturn("msg1");
		when(this.msg2.getPayload()).thenReturn("msg2");
		when(this.msg3.getPayload()).thenReturn("msg3");

		when(this.msg1.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder().build());
		when(this.msg2.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder().build());
		when(this.msg3.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder().build());

		when(this.mockPubSubSubscriberOperations.pullAndConvert("sub1", 1, true, String.class))
				.thenReturn(Collections.singletonList(this.msg1));
	}

	@Test
	public void doReceive_returnsNullWhenNoMessagesAvailable() {
		when(this.mockPubSubSubscriberOperations.pullAndConvert("sub1", 1, true, String.class))
				.thenReturn(Collections.emptyList());

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);

		Object message = pubSubMessageSource.doReceive(1);

		assertThat(message).isNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void doReceive_callsPubsubAndCachesCorrectly() {
		when(this.mockPubSubSubscriberOperations.pullAndConvert("sub1", 3, true, String.class))
				.thenReturn(Arrays.asList(this.msg1, this.msg2, this.msg3));
		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(3);
		pubSubMessageSource.setPayloadType(String.class);

		MessageBuilder<String> message1 = (MessageBuilder<String>) pubSubMessageSource.doReceive(3);
		MessageBuilder<String> message2 = (MessageBuilder<String>) pubSubMessageSource.doReceive(3);
		MessageBuilder<String> message3 = (MessageBuilder<String>) pubSubMessageSource.doReceive(3);

		assertThat(message1).isNotNull();
		assertThat(message1).isNotNull();
		assertThat(message1).isNotNull();

		assertThat(message1.getPayload()).isEqualTo("msg1");
		assertThat(message2.getPayload()).isEqualTo("msg2");
		assertThat(message3.getPayload()).isEqualTo("msg3");
		verify(this.mockPubSubSubscriberOperations, times(1))
				.pullAndConvert("sub1", 3, true, String.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void doReceive_pullsOneAtATimeWhenMaxFetchSizeZeroe() {
		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setPayloadType(String.class);

		MessageBuilder<String> message = (MessageBuilder<String>) pubSubMessageSource.doReceive(0);

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("msg1");

		verify(this.mockPubSubSubscriberOperations, times(1))
				.pullAndConvert("sub1", 1, true, String.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void doReceive_pullsOneAtATimeWhenMaxFetchSizeNegative() {
		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setPayloadType(String.class);

		MessageBuilder<String> message = (MessageBuilder<String>) pubSubMessageSource.doReceive(-1);

		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("msg1");

		verify(this.mockPubSubSubscriberOperations, times(1))
				.pullAndConvert("sub1", 1, true, String.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void doReceive_manualAckModeAppliesAcknowledgmentHeaderAndDoesNotAck() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setAckMode(AckMode.MANUAL);

		MessageBuilder<String> message = (MessageBuilder<String>) pubSubMessageSource.doReceive(1);

		assertThat(message).isNotNull();

		assertThat(message.getPayload()).isEqualTo("msg1");
		AcknowledgmentCallback callback = (AcknowledgmentCallback) message.getHeaders()
				.get(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK);
		assertThat(callback).isNotNull();
		assertThat(callback.isAcknowledged()).isFalse();
		verify(this.msg1, times(0)).ack();

		callback.acknowledge(AcknowledgmentCallback.Status.ACCEPT);
		verify(this.msg1, times(1)).ack();
		assertThat(callback.isAcknowledged()).isTrue();
	}

	@Test
	public void doReceive_autoModeAcksAndAddsOriginalMessageHeader() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setAckMode(AckMode.AUTO);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();

			assertThat(message.getPayload()).isEqualTo("msg1");
			assertThat(message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isEqualTo(this.msg1);
		});

		verify(this.msg1).ack();
	}

	@Test
	public void doReceive_autoAckModeAcksAndAddsOriginalMessageHeader() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setAckMode(AckMode.AUTO_ACK);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();

			assertThat(message.getPayload()).isEqualTo("msg1");
			assertThat(message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isEqualTo(this.msg1);
		});

		verify(this.msg1).ack();
	}

	@Test
	public void doReceive_autoAckModeIsDefault() {
		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);

		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();

			assertThat(message.getPayload()).isEqualTo("msg1");
			assertThat(message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isEqualTo(this.msg1);
		});

		verify(this.msg1).ack();
	}

	@Test
	public void doReceive_autoModeNacksAutomatically() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setAckMode(AckMode.AUTO);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		assertThatThrownBy(() -> {
			poller.poll((message) -> {
				throw new RuntimeException("Nope.");
			});
		}).isInstanceOf(MessageHandlingException.class).hasMessageContaining("Nope.");

		verify(this.msg1).nack();
	}

	@Test
	public void doReceive_autoAckModeDoesNotNackAutomatically() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);
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

		when(this.mockPubSubSubscriberOperations.pullAndConvert("sub1", 1, false, String.class))
				.thenReturn(Collections.singletonList(msg1));

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1");
		pubSubMessageSource.setMaxFetchSize(1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setBlockOnPull(true);

		MessageBuilder<String> message = (MessageBuilder<String>) pubSubMessageSource.doReceive(1);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("msg1");

		verify(this.mockPubSubSubscriberOperations)
				.pullAndConvert("sub1", 1, false, String.class);
	}

}
