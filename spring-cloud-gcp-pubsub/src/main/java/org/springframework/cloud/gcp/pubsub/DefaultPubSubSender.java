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
 */

package org.springframework.cloud.gcp.pubsub;

import java.util.Collection;
import java.util.Map;

import org.springframework.messaging.Message;

import com.google.auth.Credentials;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Vinicius Carvalho
 */
public class DefaultPubSubSender extends AbstractPubSubSender {

	public DefaultPubSubSender(String project, Credentials credentials) {
		super(project, credentials);
	}

	@Override
	public Mono<String> send(String destination, Object payload) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Mono<String> send(String destination, Object payload,
			Map<String, Object> headers) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Mono<String> send(String destination, Message<?> message) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Flux<String> sendAll(String destination, Flux<? extends Message<?>> messages) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	protected void doStart() {
	}

	@Override
	protected void doStop() {
	}

	class DefaultBlockingPubSubSender implements BlockingPubSubSender {

		@Override
		public String send(String destination, Object payload) {
			return DefaultPubSubSender.this.send(destination, payload).block();
		}

		@Override
		public String send(String destination, Object payload,
				Map<String, Object> headers) {
			return DefaultPubSubSender.this.send(destination, payload, headers).block();
		}

		@Override
		public String send(String destination, Message<?> message) {
			return DefaultPubSubSender.this.send(destination, message).block();
		}

		@Override
		public Iterable<String> sendAll(String destination,
				Collection<? extends Message<?>> messages) {
			return DefaultPubSubSender.this
					.sendAll(destination, Flux.fromIterable(messages)).toIterable();
		}
	}
}
