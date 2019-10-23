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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Integration test for the Pub/Sub functional stream binder sample app.
 *
 * @author Elena Felder
 */
public class SampleAppIntegrationTest {

	/** Captures output to check that Sink application processed the message. */
	@Rule
	public OutputCaptureRule output = new OutputCaptureRule();

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"PUB/SUB-sample integration tests are disabled. Please use '-Dit.pubsub=true' "
						+ "to enable them. ",
				System.getProperty("it.pubsub"), is("true"));
	}

	@Test
	public void testSample() throws Exception {

		// Run Source app
		Map<String, Object> sourceProperties = new HashMap<>();
		sourceProperties.put("spring.cloud.stream.bindings.generateUserMessages-out-0.destination", "my-topic");
		sourceProperties.put("spring.cloud.function.definition", "generateUserMessages");

		SpringApplicationBuilder sourceBuilder = new SpringApplicationBuilder(
				FunctionalSourceApplication.class)
				.resourceLoader(new PropertyRemovingResourceLoader())
				.properties(sourceProperties);
		sourceBuilder.run();


		//Run Sink app
		Map<String, Object> sinkProperties = new HashMap<>();
		sinkProperties.put("spring.cloud.stream.function.bindings.logUserMessage-in-0", "input");
		sinkProperties.put("spring.cloud.stream.bindings.input.destination", "my-topic");
		sinkProperties.put("spring.cloud.function.definition", "logUserMessage");
		sinkProperties.put("spring.cloud.stream.bindings.input.group", "my-group");
		sinkProperties.put("server.port", "8081");

		SpringApplicationBuilder sinkBuilder = new SpringApplicationBuilder(FunctionalSinkApplication.class)
			.resourceLoader(new PropertyRemovingResourceLoader())
			.properties(sinkProperties);
		sinkBuilder.run();


		// Post message to Source over HTTP.
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		String message = "test message " + UUID.randomUUID();
		map.add("messageBody", message);
		map.add("username", "integration-test-user");

		RestTemplate restTemplate = new RestTemplate();

		URI redirect = restTemplate.postForLocation("http://localhost:8080/postMessage", map);
		assertThat(redirect.toString()).isEqualTo("http://localhost:8080/index.html");

		Awaitility.await().atMost(10, TimeUnit.SECONDS)
				.until(() -> this.output.getOut()
						.contains("New message received from integration-test-user: " + message));

	}

	/**
	 * Removes both /application.properties files coming from the two apps.
	 */
	static class PropertyRemovingResourceLoader extends DefaultResourceLoader {
		@Override
		public Resource getResource(String location) {
			if (location.contains("classpath:/application.properties")) {
				return null;
			}

			return super.getResource(location);
		}
	}

}
