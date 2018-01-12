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

package org.springframework.integration.gcp.pubsub.outbound;

import java.nio.charset.Charset;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.gcp.pubsub.core.PubSubOperations;
import org.springframework.expression.Expression;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author João André Martins
 */
@RunWith(MockitoJUnitRunner.class)
public class PubSubMessageHandlerTests {

	@Mock
	private PubSubOperations pubSubTemplate;

	private PubSubMessageHandler adapter;
	private Message<?> message;

	@Before
	public void setUp() {
		this.message = new GenericMessage<>("testPayload",
				ImmutableMap.of("key1", "value1", "key2", "value2"));
		SettableListenableFuture<String> future = new SettableListenableFuture<>();
		future.set("benfica");
		when(this.pubSubTemplate.publish(eq("testTopic"),
				eq(ByteString.copyFrom("testPayload", Charset.defaultCharset())),
				isA(Map.class)))
				.thenReturn(future);
		this.adapter = new PubSubMessageHandler(this.pubSubTemplate, "testTopic");

	}

	@Test
	public void testPublish() {
		this.adapter.handleMessage(this.message);
		verify(this.pubSubTemplate, times(1))
				.publish(eq("testTopic"),
						eq(ByteString.copyFrom("testPayload", Charset.defaultCharset())),
						isA(Map.class));
	}

	@Test
	public void testPublishSync() {
		this.adapter.setSync(true);
		Expression timeout = spy(this.adapter.getPublishTimeoutExpression());
		this.adapter.setPublishTimeoutExpression(timeout);

		this.adapter.handleMessage(this.message);
		verify(timeout, times(1)).getValue(
				eq(null), eq(this.message), eq(Long.class));
	}

	@Test
	public void testPublishCallback() {
		ListenableFutureCallback<String> callbackSpy = spy(new ListenableFutureCallback<String>() {
			@Override
			public void onFailure(Throwable ex) {

			}

			@Override
			public void onSuccess(String result) {

			}
		});

		this.adapter.setPublishCallback(callbackSpy);

		this.adapter.handleMessage(this.message);

		verify(callbackSpy, times(1)).onSuccess(eq("benfica"));
	}
}
