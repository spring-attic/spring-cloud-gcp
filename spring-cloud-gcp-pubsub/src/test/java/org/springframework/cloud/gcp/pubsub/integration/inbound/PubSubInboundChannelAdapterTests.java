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

import java.util.function.Consumer;

import com.google.pubsub.v1.PubsubMessage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.integration.support.MutableMessageBuilder;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PubSubInboundChannelAdapter} unit tests.
 *
 * @author João André Martins
 * @author Doug Hoard
 * @author Mike Eltsufin
 * @author Taylor Burke
 * @author Chengyuan Zhao
 * @author Elena Felder
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubInboundChannelAdapterTests {

	PubSubInboundChannelAdapter adapter;

	static final String EXCEPTION_MESSAGE = "Simulated downstream message processing failure";

	/** Output capture for validating warning messages. */
	@Rule
	public OutputCaptureRule output = new OutputCaptureRule();

	@Mock
	PubSubSubscriberOperations mockPubSubSubscriberOperations;

	@Mock
	MessageChannel mockMessageChannel;

	@Mock
	ConvertedBasicAcknowledgeablePubsubMessage mockAcknowledgeableMessage;

	@Before
	@SuppressWarnings("unchecked")
	public void setUp() {

		this.adapter = new PubSubInboundChannelAdapter(
				this.mockPubSubSubscriberOperations, "testSubscription");
		this.adapter.setOutputChannel(this.mockMessageChannel);

		when(this.mockMessageChannel.send(any())).thenReturn(true);

		when(mockAcknowledgeableMessage.getPubsubMessage()).thenReturn(PubsubMessage.newBuilder().build());
		when(mockAcknowledgeableMessage.getPayload()).thenReturn("Test message payload.");

		when(this.mockPubSubSubscriberOperations.subscribeAndConvert(
				anyString(), any(Consumer.class), any(Class.class))).then((invocationOnMock) -> {
					Consumer<ConvertedBasicAcknowledgeablePubsubMessage> messageConsumer =
							invocationOnMock.getArgument(1);
					messageConsumer.accept(mockAcknowledgeableMessage);
				return null;
		});
	}

	@Test
	public void testNonNullAckMode() {

		assertThatThrownBy(() -> {
			this.adapter.setAckMode(null);
		}).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("The acknowledgement mode can't be null.");
	}

	@Test
	public void testAckModeAuto_nacksWhenDownstreamProcessingFails()  {

		when(this.mockMessageChannel.send(any())).thenThrow(new RuntimeException(EXCEPTION_MESSAGE));

		this.adapter.setAckMode(AckMode.AUTO);
		this.adapter.setOutputChannel(this.mockMessageChannel);

		this.adapter.start();

		verify(mockAcknowledgeableMessage).nack();

		assertThat(output.getOut()).contains("failed; message nacked automatically");
		assertThat(output.getOut()).contains(EXCEPTION_MESSAGE);
	}

	@Test
	public void testAckModeAutoAck_neitherAcksNorNacksWhenMessageProcessingFails() {

		when(this.mockMessageChannel.send(any())).thenThrow(new RuntimeException(EXCEPTION_MESSAGE));

		this.adapter.setAckMode(AckMode.AUTO_ACK);

		this.adapter.start();

		// When exception thrown, verify that neither ack() nor nack() is called.
		verify(mockAcknowledgeableMessage, times(0)).ack();
		verify(mockAcknowledgeableMessage, times(0)).nack();

		assertThat(output.getOut()).contains("failed; message neither acked nor nacked");
		assertThat(output.getOut()).contains(EXCEPTION_MESSAGE);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void customMessageBuilderFactoryUsedWhenAvailable() {

		MutableMessageBuilderFactory factory = mock(MutableMessageBuilderFactory.class);
		when(factory.withPayload(any())).thenReturn(MutableMessageBuilder.withPayload("custom payload"));

		this.adapter.setMessageBuilderFactory(factory);

		this.adapter.start();

		verify(factory, times(1)).withPayload(any());
		ArgumentCaptor<Message<String>> argument = ArgumentCaptor.forClass(Message.class);
		verify(this.mockMessageChannel).send(argument.capture());
		assertThat(argument.getValue().getPayload()).isEqualTo("custom payload");
	}

	@Test
	public void consumeMessageAttachesOriginalMessageHeaderInManualMode() {
		this.adapter.setAckMode(AckMode.MANUAL);
		this.adapter.start();

		verifyOriginalMessage();
	}

	@Test
	public void consumeMessageAttachesOriginalMessageHeaderInAutoMode() {
		this.adapter.setAckMode(AckMode.AUTO);
		this.adapter.start();

		verifyOriginalMessage();
	}

	@Test
	public void consumeMessageAttachesOriginalMessageHeaderInAutoAckMode() {
		this.adapter.setAckMode(AckMode.AUTO_ACK);
		this.adapter.start();

		verifyOriginalMessage();
	}

	@SuppressWarnings("unchecked")
	private void verifyOriginalMessage() {
		ArgumentCaptor<Message<?>> argument = ArgumentCaptor.forClass(Message.class);
		verify(this.mockMessageChannel).send(argument.capture());
		MessageHeaders headers = argument.getValue().getHeaders();
		assertThat(headers).containsKey(GcpPubSubHeaders.ORIGINAL_MESSAGE);
		assertThat(headers.get(GcpPubSubHeaders.ORIGINAL_MESSAGE)).isEqualTo(mockAcknowledgeableMessage);
	}

}
