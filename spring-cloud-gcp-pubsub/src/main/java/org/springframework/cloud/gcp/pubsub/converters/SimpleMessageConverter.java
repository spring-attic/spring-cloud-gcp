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

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

/**
 * Generates {@link PubsubMessage} from {@link Message} and vice-versa.
 *
 * @author João André Martins
 */
public class SimpleMessageConverter implements MessageConverter {

	@Override
	public Object fromMessage(Message<?> message, Class<?> aClass) {
		if (!aClass.equals(PubsubMessage.class)) {
			throw new MessageConversionException(
					"This converter can only convert to or from " + "PubsubMessages.");
		}

		PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();

		message.getHeaders().forEach((key, value) -> pubsubMessageBuilder
				.putAttributes(key, value.toString()));

		pubsubMessageBuilder
				.setData(ByteString.copyFrom(message.getPayload().toString().getBytes()));

		return pubsubMessageBuilder.build();
	}

	@Override
	public Message<?> toMessage(Object o, MessageHeaders messageHeaders) {
		if (!(o instanceof String)) {
			throw new MessageConversionException("Only String payloads are supported.");
		}

		if (messageHeaders == null) {
			return new GenericMessage<>((String) o);
		}

		return new GenericMessage<>((String) o, messageHeaders);
	}
}
