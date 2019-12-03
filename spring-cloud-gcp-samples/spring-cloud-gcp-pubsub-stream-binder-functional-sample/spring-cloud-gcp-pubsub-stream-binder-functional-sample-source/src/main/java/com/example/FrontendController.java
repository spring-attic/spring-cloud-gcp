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

import com.example.model.UserMessage;
import reactor.core.publisher.EmitterProcessor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the user message form submission.
 *
 * <p>The {@link EmitterProcessor} is used to locally send messages to {@link Source}.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@RestController
public class FrontendController {

	@Autowired
	private EmitterProcessor<UserMessage> postOffice;

	@PostMapping("/postMessage")
	public RedirectView sendMessage(
			@RequestParam("messageBody") String messageBody,
			@RequestParam("username") String username) {
		UserMessage userMessage = new UserMessage(messageBody, username);
		postOffice.onNext(userMessage);

		return new RedirectView("index.html");
	}
}
