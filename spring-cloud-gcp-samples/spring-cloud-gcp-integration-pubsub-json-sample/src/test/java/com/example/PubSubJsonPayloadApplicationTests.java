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

import com.google.pubsub.v1.Subscription;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
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

	private static final String TOPIC_NAME = "json-payload-sample-topic";

	private static final String SUBSCRIPTION_NAME_PREFIX = "json-payload-sample-subscription-test-";

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private PubSubAdmin pubSubAdmin;

	@Autowired
	private PubSubTemplate pubSubTemplate;

	private String testSubscriptionName;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"PUB/SUB-sample integration tests are disabled. Please use '-Dit.pubsub=true' "
						+ "to enable them. ",
				System.getProperty("it.pubsub"), is("true"));
	}

	@Before
	public void setupTestSubscription() {
		testSubscriptionName = SUBSCRIPTION_NAME_PREFIX + UUID.randomUUID();
		pubSubAdmin.createSubscription(testSubscriptionName, TOPIC_NAME);
	}

	@After
	public void deleteTestSubscription() {
		Subscription subscription = pubSubAdmin.getSubscription(testSubscriptionName);
		if (subscription != null) {
			pubSubAdmin.deleteSubscription(testSubscriptionName);
		}
	}

	@Test
	public void testReceivesJsonPayload() {
		TestMessageConsumer messageConsumer = new TestMessageConsumer();
		this.pubSubTemplate.subscribeAndConvert(testSubscriptionName, messageConsumer, Person.class);

		ImmutableMap<String, String> params = ImmutableMap.of(
				"name", "Bob",
				"age", "25");
		this.testRestTemplate.postForObject(
				"/createPerson?name={name}&age={age}", null, String.class, params);

		await().atMost(10, TimeUnit.SECONDS).until(() -> messageConsumer.isMessageProcessed());
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
			this.messageHasBeenProcessed = true;
		}

		public boolean isMessageProcessed() {
			return this.messageHasBeenProcessed;
		}
	}
}
