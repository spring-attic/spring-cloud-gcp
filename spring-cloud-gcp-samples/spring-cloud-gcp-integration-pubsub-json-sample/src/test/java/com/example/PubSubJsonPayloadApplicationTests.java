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

import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import org.awaitility.Duration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedBasicAcknowledgeablePubsubMessage;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { PubSubJsonPayloadApplication.class })
public class PubSubJsonPayloadApplicationTests {

	private static final String SUBSCRIPTION_NAME = "json-payload-sample-subscription";

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private PubSubTemplate pubSubTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"PUB/SUB-sample integration tests are disabled. Please use '-Dit.pubsub=true' "
						+ "to enable them. ",
				System.getProperty("it.pubsub"), is("true"));
	}

	@Test
	public void testReceivesJsonPayload() {
		TestMessageConsumer messageConsumer = new TestMessageConsumer();
		pubSubTemplate.subscribeAndConvert(SUBSCRIPTION_NAME, messageConsumer, Person.class);

		ImmutableMap<String, String> params = ImmutableMap.of(
				"name", "Bob",
				"age", "25");
		this.testRestTemplate.postForObject(
				"/createPerson?name={name}&age={age}", null, String.class, params);

		await().atMost(Duration.TEN_SECONDS).until(() -> messageConsumer.isMessageProcessed());
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
			assertThat(person.age).isEqualTo(25);
			messageHasBeenProcessed = true;
		}

		public boolean isMessageProcessed() {
			return messageHasBeenProcessed;
		}
	}
}
