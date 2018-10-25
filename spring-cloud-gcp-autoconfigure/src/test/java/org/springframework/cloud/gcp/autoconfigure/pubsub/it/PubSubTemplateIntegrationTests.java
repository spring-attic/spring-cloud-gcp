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

package org.springframework.cloud.gcp.autoconfigure.pubsub.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.awaitility.Duration;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.ConvertedAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.awaitility.Awaitility.await;

/**
 * @author João André Martins
 * @author Chengyuan Zhao
 * @author Dmitry Solomakha
 * @author Daniel Zou
 */
public class PubSubTemplateIntegrationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withPropertyValues("spring.cloud.gcp.pubsub.subscriber.max-ack-extension-period=0")
			.withConfiguration(AutoConfigurations.of(GcpContextAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class));

	@BeforeClass
	public static void enableTests() {
			assumeThat(System.getProperty("it.pubsub")).isEqualTo("true");
	}

	@Test
	public void testCreatePublishPullNextAndDelete() {
		this.contextRunner.run(context -> {
			PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
			PubSubTemplate pubSubTemplate = context.getBean(PubSubTemplate.class);

			String topicName = "tarkus_" + UUID.randomUUID();
			String subscriptionName = "zatoichi_" + UUID.randomUUID();

			assertThat(pubSubAdmin.getTopic(topicName)).isNull();
			assertThat(pubSubAdmin.getSubscription(subscriptionName))
					.isNull();
			pubSubAdmin.createTopic(topicName);
			pubSubAdmin.createSubscription(subscriptionName, topicName);

			Map<String, String> headers = new HashMap<>();
			headers.put("cactuar", "tonberry");
			headers.put("fujin", "raijin");
			pubSubTemplate.publish(topicName, "tatatatata", headers).get();
			PubsubMessage pubsubMessage = pubSubTemplate.pullNext(subscriptionName);

			assertThat(pubsubMessage.getData()).isEqualTo(ByteString.copyFromUtf8("tatatatata"));
			assertThat(pubsubMessage.getAttributesCount()).isEqualTo(2);
			assertThat(pubsubMessage.getAttributesOrThrow("cactuar")).isEqualTo("tonberry");
			assertThat(pubsubMessage.getAttributesOrThrow("fujin")).isEqualTo("raijin");

			assertThat(pubSubAdmin.getTopic(topicName)).isNotNull();
			assertThat(pubSubAdmin.getSubscription(subscriptionName)).isNotNull();
			assertThat(pubSubAdmin.listTopics().stream()
					.filter(topic -> topic.getName().endsWith(topicName)).toArray().length)
							.isEqualTo(1);
			assertThat(pubSubAdmin.listSubscriptions().stream().filter(
					subscription -> subscription.getName().endsWith(subscriptionName))
					.toArray().length).isEqualTo(1);
			pubSubAdmin.deleteSubscription(subscriptionName);
			pubSubAdmin.deleteTopic(topicName);
			assertThat(pubSubAdmin.getTopic(topicName)).isNull();
			assertThat(pubSubAdmin.getSubscription(subscriptionName)).isNull();
			assertThat(pubSubAdmin.listTopics().stream()
					.filter(topic -> topic.getName().endsWith(topicName)).toArray().length)
					.isEqualTo(0);
			assertThat(pubSubAdmin.listSubscriptions().stream().filter(
					subscription -> subscription.getName().endsWith(subscriptionName))
					.toArray().length).isEqualTo(0);
		});
	}

	@Test
	public void testPullAndAck() {
		this.contextRunner.run(context -> {
			PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
			String topicName = "peel-the-paint" + UUID.randomUUID();
			String subscriptionName = "i-lost-my-head" + UUID.randomUUID();
			pubSubAdmin.createTopic(topicName);
			pubSubAdmin.createSubscription(subscriptionName, topicName, 10);

			PubSubTemplate pubSubTemplate = context.getBean(PubSubTemplate.class);

			List<Future<String>> futures = new ArrayList<>();
			futures.add(pubSubTemplate.publish(topicName, "message1"));
			futures.add(pubSubTemplate.publish(topicName, "message2"));
			futures.add(pubSubTemplate.publish(topicName, "message3"));

			futures.parallelStream().forEach(f -> {
				try {
					f.get(5, TimeUnit.SECONDS);
				}
				catch (InterruptedException | ExecutionException | TimeoutException e) {
					e.printStackTrace();
				}
			});

			List<AcknowledgeablePubsubMessage> ackableMessages = new ArrayList<>();
			Set<String> messagesSet = new HashSet<>();
			for (int i = 0; i < 5 && messagesSet.size() < 3; i++) {
				List<AcknowledgeablePubsubMessage> newMessages = pubSubTemplate.pull(subscriptionName, 4, false);
				ackableMessages.addAll(newMessages);
				messagesSet.addAll(newMessages.stream()
						.map(message -> message.getPubsubMessage().getData().toStringUtf8())
						.collect(Collectors.toList()));
			}

			assertThat(messagesSet.size()).as("check that we received all the messages").isEqualTo(3);

			ackableMessages.forEach(message -> {
				if (message.getPubsubMessage().getData().toStringUtf8().equals("message1")) {
					message.ack(); //sync call
				}
				else {
					message.nack(); //sync call
				}
			});

			Thread.sleep(11_000);
			// pull the 2 nacked messages with retries for up to 10s
			int messagesCount = 0;
			int tries = 100;
			while (messagesCount < 2 && tries > 0) {
				Thread.sleep(100);
				ackableMessages = pubSubTemplate.pull(subscriptionName, 4, true);
				ackableMessages.forEach(m -> m.ack());
				messagesCount += ackableMessages.size();
				tries--;
			}
			assertThat(messagesCount).as("check that we get both nacked messages back").isEqualTo(2);

			pubSubAdmin.deleteSubscription(subscriptionName);
			pubSubAdmin.deleteTopic(topicName);
		});
	}

	@Test
	public void testPubSubTemplateLoadsMessageConverter() {
		this.contextRunner
				.withUserConfiguration(JsonPayloadTestConfiguration.class)
				.run(context -> {
					PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
					PubSubTemplate pubSubTemplate = context.getBean(PubSubTemplate.class);

					String topicName = "json-payload-topic" + UUID.randomUUID();
					String subscriptionName = "json-payload-subscription" + UUID.randomUUID();
					pubSubAdmin.createTopic(topicName);
					pubSubAdmin.createSubscription(subscriptionName, topicName, 10);

					TestUser user = new TestUser("John", "password");
					pubSubTemplate.publish(topicName, user);

					await().atMost(Duration.TEN_SECONDS).untilAsserted(() -> {
						List<ConvertedAcknowledgeablePubsubMessage<TestUser>> messages =
								pubSubTemplate.pullAndConvert(
										subscriptionName, 1, true, TestUser.class);
						assertThat(messages).hasSize(1);

						TestUser receivedTestUser = messages.get(0).getPayload();
						assertThat(receivedTestUser.username).isEqualTo("John");
						assertThat(receivedTestUser.password).isEqualTo("password");
					});

					pubSubAdmin.deleteSubscription(subscriptionName);
					pubSubAdmin.deleteTopic(topicName);
				});
	}

	@Configuration
	static class JsonPayloadTestConfiguration {

		@Bean
		public PubSubMessageConverter pubSubMessageConverter() {
			return new JacksonPubSubMessageConverter(new ObjectMapper());
		}
	}

	static class TestUser {

		public final String username;

		public final String password;

		@JsonCreator
		TestUser(@JsonProperty("username") String username, @JsonProperty("password") String password) {
			this.username = username;
			this.password = password;
		}
	}
}
