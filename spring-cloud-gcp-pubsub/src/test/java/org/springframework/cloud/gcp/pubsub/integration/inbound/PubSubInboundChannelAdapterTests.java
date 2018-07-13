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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
@RunWith(MockitoJUnitRunner.class)
public class PubSubInboundChannelAdapterTests {

	public static final String ACK = "ACK";

	public static final String NACK = "NACK";

	private PubSubOperations pubSubOperations;

	private PubSubSubscriberOperations pubSubSubscriberOperations;

	private MessageChannel messageChannel;

	private AtomicReference<String> returnValue;

	private CountDownLatch countDownLatch;

	private AckReplyConsumer ackReplyConsumer;

	private TestAnswer testAnswer;

	@Before
	public void setUp() {
		this.pubSubOperations = mock(PubSubOperations.class);
		this.pubSubSubscriberOperations = mock(PubSubSubscriberOperations.class);
		this.messageChannel = mock(MessageChannel.class);

		when(this.messageChannel.send(any())).thenThrow(new RuntimeException("Forced exception sending message"));

		this.returnValue = new AtomicReference<>();
		this.countDownLatch = new CountDownLatch(1);

		NackAnswer nackAnwser = new NackAnswer(this.returnValue);

		this.ackReplyConsumer = mock(AckReplyConsumer.class);

		doAnswer(nackAnwser).when(this.ackReplyConsumer).nack();

		this.testAnswer = new TestAnswer(this.countDownLatch, this.ackReplyConsumer);
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

		when(this.pubSubSubscriberOperations.subscribe(anyString(), any(MessageReceiver.class))).then(this.testAnswer);

		adapter.setOutputChannel(this.messageChannel);

		try {
			adapter.start();
		}
		catch (Throwable t) {

		}

		Assert.assertTrue(this.countDownLatch.await(10, TimeUnit.SECONDS));
		Assert.assertEquals(NACK, this.returnValue.get());
	}

	@Test
	public void testAckModeAutoAck() throws Exception {
		PubSubInboundChannelAdapter adapter = new PubSubInboundChannelAdapter(
				this.pubSubSubscriberOperations, "testSubscription");

		adapter.setAckMode(AckMode.AUTO_ACK);

		when(this.pubSubSubscriberOperations.subscribe(anyString(), any(MessageReceiver.class))).then(this.testAnswer);

		adapter.setOutputChannel(this.messageChannel);

		try {
			adapter.start();
		}
		catch (Throwable t) {

		}

		Assert.assertTrue(this.countDownLatch.await(10, TimeUnit.SECONDS));
		Assert.assertNull(this.returnValue.get());
	}

	private class TestAnswer implements Answer<MessageReceiver> {

		private CountDownLatch countDownLatch;

		private AckReplyConsumer ackReplyConsumer;

		TestAnswer(CountDownLatch countDownLatch, AckReplyConsumer ackReplyConsumer) {
			this.countDownLatch = countDownLatch;
			this.ackReplyConsumer = ackReplyConsumer;
		}

		public MessageReceiver answer(InvocationOnMock invocation) throws Throwable {
			try {
				PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(
						ByteString.copyFrom("Testing 1 2 3".getBytes("UTF-8"))).build();

				MessageReceiver messageReceiver = invocation.getArgument(1);
				messageReceiver.receiveMessage(pubsubMessage, this.ackReplyConsumer);
			}
			finally {
				this.countDownLatch.countDown();
			}

			return null;
		}
	}

	private class NackAnswer implements Answer<Boolean> {

		private AtomicReference<String> returnValue;

		NackAnswer(AtomicReference<String> returnValue) {
			this.returnValue = returnValue;
		}

		public Boolean answer(InvocationOnMock invocationOnMock) throws Throwable {
			this.returnValue.set(NACK);

			return null;
		}
	}
}
