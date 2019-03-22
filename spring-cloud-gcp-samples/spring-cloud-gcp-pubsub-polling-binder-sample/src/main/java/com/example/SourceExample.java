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

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * A sample source application that constructs a {@link UserMessage} object based on a
 * HTTP request, and sends it to a PubSub topic.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@EnableBinding(Source.class)
@RestController
public class SourceExample {

	@Autowired
	private Source source;

	@PostMapping("/newMessage")
	public UserMessage sendMessage(@RequestParam("messageBody") String messageBody,
			@RequestParam("username") String username) {

		UserMessage userMessage = new UserMessage(messageBody, username, LocalDateTime.now());
		this.source.output().send(new GenericMessage<>(userMessage));
		return userMessage;
	}

}
