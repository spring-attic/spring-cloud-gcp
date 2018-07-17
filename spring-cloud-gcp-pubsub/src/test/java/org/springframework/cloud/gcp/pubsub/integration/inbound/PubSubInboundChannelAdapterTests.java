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

package org.springframework.cloud.gcp.pubsub.integration.inbound;

import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.messaging.MessageChannel;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link PubSubInboundChannelAdapter} unit tests.
 *
 * @author João André Martins
 * @author Doug Hoard
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class PubSubInboundChannelAdapterTests {

	public static final String ACK = "ACK";

	public static final String NACK = "NACK";

	public static final String EXCEPTION_MESSAGE = "Forced exception sending message";

	public static final String EXPECTED_EXCEPTION = "Expected exception";

	private PubSubOperations pubSubOperations;

	private PubSubSubscriberOperations pubSubSubscriberOperations;

	private MessageChannel messageChannel;

	private String value;

	private AckReplyConsumer ackReplyConsumer;

	@Before
	public void setUp() {
		this.pubSubOperations = mock(PubSubOperations.class);
		this.pubSubSubscriberOperations = mock(PubSubSubscriberOperations.class);
		this.messageChannel = mock(MessageChannel.class);
		this.ackReplyConsumer = mock(AckReplyConsumer.class);
		this.value = null;

		when(this.messageChannel.send(any())).thenThrow(
				new RuntimeException(EXCEPTION_MESSAGE));

		when(this.pubSubSubscriberOperations.subscribe(
				anyString(), any(MessageReceiver.class))).then(
						new MessageReceiverAnswer(this.ackReplyConsumer));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNonNullAckMode() {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
				this.pubSubOperations, "testSubscription");

		adapter.setAckMode(null);
	}

	@Test
	public void testAckModeAuto() throws Exception {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
				this.pubSubSubscriberOperations, "testSubscription");

		adapter.setAckMode(AckMode.AUTO);
		adapter.setOutputChannel(this.messageChannel);

		doAnswer(new CallbackAnswer(this, NACK)).when(
				this.ackReplyConsumer).nack();

		try {
			adapter.start();

			Assert.fail(EXPECTED_EXCEPTION);
		}
		catch (Throwable t) {
			Assert.assertEquals(EXCEPTION_MESSAGE, t.getCause().getMessage());
		}

		Assert.assertEquals(NACK, this.value);
	}

	@Test
	public void testAckModeAutoAck() throws Exception {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
				this.pubSubSubscriberOperations, "testSubscription");

		adapter.setAckMode(AckMode.AUTO_ACK);
		adapter.setOutputChannel(this.messageChannel);

		doAnswer(new CallbackAnswer(this, NACK)).when(
				this.ackReplyConsumer).nack();

		try {
			adapter.start();

			Assert.fail(EXPECTED_EXCEPTION);
		}
		catch (Throwable t) {
			Assert.assertEquals(EXCEPTION_MESSAGE, t.getCause().getMessage());
		}

		Assert.assertNull(this.value);
	}

	public void setValue(String value) {
		this.value = value;
	}

	private class MessageReceiverAnswer implements Answer<Void> {

		private AckReplyConsumer ackReplyConsumer;

		MessageReceiverAnswer(AckReplyConsumer ackReplyConsumer) {
			this.ackReplyConsumer = ackReplyConsumer;
		}

		public Void answer(InvocationOnMock invocation) throws Throwable {
			PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(
					ByteString.copyFrom(
							"Testing 1 2 3".getBytes("UTF-8"))).build();

			MessageReceiver messageReceiver = invocation.getArgument(1);
			messageReceiver.receiveMessage(pubsubMessage, this.ackReplyConsumer);

			return null;
		}
	}

	private class CallbackAnswer implements Answer<Void> {

		private PubSubInboundChannelAdapterTests callback;

		private String value;

		CallbackAnswer(PubSubInboundChannelAdapterTests callback, String value) {
			this.callback = callback;
			this.value = value;
		}

		public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
			this.callback.setValue(this.value);

			return null;
		}
	}
}
