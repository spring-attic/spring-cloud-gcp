package org.springframework.cloud.gcp.pubsub.integration.outbound;

import com.google.pubsub.v1.PubsubMessage;
import org.springframework.cloud.gcp.pubsub.integration.converters.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.support.GenericMessage;

/**
 * Created by joaomartins on 5/19/17.
 */
public class PubSubMessagingTemplate<D extends MessageChannel>
    extends AbstractMessageSendingTemplate<D> {

  public PubSubMessagingTemplate() {
    setMessageConverter(new SimpleMessageConverter());
  }

  @Override
  protected void doSend(D destination, Message<?> message) {
    // Make the message conversion here while we can and put it inside another SI message.
    MessageConverter converter = getMessageConverter();

    if (converter == null) {
      throw new MessageConversionException("No converter exists.");
    }

    destination.send(new GenericMessage<>(converter.fromMessage(message, PubsubMessage.class)));
  }
}
