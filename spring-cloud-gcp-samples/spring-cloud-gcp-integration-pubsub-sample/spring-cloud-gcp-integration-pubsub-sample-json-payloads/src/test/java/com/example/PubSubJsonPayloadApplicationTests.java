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

import java.util.Map;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import org.awaitility.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { PubSubJsonPayloadApplication.class })
public class PubSubJsonPayloadApplicationTests {

	private static final String SUBSCRIPTION_NAME = "exampleSubscription";

	@LocalServerPort
	private int port;

	@Autowired
	private PubSubTemplate pubSubTemplate;

	@Autowired
	private TestRestTemplate testRestTemplate;

	private String baseUrl;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"PUB/SUB-sample integration tests are disabled. Please use '-Dit.pubsub=true' "
						+ "to enable them. ",
				System.getProperty("it.pubsub"), is("true"));
	}

	@Before
	public void setupUrl() {
		this.baseUrl = String.format("http://localhost:%s/createPerson", port);
	}

	@Test
	public void testReceiveJsonPayload() {
		TestMessageConsumer testMessageConsumer = new TestMessageConsumer();
		this.pubSubTemplate.subscribeAndConvert(SUBSCRIPTION_NAME, testMessageConsumer, Person.class);

		Map<String, String> parameters = ImmutableMap.of("name", "Bob", "age", "23");

		String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
				.queryParam("name", "Bob")
				.queryParam("age", 23)
				.toUriString();
		this.testRestTemplate.postForObject(url, parameters, String.class);

		await().atMost(Duration.TEN_SECONDS).until(() -> testMessageConsumer.isMessageProcessed());
	}

	/**
	 * Async consumer of Pub/Sub messages to verify JSON payload serialization.
	 */
	private static class TestMessageConsumer
			implements Consumer<ConvertedBasicAcknowledgeablePubsubMessage<Person>> {

		private boolean messageHasBeenProcessed;

		@Override
		public void accept(ConvertedBasicAcknowledgeablePubsubMessage<Person> message) {
			Person person = message.getPayload();
			message.ack();
			assertThat(person.name).isEqualTo("Bob");
			assertThat(person.age).isEqualTo(23);

			messageHasBeenProcessed = true;
		}

		public boolean isMessageProcessed() {
			return messageHasBeenProcessed;
		}
	}
}
