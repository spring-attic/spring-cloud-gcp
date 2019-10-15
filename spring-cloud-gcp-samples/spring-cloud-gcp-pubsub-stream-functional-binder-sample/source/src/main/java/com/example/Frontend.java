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

package com.example;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.example.model.UserMessage;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 *
 */
@Configuration
public class Frontend {

	@Bean
	@Qualifier("postOffice")
	public ConcurrentLinkedQueue<UserMessage> postOffice() {
		return new ConcurrentLinkedQueue<UserMessage>();
	}

	@Bean
	public HandlerMapping webSocketMapping() {
		ConcurrentLinkedQueue<UserMessage> postOffice = postOffice();

		Map<String, WebSocketHandler> map = new HashMap<>();
		map.put("/users", new ExampleHandler(postOffice));

		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setUrlMap(map);
		mapping.setOrder(1);
		return mapping;
	}

	@Bean
	public WebSocketHandlerAdapter handlerAdapter() {
		return new WebSocketHandlerAdapter();
	}

	private class ExampleHandler implements WebSocketHandler {

		private Jackson2JsonDecoder decoder = new Jackson2JsonDecoder();

		private ConcurrentLinkedQueue<UserMessage> postOffice;

		ExampleHandler(ConcurrentLinkedQueue<UserMessage> postOffice) {
			this.postOffice = postOffice;
		}

		@Override
		public Mono<Void> handle(WebSocketSession webSocketSession) {
			System.out.println("Yay!: " + webSocketSession);

			return webSocketSession.receive()
					.map(m -> decoder.decode(m.retain().getPayload(), ResolvableType.forClass(UserMessage.class), null,
							null))
					.cast(UserMessage.class)
					.doOnNext(u -> {
						this.postOffice.add(u);
					})
					.checkpoint("after post office")
					.then();

		}
	}
}
