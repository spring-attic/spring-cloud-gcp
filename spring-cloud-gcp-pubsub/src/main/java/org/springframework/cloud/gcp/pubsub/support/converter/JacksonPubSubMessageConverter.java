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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.util.Assert;

/**
 * A converter using Jackson JSON.
 *
 * @author Chengyuan Zhao
 * @author Mike Eltsufin
 */
public class JacksonPubSubMessageConverter implements PubSubMessageConverter {

	private final ObjectMapper objectMapper;

	/**
	 * Constructor
	 * @param objectMapper the object mapper used to create and read JSON.
	 */
	public JacksonPubSubMessageConverter(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "A valid ObjectMapper is required.");
		this.objectMapper = objectMapper;
	}

	@Override
	public PubsubMessage toPubSubMessage(Object payload, Map<String, String> headers) {
		try {
			PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder()
					.setData(ByteString.copyFrom(this.objectMapper.writeValueAsBytes(payload)));

			if (headers != null) {
				pubsubMessageBuilder.putAllAttributes(headers);
			}

			return pubsubMessageBuilder.build();
		}
		catch (JsonProcessingException e) {
			throw new PubSubMessageConversionException("JSON serialization of an object of type " +
					payload.getClass().getName() + " failed.", e);
		}
	}

	@Override
	public <T> T fromPubSubMessage(PubsubMessage message, Class<T> payloadType) {
		try {
			return (T) this.objectMapper.readerFor(payloadType).readValue(message.getData().toByteArray());
		}
		catch (IOException e) {
			throw new PubSubMessageConversionException("JSON deserialization of an object of type " +
					payloadType.getName() + " failed.", e);
		}
	}

}
