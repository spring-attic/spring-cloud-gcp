/*
 * Copyright 2017-2018 the original author or authors.
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
import reactor.core.publisher.Sinks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot application for running the Spring Cloud Stream source.
 *
 * <p>This class bootstraps the Spring Boot application and creates the {@link Sinks.Many}
 * bean that is used for communication between {@link FrontendController} and {@link Source}.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@SpringBootApplication
public class FunctionalSourceApplication {

	/**
	 * Allows {@link Source} to subscribe to {@link UserMessage} instances from front-end.
	 * @return {@link Sinks.Many} used for passing {@link UserMessage} objects.
	 */
	@Bean
	public Sinks.Many<UserMessage> postOffice() {
		return Sinks.many().unicast().onBackpressureBuffer();
	}

	public static void main(String[] args) {
		SpringApplication.run(FunctionalSourceApplication.class, args);
	}
}
