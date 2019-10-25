package org.springframework.cloud.gcp.pubsub.integration.inbound;

import java.util.Collection;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.integration.acks.AcknowledgmentCallback;
import org.springframework.util.Assert;

public class PubSubBatchAcknowledgementCallback implements AcknowledgmentCallback {


  private final Collection<AcknowledgeablePubsubMessage> messages;

  private final AckMode ackMode;

  private boolean acknowledged;

  public PubSubBatchAcknowledgementCallback(Collection<AcknowledgeablePubsubMessage> messages,
      AckMode ackMode) {
    Assert.notNull(messages, "message to be acknowledged cannot be null");
    Assert.notNull(ackMode, "ackMode cannot be null");
    this.messages = messages;
    this.ackMode = ackMode;
  }

  /**
   * In {@link AckMode#AUTO_ACK} mode, nacking cannot be done through this callback.
   * <p>Use {@link org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders#ORIGINAL_MESSAGE}
   * to nack instead.
   */
  @Override
  public void acknowledge(Status status) {
    if (status == AcknowledgmentCallback.Status.ACCEPT) {
      messages.forEach(AcknowledgeablePubsubMessage::ack);
    } else if (this.ackMode == AckMode.MANUAL || this.ackMode == AckMode.AUTO) {
      messages.forEach(AcknowledgeablePubsubMessage::nack);
    }
    this.acknowledged = true;
  }

  @Override
  public boolean isAutoAck() {
    return this.ackMode != AckMode.MANUAL;
  }

  @Override
  public boolean isAcknowledged() {
    return this.acknowledged;
  }

}
