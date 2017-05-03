package org.springframework.cloud.gcp.pubsub.converters;

import com.google.pubsub.v1.PubsubMessage;

import org.springframework.messaging.Message;

/**
 * @author Vinicius Carvalho
 */
public class PubSubMessagingConverter {

	public Message toInternal(PubsubMessage pubsubMessage){

		return null;
	}

	public PubsubMessage fromInternal(Message message) {
		return null;
	}
}
