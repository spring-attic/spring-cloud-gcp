package org.springframework.cloud.gcp.pubsub;


import java.util.Map;

import org.springframework.messaging.Message;

/**
 * @author Vinicius Carvalho
 */
public interface PubSender<S,M,F> {

	S send(Object payload, String destination);
	S send(Object payload, String destination, Map<String,Object> headers);
	S send(Message<?> message, String destination);

	M sendAll(F messages, String destination);
}
