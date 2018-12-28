/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.pubsub.support.converter;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the simple message converter.
 *
 * @author Mike Eltsufin
 * @author Chengyuan Zhao
 */
public class SimplePubSubMessageConverterTests {

	private static final String TEST_STRING = "test";

	private static final Map<String, String> TEST_HEADERS = ImmutableMap.of(
			"key1", "value1",
			"key2", "value2");

	/**
	 * used to test exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void testToString() {
		doToTestForType(String.class, (a) -> a);
	}

	@Test
	public void testFromString() {
		doFromTest(TEST_STRING);
	}

	@Test
	public void testToByteString() {
		doToTestForType(ByteString.class, (a) -> new String(a.toByteArray()));
	}

	@Test
	public void testFromByteString() {
		doFromTest(ByteString.copyFrom(TEST_STRING.getBytes()));
	}

	@Test
	public void testToByteArray() {
		doToTestForType(byte[].class, (a) -> new String(a));
	}

	@Test
	public void testFromByteArray() {
		doFromTest(TEST_STRING.getBytes());
	}

	@Test
	public void testToByteBuffer() {
		doToTestForType(ByteBuffer.class, (a) -> new String(a.array()));
	}

	@Test
	public void testFromByteBuffer() {
		doFromTest(ByteBuffer.wrap(TEST_STRING.getBytes()));
	}


	@Test
	public void testToUnknown() {
		this.expectedException.expect(PubSubMessageConversionException.class);
		this.expectedException.expectMessage("Unable to convert Pub/Sub message to " +
				"payload of type java.lang.Integer.");
		doToTestForType(Integer.class, (a) -> toString());

	}

	@Test
	public void testFromUnknown() {
		this.expectedException.expect(PubSubMessageConversionException.class);
		this.expectedException.expectMessage("Unable to convert payload of type java.lang.Class " +
				"to byte[] for sending to Pub/Sub.");
		doFromTest(Integer.class);
	}

	@Test
	public void testNullHeaders() {
		SimplePubSubMessageConverter converter = new SimplePubSubMessageConverter();
		PubsubMessage pubsubMessage = converter.toPubSubMessage(TEST_STRING, null);

		assertThat(pubsubMessage.getData().toString(Charset.defaultCharset())).isEqualTo(TEST_STRING);
		assertThat(pubsubMessage.getAttributesMap()).isEqualTo(new HashMap<>());
	}

	private <T> void doToTestForType(Class<T> type, Converter<T, String> toString) {
		SimplePubSubMessageConverter converter = new SimplePubSubMessageConverter();

		// test extraction from PubsubMessage to T

		String extractedMessage = toString.convert(
				(T) converter.fromPubSubMessage(
						PubsubMessage.newBuilder()
								.setData(ByteString.copyFrom(TEST_STRING.getBytes()))
								.putAllAttributes(TEST_HEADERS)
								.build(),
						type));

		assertThat(extractedMessage).isEqualTo(TEST_STRING);
	}

	private <T> void doFromTest(T value) {
		SimplePubSubMessageConverter converter = new SimplePubSubMessageConverter();

		// test conversion of T to PubsubMessage
		PubsubMessage convertedPubSubMessage = converter.toPubSubMessage(value, TEST_HEADERS);
		assertThat(new String(convertedPubSubMessage.getData().toByteArray())).isEqualTo(TEST_STRING);
		assertThat(convertedPubSubMessage.getAttributesMap()).isEqualTo(TEST_HEADERS);
	}
}
