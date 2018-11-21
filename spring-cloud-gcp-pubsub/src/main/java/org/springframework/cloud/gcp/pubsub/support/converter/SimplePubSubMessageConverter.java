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
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

/**
 * A simple {@link PubSubMessageConverter} that directly maps payloads of type
 * {@code byte[]}, {@code ByteString}, {@code ByteBuffer}, and {@code String} to Pub/Sub messages.
 *
 * @author Mike Eltsufin
 */
public class SimplePubSubMessageConverter implements PubSubMessageConverter {

	private final Charset charset;

	public SimplePubSubMessageConverter() {
		this(Charset.defaultCharset());
	}

	public SimplePubSubMessageConverter(Charset charset) {
		this.charset = charset;
	}

	@Override
	public PubsubMessage toPubSubMessage(Object payload, Map<String, String> headers) {

		ByteString convertedPayload;

		if (payload instanceof ByteString) {
			convertedPayload = (ByteString) payload;
		}
		else if (payload instanceof String) {
			convertedPayload = ByteString.copyFrom(((String) payload).getBytes(this.charset));
		}
		else if (payload instanceof ByteBuffer) {
			convertedPayload = ByteString.copyFrom((ByteBuffer) payload);
		}
		else if (payload instanceof byte[]) {
			convertedPayload = ByteString.copyFrom((byte[]) payload);
		}
		else {
			throw new PubSubMessageConversionException("Unable to convert payload of type " +
					payload.getClass().getName() + " to byte[] for sending to Pub/Sub.");
		}

		PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder()
				.setData(convertedPayload);

		if (headers != null) {
			pubsubMessageBuilder.putAllAttributes(headers);
		}

		return pubsubMessageBuilder.build();

	}

	@Override
	public <T> T fromPubSubMessage(PubsubMessage message, Class<T> payloadType) {
		T result;
		byte[] payload = message.getData().toByteArray();

		if (payloadType == ByteString.class) {
			result = (T) message.getData();
		}
		else if (payloadType == String.class) {
			result = (T) new String(payload, this.charset);
		}
		else if (payloadType == ByteBuffer.class) {
			result = (T) ByteBuffer.wrap(payload);
		}
		else if (payloadType == byte[].class) {
			result = (T) payload;
		}
		else {
			throw new PubSubMessageConversionException("Unable to convert Pub/Sub message to payload of type " +
					payloadType.getName() + ".");
		}

		return result;
	}
}
