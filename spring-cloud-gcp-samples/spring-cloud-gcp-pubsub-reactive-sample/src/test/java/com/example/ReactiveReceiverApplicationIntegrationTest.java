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

import java.io.UnsupportedEncodingException;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for the Reactive Pub/Sub sample application.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = ReactiveReceiverApplication.class)
public class ReactiveReceiverApplicationIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private WebTestClient webTestClient;

	@Autowired
	private PubSubTemplate pubSubTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat("PUB/SUB-sample integration tests disabled. Use '-Dit.pubsub=true' to enable them.",
				System.getProperty("it.pubsub"), is("true"));
	}

	@Test
	public void testSample() throws UnsupportedEncodingException {
		// clear any old messages
		System.out.println("Cleared " +
				pubSubTemplate.pullAndAck("exampleSubscription", 1000, true).size() +
				" old messages");

		webTestClient.post()
				.uri(uriBuilder -> uriBuilder
						.path("/postMessage")
						.queryParam("message", "reactive test msg")
						.queryParam("count", "2")
						.build())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.exchange();

		FluxExchangeResult<String> result = webTestClient.get()
			.uri("/getMessages")
			.accept(MediaType.TEXT_EVENT_STREAM)
			.exchange()
			.returnResult(String.class);

		StepVerifier.create(result.getResponseBody())
			.expectNext("reactive test msg 0")
			.expectNext("reactive test msg 1")
			.thenCancel()
			.verify();
	}

}
