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

package com.example.app

import com.example.data.Person
import com.example.data.PersonRepository
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.view.RedirectView

/**
 * Creates endpoints for registering and displaying users.
 *
 * @author Daniel Zou
 *
 * @since 1.1
 */
@RestController
class Controller(val pubSubTemplate: PubSubTemplate, val personRepository: PersonRepository) {

	val REGISTRATION_TOPIC = "registrations"

	@PostMapping("/registerPerson")
	fun registerPerson(
			@RequestParam("firstName") firstName: String,
			@RequestParam("lastName") lastName: String,
			@RequestParam("email") email: String): RedirectView {

		pubSubTemplate.publish(REGISTRATION_TOPIC, Person(firstName, lastName, email))
		return RedirectView("/")
	}

	@GetMapping("/registrants")
	fun getRegistrants(): ModelAndView {
		val personsList = personRepository.findAll().toList()

		return ModelAndView("registrants", mapOf("personsList" to personsList))
	}
}
