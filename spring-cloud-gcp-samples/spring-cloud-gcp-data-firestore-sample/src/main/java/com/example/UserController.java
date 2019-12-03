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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

/**
 * Example controller demonstrating usage of Spring Data Repositories for Firestore.
 *
 * @author Daniel Zou
 */
@RestController
@RequestMapping("/users")
public class UserController {

	private final UserRepository userRepository;

	public UserController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping
	private Flux<User> getAllUsers() {
		return userRepository.findAll();
	}

	@GetMapping("/age")
	private Flux<User> getUsersByAge(@RequestParam int age) {
		return userRepository.findByAge(age);
	}

	@PostMapping("/saveUser")
	private Mono<User> saveUser(ServerWebExchange serverWebExchange) {
		return serverWebExchange.getFormData()
				.flatMap(formData -> {
					User user = new User(
							formData.getFirst("name"),
							Integer.parseInt(formData.getFirst("age")));
					return userRepository.save(user);
				});
	}
}
