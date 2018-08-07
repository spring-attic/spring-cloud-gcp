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
import java.util.List;
import java.util.function.Consumer;

import com.google.api.gax.rpc.UnaryCallable;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.stub.SubscriberStub;
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

import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PubSubSubscriberTemplate}.
 *
 * @author Mike Eltsufin
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubSubscriberTemplateTests {

	private PubSubSubscriberTemplate pubSubSubscriberTemplate;

	private PubsubMessage pubsubMessage = PubsubMessage.newBuilder().build();

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
	private Consumer<ConvertedBasicAcknowledgeablePubsubMessage<Boolean>> convertedConsumer;

	@Captor
	private ArgumentCaptor<ConvertedBasicAcknowledgeablePubsubMessage<Boolean>> convertedMessage;

	@Mock
	private SubscriberStub subscriberStub;

	@Mock
	private UnaryCallable<PullRequest, PullResponse> pullCallable;


	@Before
	public void setUp() {
		// for subscribe
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
		when(this.pullCallable.call(any(PullRequest.class))).thenReturn(PullResponse.newBuilder()
				.addReceivedMessages(ReceivedMessage.newBuilder().setMessage(this.pubsubMessage).build()).build());

		// create object under test
		this.pubSubSubscriberTemplate = new PubSubSubscriberTemplate(this.subscriberFactory);
		this.pubSubSubscriberTemplate.setMessageConverter(this.messageConverter);
	}

	@Test
	public void testSubscribeAndConvert() {
		this.pubSubSubscriberTemplate.subscribeAndConvert("sub1", this.convertedConsumer, Boolean.class);

		verify(this.subscriber).startAsync();
		verify(this.messageConverter).fromPubSubMessage(this.pubsubMessage, Boolean.class);

		verify(this.convertedConsumer).accept(this.convertedMessage.capture());
		assertThat(this.convertedMessage.getValue().getPubsubMessage()).isSameAs(this.pubsubMessage);
		assertThat(this.convertedMessage.getValue().getSubscriptionName()).isEqualTo("sub1");

		this.convertedMessage.getValue().ack();
		verify(this.ackReplyConsumer).ack();
	}

	@Test
	public void testPullAndConvert() {
		List<ConvertedAcknowledgeablePubsubMessage<BigInteger>> result =
				this.pubSubSubscriberTemplate.pullAndConvert("sub2", 1, true, BigInteger.class);

		verify(this.messageConverter).fromPubSubMessage(this.pubsubMessage, BigInteger.class);
		assertThat(result.size()).isEqualTo(1);
		assertThat(result.get(0).getPubsubMessage()).isSameAs(this.pubsubMessage);
		assertThat(result.get(0).getSubscriptionName()).isEqualTo("sub2");
	}

}
