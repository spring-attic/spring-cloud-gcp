/*
 *  Copyright 2017-2018 original author or authors.
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

package org.springframework.cloud.gcp.pubsub.core;

import java.util.concurrent.ExecutionException;

import com.google.api.core.ApiService;
import com.google.api.core.SettableApiFuture;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.cloud.gcp.pubsub.support.SubscriberFactory;
import org.springframework.util.concurrent.ListenableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubTemplateTests {

	@Mock
	private PublisherFactory mockPublisherFactory;

	@Mock
	private SubscriberFactory mockSubscriberFactory;

	@Mock
	private Publisher mockPublisher;

	@Mock
	private Subscriber mockSubscriber;

	private PubSubTemplate pubSubTemplate;
	private PubsubMessage pubsubMessage;
	private SettableApiFuture<String> settableApiFuture;

	@Before
	public void setUp() {
		this.pubSubTemplate = new PubSubTemplate(this.mockPublisherFactory,
				this.mockSubscriberFactory);
		when(this.mockPublisherFactory.createPublisher("testTopic"))
				.thenReturn(this.mockPublisher);
		this.settableApiFuture = SettableApiFuture.create();
		when(this.mockPublisher.publish(isA(PubsubMessage.class)))
				.thenReturn(this.settableApiFuture);

		when(this.mockSubscriberFactory.createSubscriber(
				eq("testSubscription"), isA(MessageReceiver.class)))
				.thenReturn(this.mockSubscriber);
		when(this.mockSubscriber.startAsync()).thenReturn(mock(ApiService.class));

		this.pubsubMessage = PubsubMessage.newBuilder().setData(
				ByteString.copyFrom("permanating".getBytes())).build();
	}

	@Test
	public void testPublish() throws ExecutionException, InterruptedException {
		this.settableApiFuture.set("result");
		ListenableFuture<String> future = this.pubSubTemplate.publish("testTopic",
				this.pubsubMessage);

		assertEquals("result", future.get());
	}

	@Test
	public void testPublish_String() {
		this.pubSubTemplate.publish("testTopic", "testPayload", null);

		verify(this.mockPublisher, times(1))
				.publish(isA(PubsubMessage.class));
	}

	@Test
	public void testPublish_Bytes() {
		this.pubSubTemplate.publish("testTopic", "testPayload".getBytes(), null);

		verify(this.mockPublisher, times(1))
				.publish(isA(PubsubMessage.class));
	}

	@Test(expected = PubSubException.class)
	public void testSend_noPublisher() {
		when(this.mockPublisherFactory.createPublisher("testTopic"))
				.thenThrow(new PubSubException("couldn't create the publisher."));

		this.pubSubTemplate.publish("testTopic", this.pubsubMessage);
	}

	@Test
	public void testSend_onFailure() {
		ListenableFuture<String> future =
				this.pubSubTemplate.publish("testTopic", this.pubsubMessage);
		this.settableApiFuture.setException(new Exception("future failed."));

		try {
			future.get();
			fail("Test should fail.");
		}
		catch (InterruptedException ie) {
			fail("get() should fail with an ExecutionException.");
		}
		catch (ExecutionException ee) {
			assertEquals("future failed.", ee.getCause().getMessage());
		}
	}

	@Test
	public void testSubscribe() {
		Subscriber subscriber = this.pubSubTemplate.subscribe("testSubscription",
				(message, consumer) -> { });
		assertEquals(this.mockSubscriber, subscriber);
		verify(this.mockSubscriber, times(1)).startAsync();
	}
}
