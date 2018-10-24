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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * This application demonstrates serializing a POJO into JSON when publishing to a topic
 * and then deserializing the object from JSON for the subscriber.
 *
 * @author Daniel Zou
 */
@RestController
public class WebController {

	private static final Log LOGGER = LogFactory.getLog(PubSubJsonPayloadApplication.class);

	private static final String TOPIC_NAME = "exampleTopic";

	private static final String SUBSCRIPTION_NAME = "exampleSubscription";

	private final PubSubTemplate pubSubTemplate;

	public WebController(PubSubTemplate pubSubTemplate) {
		this.pubSubTemplate = pubSubTemplate;
		this.pubSubTemplate.setMessageConverter(new JacksonPubSubMessageConverter(new ObjectMapper()));
		this.pubSubTemplate.subscribeAndConvert(
				SUBSCRIPTION_NAME, (message) -> {
					LOGGER.info(message.getPayload());
					message.ack();
				}, Person.class);
	}

	@PostMapping("/createPerson")
	public RedirectView createUser(@RequestParam("name") String name, @RequestParam("age") int age) {
		Person person = new Person(name, age);
		this.pubSubTemplate.publish(TOPIC_NAME, person);
		return new RedirectView("/");
	}
}
