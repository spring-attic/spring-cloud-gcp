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

import java.util.ArrayList;
import java.util.List;

import com.example.SenderConfiguration.PubSubPersonGateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Provides REST endpoint allowing you to send JSON payloads to a sample Pub/Sub topic for
 * demo.
 *
 * @author Daniel Zou
 */
@RestController
public class WebController {

	private final PubSubPersonGateway pubSubPersonGateway;

	@Autowired
	@Qualifier("ProcessedPersonsList")
	private ArrayList<Person> processedPersonsList;

	public WebController(PubSubPersonGateway pubSubPersonGateway) {
		this.pubSubPersonGateway = pubSubPersonGateway;
	}

	@PostMapping("/createPerson")
	public RedirectView createUser(@RequestParam("name") String name, @RequestParam("age") int age) {
		Person person = new Person(name, age);
		this.pubSubPersonGateway.sendPersonToPubSub(person);
		return new RedirectView("/");
	}

	@GetMapping("/listPersons")
	public List<Person> listPersons() {
		return this.processedPersonsList;
	}
}
