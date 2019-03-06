/*
 * Copyright 2017-2019 the original author or authors.
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

import java.nio.charset.Charset;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.reactive.PubSubReactiveFactory;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Elena Felder
 * @since 1.2
 */
@RestController
public class ReactiveController {

	@Autowired
	PubSubTemplate template;

	@GetMapping(value = "/getmessages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<? super String> getMessages() {

		// TODO: autoconfigure and inject.
		PubSubReactiveFactory factory = new PubSubReactiveFactory(this.template);

		Publisher<AcknowledgeablePubsubMessage> flux
				= factory.createPolledPublisher("reactiveSubscription", 1000);

		return Flux.from(flux)
				//.doOnSubscribe(s -> s.request(Long.MAX_VALUE))
				.doOnNext(message -> message.ack())  // remove after merging master
				//.limitRequest(5)	// example of finite flux, as requested by client
				.map(message -> new String(
						message.getPubsubMessage().getData().toByteArray(),
						Charset.defaultCharset()));
	}
}
