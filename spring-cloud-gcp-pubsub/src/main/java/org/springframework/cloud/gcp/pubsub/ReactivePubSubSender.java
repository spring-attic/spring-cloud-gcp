package org.springframework.cloud.gcp.pubsub;


import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.messaging.Message;


/**
 * @author Vinicius Carvalho
 */
public interface ReactivePubSubSender extends PubSender<Mono<String>, Flux<String>, Flux<? extends Message>>{

}
