/*
 *  Copyright 2017 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

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
