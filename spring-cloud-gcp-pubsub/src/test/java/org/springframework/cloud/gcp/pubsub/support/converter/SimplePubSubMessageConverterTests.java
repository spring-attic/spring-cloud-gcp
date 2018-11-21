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

package org.springframework.cloud.gcp.pubsub.support.converter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Test;

import org.springframework.core.convert.converter.Converter;

import static org.junit.Assert.assertEquals;

/**
 * @author Mike Eltsufin
 */
public class SimplePubSubMessageConverterTests {

	private final static String TEST_STRING = "test";

	private final static Map<String, String> TEST_HEADERS = ImmutableMap.of(
			"key1", "value1",
			"key2", "value2");

	@Test
	public void testToString() {
		doToTestForType(String.class, a -> a);
	}

	@Test
	public void testFromString() {
		doFromTest(TEST_STRING);
	}

	@Test
	public void testToByteString() {
		doToTestForType(ByteString.class, a -> new String(a.toByteArray()));
	}

	@Test
	public void testFromByteString() {
		doFromTest(ByteString.copyFrom(TEST_STRING.getBytes()));
	}

	@Test
	public void testToByteArray() {
		doToTestForType(byte[].class, a -> new String(a));
	}

	@Test
	public void testFromByteArray() {
		doFromTest(TEST_STRING.getBytes());
	}

	@Test
	public void testToByteBuffer() {
		doToTestForType(ByteBuffer.class, a -> new String(a.array()));
	}

	@Test
	public void testFromByteBuffer() {
		doFromTest(ByteBuffer.wrap(TEST_STRING.getBytes()));
	}


	@Test(expected = PubSubMessageConversionException.class)
	public void testToUnknown() {
		doToTestForType(Integer.class, a -> toString());

	}

	@Test(expected = PubSubMessageConversionException.class)
	public void testFromUnknown() {
		doFromTest(Integer.class);
	}

	@Test
	public void testNullHeaders() {
		SimplePubSubMessageConverter converter = new SimplePubSubMessageConverter();
		PubsubMessage pubsubMessage = converter.toPubSubMessage(TEST_STRING, null);

		assertEquals(TEST_STRING, pubsubMessage.getData().toString(Charset.defaultCharset()));
		assertEquals(new HashMap<>(), pubsubMessage.getAttributesMap());
	}

	private <T> void doToTestForType(Class<T> type, Converter<T, String> toString) {
		SimplePubSubMessageConverter converter = new SimplePubSubMessageConverter();

		// test extraction from PubsubMessage to T
		assertEquals(TEST_STRING, toString.convert((T) converter.fromPubSubMessage(PubsubMessage.newBuilder()
				.setData(ByteString.copyFrom(TEST_STRING.getBytes())).putAllAttributes(TEST_HEADERS).build(), type)));
	}

	private <T> void doFromTest(T value) {
		SimplePubSubMessageConverter converter = new SimplePubSubMessageConverter();

		// test conversion of T to PubsubMessage
		PubsubMessage convertedPubSubMessage = converter.toPubSubMessage(value, TEST_HEADERS);
		assertEquals(TEST_STRING, new String(convertedPubSubMessage.getData().toByteArray()));
		assertEquals(TEST_HEADERS, convertedPubSubMessage.getAttributesMap());
	}
}
