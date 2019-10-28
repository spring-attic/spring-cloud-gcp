/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.pubsub.integration.inbound;

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.gcp.pubsub.core.subscriber.PubSubSubscriberOperations;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.PubSubHeaderMapper;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.endpoint.AbstractFetchLimitingMessageSource;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A <a href="https://cloud.google.com/pubsub/docs/pull#pubsub-pull-messages-sync-java">PubSub
 * Synchronous pull</a> implementation of {@link AbstractMessageSource}.
 *
 * @author Eric Ngeo
 * @since 1.2
 */
public class PubSubBatchMessageSource extends AbstractFetchLimitingMessageSource<Object> {

  private final String subscriptionName;

  private final PubSubSubscriberOperations pubSubSubscriberOperations;

  private AckMode ackMode = AckMode.AUTO;

  private HeaderMapper<Map<String, String>> headerMapper = new PubSubHeaderMapper();

  private boolean blockOnPull;

  public PubSubBatchMessageSource(PubSubSubscriberOperations pubSubSubscriberOperations,
      String subscriptionName) {
    Assert.notNull(pubSubSubscriberOperations, "Pub/Sub subscriber template can't be null.");
    Assert.notNull(subscriptionName, "Pub/Sub subscription name can't be null.");
    this.pubSubSubscriberOperations = pubSubSubscriberOperations;
    this.subscriptionName = subscriptionName;
  }

  public void setAckMode(AckMode ackMode) {
    Assert.notNull(ackMode, "The acknowledgement mode can't be null.");
    this.ackMode = ackMode;
  }

  public void setHeaderMapper(HeaderMapper<Map<String, String>> headerMapper) {
    Assert.notNull(headerMapper, "The header mapper can't be null.");
    this.headerMapper = headerMapper;
  }

  /**
   * Instructs synchronous pull to wait until at least one message is available.
   *
   * @param blockOnPull whether to block until a message is available
   */
  public void setBlockOnPull(boolean blockOnPull) {
    this.blockOnPull = blockOnPull;
  }

  /**
   * Provides a List of messages with max size of provided fetchSize. Returns null if
   * <p>Messages are received from Pub/Sub by synchronous pull, in batches determined
   * by {@code fetchSize}.
   *
   * @param fetchSize number of messages to fetch from Pub/Sub.
   * @return {@link Message} wrapper containing the original message.
   */
  @Override
  protected Object doReceive(int fetchSize) {
    Integer maxMessages = (fetchSize > 0) ? fetchSize : 1;

    List<AcknowledgeablePubsubMessage> messages = this.pubSubSubscriberOperations
        .pull(this.subscriptionName, maxMessages, !this.blockOnPull);
    if (messages.isEmpty()) {
      return null;
    }
    return getMessageBuilderFactory()
        .withPayload(messages)
        .copyHeaders(ImmutableMap.of(IntegrationMessageHeaderAccessor.ACKNOWLEDGMENT_CALLBACK,
            new PubSubBatchAcknowledgementCallback(messages, this.ackMode)));
  }

  @Override
  public String getComponentType() {
    return "gcp-pubsub:batching-message-source";
  }

}
