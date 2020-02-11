/*
 * Copyright 2017-2020 the original author or authors.
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebController {

	@Autowired
	private Environment environment;

	@Autowired
	private MyAppProperties properties;

	// Application secrets can be accessed using @Value and passing in the secret name.
	// Note that the secret name is prefixed with "secrets" because of the prefix setting in
	// bootstrap.properties.
	@Value("${secrets.application-secret}")
	private String applicationSecretValue;

	@GetMapping("/")
	public String getRoot() {
		// In practice, you never want to print your secrets as plaintext.
		return "<h1>Secret Manager Sample Application</h1>"
				+ "The secret property is: " + properties.getApplicationSecret() + "<br/>"
				+ "You can also access secrets using @Value: " + applicationSecretValue + "<br/>";
	}
}
