package org.springframework.cloud.gcp.pubsub;

import java.util.Collection;
import java.util.Map;

import com.google.auth.Credentials;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.messaging.Message;

/**
 * @author Vinicius Carvalho
 */
public class DefaultPubSubSender extends AbstractPubSender {


	public DefaultPubSubSender(String project, Credentials credentials) {
		super(project, credentials);
	}

	@Override
	public Mono<String> send(Object payload, String destination) {
		return null;
	}

	@Override
	public Mono<String> send(Object payload, String destination, Map<String, Object> headers) {
		return null;
	}

	@Override
	public Mono<String> send(Message<?> message, String destination) {
		return null;
	}

	@Override
	public Flux<String> sendAll(Flux<? extends Message> messages, String destination) {
		return null;
	}

	@Override
	void doStart() {

	}

	@Override
	void doStop() {

	}


	class BlockingPubSubSender implements BlockingPubSender{

		@Override
		public String send(Object payload, String destination) {
			return DefaultPubSubSender.this.send(payload,destination).block();
		}

		@Override
		public String send(Object payload, String destination, Map<String, Object> headers) {
			return DefaultPubSubSender.this.send(payload,destination,headers).block();
		}

		@Override
		public String send(Message<?> message, String destination) {
			return DefaultPubSubSender.this.send(message,destination).block();
		}

		@Override
		public Iterable<String> sendAll(Collection<? extends Message> messages, String destination) {
			return DefaultPubSubSender.this.sendAll(Flux.fromIterable(messages),destination).toIterable();
		}
	}

}
