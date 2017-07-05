/*
 *  Copyright 2017 original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.api.core.SettableApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.support.PublisherFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.concurrent.ListenableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubTemplateTests {

	@Mock
	private PublisherFactory mockPublisherFactory;
	@Mock
	private Publisher mockPublisher;

	private PubSubTemplate pubSubTemplate;
	private Message<String> siMessage;
	private SettableApiFuture<String> settableApiFuture;

	@Before
	public void setUp() throws ExecutionException, InterruptedException {
		this.pubSubTemplate = new PubSubTemplate(this.mockPublisherFactory);
		when(this.mockPublisherFactory.getPublisher("testTopic")).thenReturn(this.mockPublisher);
		this.settableApiFuture = SettableApiFuture.create();
		when(this.mockPublisher.publish(isA(PubsubMessage.class)))
				.thenReturn(this.settableApiFuture);

		Map<String, Object> headers = new HashMap<>();
		headers.put("header1", "value1");
		headers.put("header2", 12345);

		this.siMessage = new GenericMessage<>("testBody", headers);
	}

	@Test
	public void testSend() throws ExecutionException, InterruptedException {
		this.settableApiFuture.set("result");
		ListenableFuture<String> future = this.pubSubTemplate.send("testTopic", this.siMessage);

		assertEquals("result", future.get());
	}

	@Test(expected = MessageConversionException.class)
	public void testSend_notPubsubMessage() {
		this.pubSubTemplate.setMessageConverter(new MessageConverter() {
			@Override
			public Object fromMessage(Message<?> message, Class<?> targetClass) {
				return "not PubsubMessage.class";
			}

			@Override
			public Message<?> toMessage(Object payload, MessageHeaders headers) {
				return null;
			}
		});

		this.pubSubTemplate.send("testTopic", this.siMessage);
	}

	@Test(expected = PubSubException.class)
	public void testSend_noPublisher() {
		when(this.mockPublisherFactory.getPublisher("testTopic"))
				.thenThrow(new PubSubException("couldn't create the publisher."));

		this.pubSubTemplate.send("testTopic", this.siMessage);
	}

	@Test
	public void testSend_onFailure() {
		ListenableFuture<String> future = this.pubSubTemplate.send("testTopic", this.siMessage);
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

	@Test(expected = IllegalArgumentException.class)
	public void testSetNullMessageConverter() {
		this.pubSubTemplate.setMessageConverter(null);
	}
}
