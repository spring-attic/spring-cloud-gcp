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

package org.springframework.cloud.gcp.pubsub.converters;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.GenericMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class PubSubMessageConverterTests {

	@Test
	public void testFromMessage() {
		PubSubMessageConverter converter = new PubSubMessageConverter();

		Map<String, Object> headers = new HashMap<>();
		headers.put("h1", "value");
		headers.put("h2", 23);
		Message<?> message = new GenericMessage<Object>("test payload", headers);
		PubsubMessage pubsubMessage = (PubsubMessage) converter.fromMessage(message,
				PubsubMessage.class);
		assertEquals("test payload", pubsubMessage.getData().toStringUtf8());
		assertEquals("value", pubsubMessage.getAttributesOrDefault("h1", "def"));
		assertEquals("23", pubsubMessage.getAttributesOrDefault("h2", "def"));
		assertEquals(4, pubsubMessage.getAttributesCount());
	}

	@Test(expected = MessageConversionException.class)
	public void testFromMessage_notPubsubMessage() {
		new PubSubMessageConverter().fromMessage(new GenericMessage<Object>("payload"),
				String.class);
	}

	@Test
	public void testToMessage() {
		PubSubMessageConverter converter = new PubSubMessageConverter();
		PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8("test payload"))
				.putAttributes("headerKey1", "headerValue1")
				.putAttributes("headerKey2", "headerValue2")
				.build();

		Map<String, Object> headers = new HashMap<>();
		headers.put("headerKey3", "headerValue3");
		headers.put("headerKey4", "headerValue4");
		Message<?> message = converter.toMessage(pubsubMessage, new MessageHeaders(headers));
		assertEquals("test payload", message.getPayload());
		assertTrue(message.getHeaders().get("headerKey1").equals("headerValue1"));
		assertTrue(message.getHeaders().get("headerKey2").equals("headerValue2"));
		assertTrue(message.getHeaders().get("headerKey3").equals("headerValue3"));
		assertTrue(message.getHeaders().get("headerKey4").equals("headerValue4"));
	}

	@Test
	public void testToMessage_noHeaders() {
		PubSubMessageConverter converter = new PubSubMessageConverter();
		PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
				.setData(ByteString.copyFromUtf8("test payload"))
				.build();
		Message<?> message = converter.toMessage(pubsubMessage,
				new MessageHeaders(new HashMap<>()));
		assertEquals("test payload", message.getPayload());
	}

	@Test(expected = MessageConversionException.class)
	public void testToMessage_notString() {
		new PubSubMessageConverter().toMessage(new HashMap<>(),
				new MessageHeaders(new HashMap<>()));
	}
}
