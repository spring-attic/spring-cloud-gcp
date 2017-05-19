package org.springframework.cloud.gcp.pubsub.integration.outbound;

import com.google.cloud.pubsub.spi.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import java.io.IOException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;

/**
 * Created by joaomartins on 5/19/17.
 */
public class PubSubOutboundChannelAdapter extends AbstractMessageHandler {

  private String projectId;

  private Publisher publisher;

  public PubSubOutboundChannelAdapter(String projectId, String topicName) throws IOException {
    publisher = Publisher.defaultBuilder(TopicName.create(
        projectId, topicName)).build();
  }

  @Override
  protected void handleMessageInternal(Message<?> message) throws Exception {
    if (!(message.getPayload() instanceof PubsubMessage)) {
      throw new IllegalArgumentException("This channel adapter cannot send messages other than"
          + "PubsubMessages.");
    }
    publisher.publish((PubsubMessage) message.getPayload());
  }
}
