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

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Web app for sample app.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
@RestController
public class WebController {

	private final SenderApplication.PubSubOutboundGateway messagingGateway;

	public WebController(SenderApplication.PubSubOutboundGateway messagingGateway) {
		this.messagingGateway = messagingGateway;
	}

	/**
	 * Posts a message to a Google Cloud Pub/Sub topic, through Spring's messaging
	 * gateway, and redirects the user to the home page.
	 * @param message the message that will be posted to the Pub/Sub topic, with a
	 * parenthesized position suffix
	 * @param numTimes how many copies of the message to send
	 * @return the redirected view for the request
	 */
	@PostMapping("/postMessage")
	public RedirectView postMessage(@RequestParam("message") String message,
			@RequestParam("times") int numTimes) {
		for (int i = 0; i < numTimes; i++) {
			this.messagingGateway.sendToPubSub(message + "(" + i + ")");
		}
		return new RedirectView("/");
	}

}
