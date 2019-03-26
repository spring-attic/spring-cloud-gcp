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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.pubsub.core.publisher.PubSubPublisherTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.reactive.result.view.RedirectView;

/**
 * A helper controller for sending Pub/Sub messages, which can then be received by {@link ReactiveController}.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@Controller
public class MessageSenderController {

	@Autowired
	PubSubPublisherTemplate pubSubPublisherTemplate;

	@PostMapping("/postMessage")
	public RedirectView publish(@ModelAttribute("message") Message message) {

		for (int i = 0; i < message.count; i++) {
			this.pubSubPublisherTemplate.publish("exampleTopic", message.message + " " + i);
		}

		return new RedirectView("/?statusMessage=Published");
	}

	/**
	 * Convenience class encapsulating the form field values needed for publishing.
	 */
	public static class Message {

		String message;
		int count;

		public Message(String message, int count) {
			this.message = message;
			this.count = count;
		}

	}

}
