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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

/**
 * Sample application for Spring Data Firestore.
 *
 * @author Daniel Zou
 */
@SpringBootApplication
public class FirestoreSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(FirestoreSampleApplication.class, args);
	}

	@Bean
	public RouterFunction<ServerResponse> indexRouter(
			@Value("classpath:/static/index.html") final Resource indexHtml) {

		// Serve static index.html at root, for convenient message publishing.
		return route(
				GET("/"),
				request -> ok().contentType(MediaType.TEXT_HTML).bodyValue(indexHtml));
	}
}
