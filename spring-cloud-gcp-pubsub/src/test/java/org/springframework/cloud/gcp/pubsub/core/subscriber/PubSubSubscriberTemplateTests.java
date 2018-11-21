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

package org.springframework.cloud.gcp.pubsub.core.subscriber;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.api.core.ApiFuture;
import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
import com.google.protobuf.Empty;
import com.google.pubsub.v1.AcknowledgeRequest;
import com.google.pubsub.v1.ModifyAckDeadlineRequest;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.ReceivedMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PubSubSubscriberTemplate}.
 *
 * @author Mike Eltsufin
 * @author Doug Hoard
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubSubscriberTemplateTests {

	private PubSubSubscriberTemplate pubSubSubscriberTemplate;

	private PubsubMessage pubsubMessage = PubsubMessage.newBuilder().build();

	@Mock
	private MessageReceiver messageReceiver;

	@Mock
	private AckReplyConsumer ackReplyConsumer;

	@Mock
	private SubscriberFactory subscriberFactory;

	@Mock
	private Subscriber subscriber;

	@Mock
	private PubSubMessageConverter messageConverter;

	@Mock
	private Consumer<BasicAcknowledgeablePubsubMessage> consumer;

	@Captor
	private ArgumentCaptor<BasicAcknowledgeablePubsubMessage> message;

	@Mock
	private Consumer<ConvertedBasicAcknowledgeablePubsubMessage<Boolean>> convertedConsumer;

	@Captor
	private ArgumentCaptor<ConvertedBasicAcknowledgeablePubsubMessage<Boolean>> convertedMessage;

	@Mock
	private SubscriberStub subscriberStub;

	@Mock
	private UnaryCallable<PullRequest, PullResponse> pullCallable;

	@Mock
	private UnaryCallable<AcknowledgeRequest, Empty> ackCallable;

	@Mock
	private UnaryCallable<ModifyAckDeadlineRequest, Empty> modifyAckDeadlineCallable;

	@Mock
	private ApiFuture<Empty> apiFuture;

	@Before
	public void setUp() {
		reset(this.subscriberFactory);
		reset(this.subscriberStub);
		reset(this.subscriber);
		reset(this.messageReceiver);
		reset(this.apiFuture);

		when(this.subscriberFactory.getProjectId()).thenReturn("testProject");

		// for subscribe with MessageReceiver
		when(this.subscriberFactory.createSubscriber(any(String.class), any(MessageReceiver.class)))
				.then(invocation -> {
					this.messageReceiver = invocation.getArgument(1);
					return this.subscriber;
				});

		when(this.subscriber.startAsync()).then(invocation -> {
			this.messageReceiver.receiveMessage(this.pubsubMessage, this.ackReplyConsumer);
			return null;
		});

		// for pull
		when(this.subscriberFactory.createPullRequest(any(String.class), any(Integer.class), any(Boolean.class)))
				.then(invocation -> PullRequest.newBuilder().setSubscription(invocation.getArgument(0)).build());

		when(this.subscriberFactory.createSubscriberStub()).thenReturn(this.subscriberStub);

		when(this.subscriberStub.pullCallable()).thenReturn(this.pullCallable);
		when(this.subscriberStub.acknowledgeCallable()).thenReturn(this.ackCallable);
		when(this.subscriberStub.modifyAckDeadlineCallable()).thenReturn(this.modifyAckDeadlineCallable);

		when(this.ackCallable.futureCall(any(AcknowledgeRequest.class))).thenReturn(this.apiFuture);

		when(this.modifyAckDeadlineCallable.futureCall(any(ModifyAckDeadlineRequest.class))).thenReturn(this.apiFuture);

		doAnswer(invocation -> {
			Runnable runnable = invocation.getArgument(0);
			runnable.run();
			return null;
		}).when(this.apiFuture).addListener(any(Runnable.class), any(Executor.class));

		when(this.apiFuture.isDone()).thenReturn(true);

		doNothing().when(this.ackReplyConsumer).ack();
		doNothing().when(this.ackReplyConsumer).nack();

		// create objects under test
		when(this.subscriberFactory.createSubscriberStub()).thenReturn(this.subscriberStub);
		when(this.subscriberStub.pullCallable()).thenReturn(this.pullCallable);
		when(this.pullCallable.call(any(PullRequest.class))).thenReturn(PullResponse.newBuilder()
				.addReceivedMessages(ReceivedMessage.newBuilder().setMessage(this.pubsubMessage).build()).build());

		// create object under test
		this.pubSubSubscriberTemplate = spy(new PubSubSubscriberTemplate(this.subscriberFactory));
		this.pubSubSubscriberTemplate.setMessageConverter(this.messageConverter);
	}

	@Test
	public void testSubscribe_AndManualAck() throws InterruptedException, ExecutionException, TimeoutException {
		this.pubSubSubscriberTemplate.subscribe("sub1", this.consumer);

		verify(this.subscriber).startAsync();
		verify(this.consumer).accept(this.message.capture());

		TestListenableFutureCallback testListenableFutureCallback = new TestListenableFutureCallback();

		ListenableFuture<Void> listenableFuture = this.message.getValue().ack();

		assertThat(listenableFuture).isNotNull();

		listenableFuture.addCallback(testListenableFutureCallback);
		listenableFuture.get(10L, TimeUnit.SECONDS);

		assertThat(listenableFuture.isDone()).isTrue();

		verify(this.ackReplyConsumer).ack();

		assertThat(testListenableFutureCallback.getThrowable()).isNull();
	}

	@Test
	public void testSubscribe_AndManualNack() throws InterruptedException, ExecutionException, TimeoutException {
		this.pubSubSubscriberTemplate.subscribe("sub1", this.consumer);

		verify(this.subscriber).startAsync();
		verify(this.consumer).accept(this.message.capture());

		TestListenableFutureCallback testListenableFutureCallback = new TestListenableFutureCallback();

		ListenableFuture<Void> listenableFuture = this.message.getValue().nack();

		assertThat(listenableFuture).isNotNull();

		listenableFuture.addCallback(testListenableFutureCallback);
		listenableFuture.get(10L, TimeUnit.SECONDS);

		assertThat(listenableFuture.isDone()).isTrue();

		verify(this.ackReplyConsumer).nack();

		assertThat(testListenableFutureCallback.getThrowable()).isNull();
	}

	@Test
	public void testSubscribeAndConvert_AndManualAck()
			throws InterruptedException, ExecutionException, TimeoutException {
		this.pubSubSubscriberTemplate.subscribeAndConvert("sub1", this.convertedConsumer, Boolean.class);

		verify(this.subscriber).startAsync();
		verify(this.messageConverter).fromPubSubMessage(this.pubsubMessage, Boolean.class);
		verify(this.convertedConsumer).accept(this.convertedMessage.capture());

		assertThat(this.convertedMessage.getValue().getPubsubMessage()).isSameAs(this.pubsubMessage);
		assertThat(this.convertedMessage.getValue().getProjectSubscriptionName().getProject()).isEqualTo("testProject");
		assertThat(this.convertedMessage.getValue().getProjectSubscriptionName().getSubscription()).isEqualTo("sub1");

		TestListenableFutureCallback testListenableFutureCallback = new TestListenableFutureCallback();

		ListenableFuture<Void> listenableFuture = this.convertedMessage.getValue().ack();

		assertThat(listenableFuture).isNotNull();

		listenableFuture.addCallback(testListenableFutureCallback);
		listenableFuture.get(10L, TimeUnit.SECONDS);

		assertThat(listenableFuture.isDone()).isTrue();

		verify(this.ackReplyConsumer).ack();

		assertThat(testListenableFutureCallback.getThrowable()).isNull();
	}

	@Test
	public void testSubscribeAndConvert_AndManualNack()
			throws InterruptedException, ExecutionException, TimeoutException {
		this.pubSubSubscriberTemplate.subscribeAndConvert("sub1", this.convertedConsumer, Boolean.class);

		verify(this.subscriber).startAsync();
		verify(this.messageConverter).fromPubSubMessage(this.pubsubMessage, Boolean.class);
		verify(this.convertedConsumer).accept(this.convertedMessage.capture());

		assertThat(this.convertedMessage.getValue().getPubsubMessage()).isSameAs(this.pubsubMessage);
		assertThat(this.convertedMessage.getValue().getProjectSubscriptionName().getProject()).isEqualTo("testProject");
		assertThat(this.convertedMessage.getValue().getProjectSubscriptionName().getSubscription()).isEqualTo("sub1");

		TestListenableFutureCallback testListenableFutureCallback = new TestListenableFutureCallback();

		ListenableFuture<Void> listenableFuture = this.convertedMessage.getValue().nack();

		assertThat(listenableFuture).isNotNull();

		listenableFuture.addCallback(testListenableFutureCallback);
		listenableFuture.get(10L, TimeUnit.SECONDS);

		assertThat(listenableFuture.isDone()).isTrue();

		verify(this.ackReplyConsumer).nack();

		assertThat(testListenableFutureCallback.getThrowable()).isNull();
	}

	@Test
	public void testPull_AndManualAck() throws InterruptedException, ExecutionException, TimeoutException {
		List<AcknowledgeablePubsubMessage> result = this.pubSubSubscriberTemplate.pull(
				"sub2", 1, true);

		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getPubsubMessage()).isSameAs(this.pubsubMessage);
		assertThat(result.get(0).getProjectSubscriptionName().getProject()).isEqualTo("testProject");
		assertThat(result.get(0).getProjectSubscriptionName().getSubscription()).isEqualTo("sub2");

		AcknowledgeablePubsubMessage acknowledgeablePubsubMessage = result.get(0);
		assertThat(acknowledgeablePubsubMessage.getAckId()).isNotNull();

		TestListenableFutureCallback testListenableFutureCallback = new TestListenableFutureCallback();

		ListenableFuture<Void> listenableFuture = this.pubSubSubscriberTemplate.ack(result);

		assertThat(listenableFuture).isNotNull();

		listenableFuture.addCallback(testListenableFutureCallback);
		listenableFuture.get(10L, TimeUnit.SECONDS);

		assertThat(listenableFuture.isDone()).isTrue();

		assertThat(testListenableFutureCallback.getThrowable()).isNull();
	}

	@Test
	public void testPull_AndManualNack() throws InterruptedException, ExecutionException, TimeoutException {
		List<AcknowledgeablePubsubMessage> result = this.pubSubSubscriberTemplate.pull(
				"sub2", 1, true);

		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getPubsubMessage()).isSameAs(this.pubsubMessage);
		assertThat(result.get(0).getProjectSubscriptionName().getProject()).isEqualTo("testProject");
		assertThat(result.get(0).getProjectSubscriptionName().getSubscription()).isEqualTo("sub2");

		AcknowledgeablePubsubMessage acknowledgeablePubsubMessage = result.get(0);
		assertThat(acknowledgeablePubsubMessage.getAckId()).isNotNull();

		TestListenableFutureCallback testListenableFutureCallback = new TestListenableFutureCallback();

		ListenableFuture<Void> listenableFuture = this.pubSubSubscriberTemplate.nack(result);

		assertThat(listenableFuture).isNotNull();

		listenableFuture.addCallback(testListenableFutureCallback);
		listenableFuture.get(10L, TimeUnit.SECONDS);

		assertThat(listenableFuture.isDone()).isTrue();

		assertThat(testListenableFutureCallback.getThrowable()).isNull();
	}

	@Test
	public void testPull_AndManualMultiSubscriptionAck()
			throws InterruptedException, ExecutionException, TimeoutException {
		List<AcknowledgeablePubsubMessage> result1 = this.pubSubSubscriberTemplate.pull(
				"sub1", 1, true);
		List<AcknowledgeablePubsubMessage> result2 = this.pubSubSubscriberTemplate.pull(
				"sub2", 1, true);
		Set<AcknowledgeablePubsubMessage> combinedMessages = new HashSet<>(result1);
		combinedMessages.addAll(result2);

		assertThat(combinedMessages.size()).isEqualTo(2);

		TestListenableFutureCallback testListenableFutureCallback = new TestListenableFutureCallback();

		ListenableFuture<Void> listenableFuture = this.pubSubSubscriberTemplate.ack(combinedMessages);
		assertThat(listenableFuture).isNotNull();

		listenableFuture.addCallback(testListenableFutureCallback);
		listenableFuture.get(10L, TimeUnit.SECONDS);

		assertThat(listenableFuture.isDone()).isTrue();
		assertThat(testListenableFutureCallback.getThrowable()).isNull();
		verify(this.ackCallable, times(2)).futureCall(any(AcknowledgeRequest.class));
	}

	@Test
	public void testPullAndAck() {
		List<PubsubMessage> result = this.pubSubSubscriberTemplate.pullAndAck(
				"sub2", 1, true);

		assertThat(result.size()).isEqualTo(1);

		PubsubMessage pubsubMessage = result.get(0);
		assertThat(pubsubMessage).isSameAs(this.pubsubMessage);

		verify(this.pubSubSubscriberTemplate, times(1)).ack(any());
	}

	@Test
	public void testPullAndAck_NoMessages() {
		when(this.pullCallable.call(any(PullRequest.class))).thenReturn(PullResponse.newBuilder().build());

		List<PubsubMessage> result = this.pubSubSubscriberTemplate.pullAndAck(
				"sub2", 1, true);

		assertThat(result.size()).isEqualTo(0);

		verify(this.pubSubSubscriberTemplate, never()).ack(any());
	}

	@Test
	public void testPullAndConvert() {
		List<ConvertedAcknowledgeablePubsubMessage<BigInteger>> result = this.pubSubSubscriberTemplate.pullAndConvert(
				"sub2", 1, true, BigInteger.class);

		verify(this.messageConverter).fromPubSubMessage(this.pubsubMessage, BigInteger.class);

		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getPubsubMessage()).isSameAs(this.pubsubMessage);
		assertThat(result.get(0).getProjectSubscriptionName().getProject()).isEqualTo("testProject");
		assertThat(result.get(0).getProjectSubscriptionName().getSubscription()).isEqualTo("sub2");
	}

	private class TestListenableFutureCallback implements ListenableFutureCallback<Void> {

		private Throwable throwable;

		@Override
		public void onFailure(Throwable throwable) {
			this.throwable = throwable;
		}

		@Override
		public void onSuccess(Void aVoid) {

		}

		public Throwable getThrowable() {
			return this.throwable;
		}
	}

}
