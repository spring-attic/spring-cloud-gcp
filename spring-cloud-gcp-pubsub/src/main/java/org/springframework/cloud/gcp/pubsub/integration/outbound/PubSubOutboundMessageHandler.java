package org.springframework.cloud.gcp.pubsub.integration.outbound;

import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;
import org.springframework.cloud.gcp.pubsub.integration.converters.SimpleMessageConverter;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Created by joaomartins on 5/19/17.
 */
public class PubSubOutboundMessageHandler extends AbstractMessageHandler {

  private Publisher publisher;
  private MessageConverter messageConverter;

  public PubSubOutboundMessageHandler(String projectId, String topicName) throws IOException {
    this(projectId, topicName, new SimpleMessageConverter());
  }

  public PubSubOutboundMessageHandler(String projectId, String topicName,
      MessageConverter messageConverter) throws IOException {
    publisher = Publisher.defaultBuilder(TopicName.create(
        projectId, topicName)).build();
    this.messageConverter = messageConverter;
  }

  @Override
  protected void handleMessageInternal(Message<?> message) throws Exception {
//    messageConverter.toMessage(message.getPayload(), message.getHeaders());
    Object pubsubMessageObject = messageConverter.fromMessage(message, PubsubMessage.class);

    if (!(pubsubMessageObject instanceof PubsubMessage)) {
      throw new MessageConversionException("The specified converter must produce"
          + "PubsubMessages to send to Google Cloud Pub/Sub.");
    }

    publisher.publish((PubsubMessage) pubsubMessageObject);
  }
}
