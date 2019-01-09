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

import com.google.pubsub.v1.PubsubMessage;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.integration.endpoint.MessageSourcePollingTemplate;
import org.springframework.messaging.Message;
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
 * @since 1.1
 */
public class PubSubMessageSourceTests {

	private PubSubSubscriberOperations mockPubSubSubscriberOperations;

	private ConvertedAcknowledgeablePubsubMessage<String> msg1;

	private ConvertedAcknowledgeablePubsubMessage<String> msg2;

	private ConvertedAcknowledgeablePubsubMessage<String> msg3;

	@Before
	public void setUp() {
		this.mockPubSubSubscriberOperations = mock(PubSubSubscriberOperations.class);
		this.msg1 = mock(ConvertedAcknowledgeablePubsubMessage.class);
		this.msg2 = mock(ConvertedAcknowledgeablePubsubMessage.class);
		this.msg3 = mock(ConvertedAcknowledgeablePubsubMessage.class);

		when(this.msg1.getPayload()).thenReturn("msg1");
		when(this.msg2.getPayload()).thenReturn("msg2");
		when(this.msg3.getPayload()).thenReturn("msg3");

		when(this.msg1.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder().build());
		when(this.msg2.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder().build());
		when(this.msg3.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder().build());

		when(this.mockPubSubSubscriberOperations.pullAndConvert("sub1", 1, true, String.class)).thenReturn(Arrays.asList(this.msg1));
	}

	@Test
	public void doReceive_returnsNullWhenNoMessagesAvailable() {
		when(this.mockPubSubSubscriberOperations.pullAndConvert("sub1", 1, true, String.class))
				.thenReturn(Arrays.asList());

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1", 1);
		pubSubMessageSource.setPayloadType(String.class);

		Object message = pubSubMessageSource.doReceive(1);

		assertThat(message).isNull();
	}

	@Test
	public void doReceive_callsPubsubAndCachesCorrectly() {
		when(this.mockPubSubSubscriberOperations.pullAndConvert("sub1", 3, true, String.class))
				.thenReturn(Arrays.asList(this.msg1, this.msg2, this.msg3));
		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1", 3);
		pubSubMessageSource.setPayloadType(String.class);

		Message<String> message1 = (Message<String>) pubSubMessageSource.doReceive(3);
		Message<String> message2 = (Message<String>) pubSubMessageSource.doReceive(3);
		Message<String> message3 = (Message<String>) pubSubMessageSource.doReceive(3);

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
	public void doReceive_manualAckModeAppliesOriginalMessageHeaderAndDoesNotAck() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1", 1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setAckMode(AckMode.MANUAL);

		Message<String> message1 = (Message<String>) pubSubMessageSource.doReceive(1);

		assertThat(message1).isNotNull();

		assertThat(message1.getPayload()).isEqualTo("msg1");
		assertThat(message1.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isSameAs(this.msg1);
		verify(this.msg1, times(0)).ack();
	}

	@Test
	public void doReceive_autoModeAcksWithoutAddingOriginalMessageHeader() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1", 1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setAckMode(AckMode.AUTO);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();

			assertThat(message.getPayload()).isEqualTo("msg1");
			assertThat(message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isNull();
		});

		verify(this.msg1).ack();
	}

	@Test
	public void doReceive_autoAckModeAcksWithoutAddingOriginalMessageHeader() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1", 1);
		pubSubMessageSource.setPayloadType(String.class);
		pubSubMessageSource.setAckMode(AckMode.AUTO_ACK);
		MessageSourcePollingTemplate poller = new MessageSourcePollingTemplate(pubSubMessageSource);
		poller.poll((message) -> {
			assertThat(message).isNotNull();

			assertThat(message.getPayload()).isEqualTo("msg1");
			assertThat(message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isNull();
		});

		verify(this.msg1).ack();
	}


	@Test
	public void doReceive_autoModeNacksAutomatically() {

		PubSubMessageSource pubSubMessageSource = new PubSubMessageSource(
				this.mockPubSubSubscriberOperations, "sub1", 1);
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
				this.mockPubSubSubscriberOperations, "sub1", 1);
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
}
