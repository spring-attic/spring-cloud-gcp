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

import java.io.IOException;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

/**
 * Interface for converters that can convert POJOs to and from PubSub messages.
 *
 * @author Chengyuan Zhao
 */
public interface PubSubMessageConverter {

	/**
	 * Convert a Java object to a byte array payload.
	 * @param object the object to convert
	 * @return a byte array ready to be sent with PubSub
	 */
	byte[] toPayload(Object object) throws IOException;

	/**
	 * Convert a payload from a PubSub message into an object of the desired type
	 * @param payload the {@link ByteString} payload from the message
	 * @param payloadType the desired type of the object
	 * @return the object converted from the payload of the message
	 */
	<T> T fromPayload(ByteString payload, Class<T> payloadType) throws IOException;

	/**
	 * Create a PubsubMessage given an object for the payload and a map of headers
	 * @param object the object to place into the message payload
	 * @param headers the headers of the message
	 * @return the PubsubMessage ready to be sent
	 */
	default PubsubMessage toMessage(Object object, Map<String, String> headers)
			throws IOException {
		PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder()
				.setData(ByteString.copyFrom(toPayload(object)));
		if (headers != null) {
			pubsubMessageBuilder.putAllAttributes(headers);
		}
		return pubsubMessageBuilder.build();
	}

	/**
	 * Convert the payload of a given PubSub message to a desired Java type.
	 * @param message the message containing the payload of the object
	 * @param payloadType the desired type of the object
	 * @return the object converted from the message's payload
	 */
	default <T> T fromMessage(PubsubMessage message, Class<T> payloadType)
			throws IOException {
		return fromPayload(message.getData(), payloadType);
	}
}
