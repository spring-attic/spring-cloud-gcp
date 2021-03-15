/*
 * Copyright 2017-2018 the original author or authors.
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

import java.time.LocalDateTime;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * An example source for the sample app.
 *
 * @author Travis Tomsu
 */
@RestController
@Slf4j
public class SourceExample {

	@Autowired
	private Sinks.Many<UserMessage> postOffice;

	@PostMapping("/newMessage")
	public UserMessage sendMessage(
			@RequestParam("messageBody") String messageBody,
			@RequestParam("username") String username) {
		UserMessage userMessage = new UserMessage(messageBody, username, LocalDateTime.now());
		log.info("Publishing message from {}", username);
		this.postOffice.tryEmitNext(userMessage);
		return userMessage;
	}

	@Configuration
	public static class SourceConfig {

		@Bean
		public Sinks.Many<UserMessage> postOffice() {
			return Sinks.many().unicast().onBackpressureBuffer();
		}

		@Bean
		Supplier<Flux<UserMessage>> generateUserMessages(Sinks.Many<UserMessage> postOffice) {
			return postOffice::asFlux;
		}
	}
}
