/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.binder.PollableMessageSource;
import org.springframework.context.annotation.Bean;

/**
 * Example of a sink for the sample app.
 *
 * @author João André Martins
 */
@EnableBinding(PollableSink.class)
public class SinkExample {

	private static final Log LOGGER = LogFactory.getLog(SinkExample.class);

	@Bean
	public ApplicationRunner poller(PollableMessageSource destIn) {
		LOGGER.info("&&&&&&&&&&&&&&&&&&&&&& initializing poller: ");

		return args -> {
			while (true) {
				try {
					System.out.println("Polling " + destIn);
					if (!destIn.poll(m -> {
						LOGGER.info("GAAAAAAH! " + m);
						// LOGGER.info("New message received from " + userMessage.getUsername() + ": " + userMessage.getBody() +
						//				" at " + userMessage.getCreatedAt());
					})) {
						Thread.sleep(5000);
					}
				}
				catch (Exception e) {
					LOGGER.info("WTF!!!!!!!!!!!!!!! ");
					break;
				}
			}
		};
	}
}

/**
 * Custom interface binding a pollable input.
 *
 * @author Elena Felder
 */
interface PollableSink {

	@Input("input")
	PollableMessageSource input();
}
