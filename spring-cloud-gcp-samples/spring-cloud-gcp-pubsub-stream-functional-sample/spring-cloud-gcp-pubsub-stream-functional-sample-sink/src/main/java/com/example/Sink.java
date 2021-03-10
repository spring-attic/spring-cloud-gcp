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

import java.util.function.Consumer;

import com.example.model.UserMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Stream sink that receives {@link UserMessage} objects and acts on them.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@Configuration
public class Sink {

	private static final Log LOGGER = LogFactory.getLog(Sink.class);

	@Bean
	public Consumer<UserMessage> logUserMessage() {
		return userMessage -> {

			LOGGER.info(String.format("New message received from %s: %s at %s",
					userMessage.getUsername(), userMessage.getBody(), userMessage.getCreatedAt()));
		};
	}

}
