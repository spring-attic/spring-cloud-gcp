/*
 *  Copyright 2018 original author or authors.
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

package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;

/**
 * @author João André Martins
 */
@EnableBinding(Sink.class)
public class SinkExample {

	private static final Log LOGGER = LogFactory.getLog(SinkExample.class);

	@StreamListener(Sink.INPUT)
	public void handleMessage(UserMessage userMessage) {
		LOGGER.info("New message received from " + userMessage.getUsername() + ": " + userMessage.getBody() +
				" at " + userMessage.getCreatedAt());
	}
}
