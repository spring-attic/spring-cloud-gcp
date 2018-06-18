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

import java.nio.charset.Charset;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

/**
 * A simple {@link SimplePubSubMessageConverter} that that directly maps payloads of type
 * {@code byte[]}, {@code ByteString}, and {@code String} to Pub/Sub messages.
 *
 * @author Mike Eltsufin
 */
public class SimplePubSubMessageConverter implements PubSubMessageConverter {

	private Charset charset = Charset.defaultCharset();

	/**
	 * Set charset for converting strings.
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	public PubsubMessage toMessage(Object payload, Map<String, String> headers) {

		ByteString convertedPayload;

		if (payload instanceof String) {
			convertedPayload = ByteString.copyFrom(((String) payload).getBytes(this.charset));
		}
		else if (payload instanceof ByteString) {
			convertedPayload = (ByteString) payload;
		}
		else if (payload instanceof byte[]) {
			convertedPayload = ByteString.copyFrom((byte[]) payload);
		}
		else {
			throw new IllegalArgumentException("Unable to convert payload of type "
					+ payload.getClass().getName() + " to byte[] for sending to Pub/Sub.");
		}

		PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder()
				.setData(convertedPayload);

		if (headers != null) {
			pubsubMessageBuilder.putAllAttributes(headers);
		}

		return pubsubMessageBuilder.build();

	}

	@Override
	public <T> T fromMessage(PubsubMessage message, Class<T> payloadType) {
		byte[] payload = message.getData().toByteArray();

		if (payloadType == String.class) {
			return (T) new String(payload, this.charset);
		}
		else if (payloadType == ByteString.class) {
			return (T) message.getData();
		}
		else if (payloadType == byte[].class) {
			return (T) message.getData().toByteArray();
		}
		else {
			throw new IllegalArgumentException("Unable to convert Pub/Sub message to payload of type "
					+ payloadType.getName() + ".");
		}
	}
}
