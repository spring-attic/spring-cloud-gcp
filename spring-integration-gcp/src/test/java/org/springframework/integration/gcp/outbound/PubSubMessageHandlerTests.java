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

package org.springframework.integration.gcp.outbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubMessageHandlerTests {

	@Mock
	private PubSubTemplate pubSubTemplate;

	private PubSubMessageHandler adapter;
	private Message<?> message;

	@Before
	public void setUp() {
		this.message = new GenericMessage<>("testPayload");
		when(this.pubSubTemplate.send(eq("testTopic"), eq(this.message)))
				.thenReturn(new SettableListenableFuture<>());
		this.adapter = new PubSubMessageHandler(this.pubSubTemplate);
		this.adapter.setTopic("testTopic");
	}

	@Test
	public void testSend() {
		this.adapter.handleMessage(this.message);
		verify(this.pubSubTemplate, times(1))
				.send(eq("testTopic"), eq(this.message));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetNullTopic() {
		this.adapter.setTopic(null);
	}
}
