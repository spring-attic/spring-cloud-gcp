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

package com.example;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
public class WebController {

	private static final Log LOGGER = LogFactory.getLog(PubSubApplication.class);

	private final PubSubTemplate pubSubTemplate;

	private final PubSubAdmin pubSubAdmin;

	public WebController(PubSubTemplate pubSubTemplate, PubSubAdmin pubSubAdmin) {
		this.pubSubTemplate = pubSubTemplate;
		this.pubSubAdmin = pubSubAdmin;
	}

	@PostMapping("/createTopic")
	public RedirectView createTopic(@RequestParam("topicName") String topicName) {
		this.pubSubAdmin.createTopic(topicName);

		return new RedirectView("/");
	}

	@PostMapping("/createSubscription")
	public RedirectView createSubscription(@RequestParam("topicName") String topicName,
			@RequestParam("subscriptionName") String subscriptionName) {
		this.pubSubAdmin.createSubscription(subscriptionName, topicName);

		return new RedirectView("/");
	}

	@GetMapping("/postMessage")
	public RedirectView publish(@RequestParam("message") String message,
			@RequestParam("topicName") String topicName) {
		this.pubSubTemplate.publish(topicName, message, null);

		return new RedirectView("/");
	}

	@GetMapping("/subscribe")
	public RedirectView subscribe(@RequestParam("subscription") String subscriptionName) {
		this.pubSubTemplate.subscribe(subscriptionName, (pubsubMessage, ackReplyConsumer) -> {
			LOGGER.info("Message received from " + subscriptionName + " subscription. "
					+ pubsubMessage.getData().toStringUtf8());
			ackReplyConsumer.ack();
		});

		return new RedirectView("/");
	}

	@PostMapping("/deleteTopic")
	public RedirectView deleteTopic(@RequestParam("topic") String topicName) {
		this.pubSubAdmin.deleteTopic(topicName);

		return new RedirectView("/");
	}

	@PostMapping("/deleteSubscription")
	public RedirectView deleteSubscription(@RequestParam("subscription") String subscriptionName) {
		this.pubSubAdmin.deleteSubscription(subscriptionName);

		return new RedirectView("/");
	}
}
