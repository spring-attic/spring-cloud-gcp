package org.springframework.cloud.gcp.pubsub.integration.converters;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.support.GenericMessage;

/**
 * Created by joaomartins on 5/19/17.
 */
public class SimpleMessageConverter implements MessageConverter {

  @Override
  public Object fromMessage(Message<?> message, Class<?> aClass) {
    if (!aClass.equals(PubsubMessage.class)) {
      throw new MessageConversionException("This converter can only convert to or from "
          + "PubsubMessages.");
    }

    PubsubMessage.Builder pubsubMessageBuilder = PubsubMessage.newBuilder();

    message.getHeaders().forEach(
        (key, value) -> pubsubMessageBuilder.putAttributes(key, value.toString())
    );

    pubsubMessageBuilder.setData(ByteString.copyFrom(message.getPayload().toString().getBytes()));

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
