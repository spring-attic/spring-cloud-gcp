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

import java.nio.charset.Charset;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.pubsub.reactive.PubSubReactiveFactory;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Sample controller demonstrating an HTTP endpoint acquiring data from a reactive GCP Pub/Sub stream.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@Controller
@ResponseBody
public class ReactiveController {

	@Autowired
	PubSubReactiveFactory reactiveFactory;

	@GetMapping(value = "/getmessages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> getMessages() {

		Flux<AcknowledgeablePubsubMessage> flux
				= this.reactiveFactory.poll("exampleSubscription", 1000);

		return flux
				.doOnNext(message -> {
					System.out.println("Received a message: " + message.getPubsubMessage().getMessageId());
					message.ack();
				})
				.map(message -> new String(
						message.getPubsubMessage().getData().toByteArray(),
						Charset.defaultCharset()));
	}

}
