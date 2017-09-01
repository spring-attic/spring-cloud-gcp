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

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

/**
 * Generates {@link PubsubMessage} from {@link Message} with payload type of
 * {@link String} and vice-versa. When converting to String, it defaults to using UTF-8
 * @{link Charset}.
 *
 * @author Ray Tsang
 */
public class PubSubMessageConverter implements MessageConverter {
	private final Charset charset;

	public PubSubMessageConverter() {
		this(Charset.forName("UTF-8"));
	}

	public PubSubMessageConverter(Charset charset) {
		this.charset = charset;
	}

	@Override
	public Object fromMessage(Message<?> message, Class<?> aClass) {
		if (!aClass.equals(PubsubMessage.class)) {
			throw new MessageConversionException(
					"This converter can only convert to PubsubMessage.");
		}

		PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();

		pubsubMessageBuilder.setData(ByteString.copyFrom(message.getPayload().toString(), this.charset));
		message.getHeaders().forEach((key, value) -> pubsubMessageBuilder
				.putAttributes(key, value.toString()));

		return pubsubMessageBuilder.build();
	}

	@Override
	public Message<?> toMessage(Object o, MessageHeaders messageHeaders) {
		if (!(o instanceof PubsubMessage)) {
			throw new MessageConversionException("This converter can only convert from PubsubMessage");
		}

		PubsubMessage pubsubMessage = (PubsubMessage) o;

		Map<String, Object> headers = new HashMap<>();
		headers.put(MessageHeaders.CONTENT_TYPE, "application/text");

		headers.putAll(pubsubMessage.getAttributesMap());
		if (messageHeaders != null) {
			headers.putAll(messageHeaders);
		}

		GenericMessage<String> message = new GenericMessage<String>(pubsubMessage.getData().toString(this.charset),
				new MessageHeaders(headers));

		return message;
	}
}
