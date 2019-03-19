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

package org.springframework.cloud.gcp.logging.it;

import java.time.LocalDateTime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webapp used for Logging integration tests.
 *
 * @author Daniel Zou
 */
@RestController
@SpringBootApplication
public class LoggingTestApplication {

	private static final Log LOGGER = LogFactory.getLog(StackdriverLoggingIntegrationTests.class);

	static final LocalDateTime NOW = LocalDateTime.now();

	@GetMapping("/")
	public String log() {
		LOGGER.error("#$%^&" + NOW);
		return "Log sent.";
	}

	public static void main(String[] args) {
		SpringApplication.run(LoggingTestApplication.class, args);
	}
}
