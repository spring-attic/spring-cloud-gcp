package org.springframework.cloud.gcp.pubsub;

import java.util.Collection;

import org.springframework.messaging.Message;

/**
 * @author Vinicius Carvalho
 */
public interface BlockingPubSender extends PubSender<String,Iterable<String>,Collection<? extends Message>> {
}
