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

import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Integration test for the config client with local config server.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
public class LocalSampleAppIntegrationTest {

	private RestTemplate restTemplate = new RestTemplate();

	@BeforeClass
	public static void prepare() {
		assumeThat(
			"PUB/SUB integration tests are disabled. Use '-Dit.pubsub=true' to enable.",
			System.getProperty("it.pubsub"), is("true"));

	}

	@Test
	public void testSample() {
		SpringApplicationBuilder configServer = new SpringApplicationBuilder(PubSubConfigServerApplication.class)
			.properties("");

		/*
		server.port = 8888

spring.profiles.active=native
spring.cloud.config.server.native.searchLocations=file:/tmp/config/
		 */
		SpringApplicationBuilder configClient = new SpringApplicationBuilder(PubSubConfigApplication.class);

	}

}
