package org.springframework.cloud.gcp.pubsub.integration.inbound;

import com.google.cloud.pubsub.spi.v1.AckReplyConsumer;
import com.google.cloud.pubsub.spi.v1.Subscriber;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.SubscriptionName;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cloud.gcp.support.GcpHeaders;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.messaging.MessageHeaders;

/**
 * Created by joaomartins on 5/19/17.
 */
public class PubSubInboundChannelAdapter extends MessageProducerSupport {

  private String projectId;
  private String subscriptionName;
  private Subscriber subscriber;

  public PubSubInboundChannelAdapter(String projectId, String subscriptionName) {
    this.projectId = projectId;
    this.subscriptionName = subscriptionName;
  }

  @Override
  protected void doStart() {
    super.doStart();

    subscriber = Subscriber.defaultBuilder(SubscriptionName.create(
        projectId, this.subscriptionName), this::receiveMessage).build();
    subscriber.startAsync();
  }

  private void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
    Map<String, Object> messageHeaders = new HashMap<>();

    message.getAttributesMap().forEach(messageHeaders::put);
    // Send the consumer downstream so user decides on when to ack/nack.
    messageHeaders.put(GcpHeaders.CONSUMER, consumer);

    sendMessage(
        getMessagingTemplate().getMessageConverter().toMessage(message.getData(),
            new MessageHeaders(messageHeaders)));
  }

  @Override
  protected void doStop() {
    if (subscriber != null) {
      subscriber.stopAsync();
    }

    super.doStop();
  }
}
