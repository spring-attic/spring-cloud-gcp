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

package org.springframework.cloud.gcp.autoconfigure.pubsub.it;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import org.awaitility.Awaitility;
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
 * Documentation tests for Pub/Sub.
 *
 * @author Dmitry Solomakha
 */
public class PubSubTemplateDocumentationIntegrationTests {

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
		pubSubTest((PubSubTemplate pubSubTemplate, String subscriptionName, String topicName) -> {
			//tag::publish[]
			Map<String, String> headers = Collections.singletonMap("key1", "val1");
			pubSubTemplate.publish(topicName, "message", headers).get();
			//end::publish[]
			PubsubMessage pubsubMessage = pubSubTemplate.pullNext(subscriptionName);

			assertThat(pubsubMessage.getData()).isEqualTo(ByteString.copyFromUtf8("message"));
			assertThat(pubsubMessage.getAttributesCount()).isEqualTo(1);
			assertThat(pubsubMessage.getAttributesOrThrow("key1")).isEqualTo("val1");
		});
	}


	private void pubSubTest(PubSubTest pubSubTest, Class... configClass) {
		ApplicationContextRunner contextRunner = configClass.length == 0 ? this.contextRunner
				: this.contextRunner.withUserConfiguration(configClass[0]);
		contextRunner.run((context) -> {
			PubSubAdmin pubSubAdmin = context.getBean(PubSubAdmin.class);
			PubSubTemplate pubSubTemplate = context.getBean(PubSubTemplate.class);

			String subscriptionName = "test_subscription_" + UUID.randomUUID();
			String topicName = "test_topic_" + UUID.randomUUID();

			try {
				assertThat(pubSubAdmin.getTopic(topicName)).isNull();
				assertThat(pubSubAdmin.getSubscription(subscriptionName)).isNull();

				//tag::create_topic[]
				pubSubAdmin.createTopic(topicName);
				//end::create_topic[]
				//tag::create_subscription[]
				pubSubAdmin.createSubscription(subscriptionName, topicName);
				//end::create_subscription[]

				pubSubTest.run(pubSubTemplate, subscriptionName, topicName);
			}
			finally {
				//tag::list_subscriptions[]
				List<String> subscriptions = pubSubAdmin
						.listSubscriptions()
						.stream()
						.map(Subscription::getName)
						.collect(Collectors.toList());
				//end::list_subscriptions[]

				//tag::list_topics[]
				List<String> topics = pubSubAdmin
						.listTopics()
						.stream()
						.map(Topic::getName)
						.collect(Collectors.toList());
				//end::list_topics[]

				pubSubAdmin.deleteSubscription(subscriptionName);
				pubSubAdmin.deleteTopic(topicName);

				assertThat(subscriptions.stream().map(this::getLastPart)).contains(subscriptionName);
				assertThat(topics.stream().map(this::getLastPart)).contains(topicName);
			}
		});
	}

	private String getLastPart(String s) {
		String[] split = s.split("/");
		return split[split.length - 1];
	}

	@Test
	public void subscribeSimpleTest() {
		pubSubTest((PubSubTemplate pubSubTemplate, String subscriptionName, String topicName) -> {
			pubSubTemplate.publish(topicName, "message");

			Logger logger = new Logger();
			//tag::subscribe[]
			Subscriber subscriber = pubSubTemplate.subscribe(subscriptionName, (message) -> {
				logger.info("Message received from " + subscriptionName + " subscription: "
						+ message.getPubsubMessage().getData().toStringUtf8());
				message.ack();
			});
			//end::subscribe[]

			List<String> messages = logger.getMessages();
			Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> !messages.isEmpty());
			assertThat(messages)
					.containsExactly("Message received from " + subscriptionName + " subscription: message");
		});
	}

	@Test
	public void testPubSubTemplatePull() {
		pubSubTest((PubSubTemplate pubSubTemplate, String subscriptionName, String topicName) -> {

			pubSubTemplate.publish(topicName, "message");
			Logger logger = new Logger();
			await().atMost(Duration.TEN_SECONDS).untilAsserted(() -> {
				// tag::pull[]
				int maxMessages = 10;
				boolean returnImmediately = false;
				List<AcknowledgeablePubsubMessage> messages = pubSubTemplate.pull(subscriptionName, maxMessages,
						returnImmediately);
				// end::pull[]

				assertThat(messages).hasSize(1);

				// tag::pull[]

				//acknowledge the messages
				pubSubTemplate.ack(messages);

				messages.forEach(message -> logger.info(message.getPubsubMessage().getData().toStringUtf8()));

				// end::pull[]

				assertThat(logger.getMessages()).containsExactly("message");

			});

		});
	}

	@Test
	public void testPubSubTemplateLoadsMessageConverter() {
		pubSubTest((PubSubTemplate pubSubTemplate, String subscriptionName, String topicName) -> {
			// tag::json_publish[]
			TestUser user = new TestUser();
			user.setUsername("John");
			user.setPassword("password");
			pubSubTemplate.publish(topicName, user);
			// end::json_publish[]

			await().atMost(Duration.TEN_SECONDS).untilAsserted(() -> {
				// tag::json_pull[]
				int maxMessages = 1;
				boolean returnImmediately = false;
				List<ConvertedAcknowledgeablePubsubMessage<TestUser>> messages = pubSubTemplate.pullAndConvert(
						subscriptionName, maxMessages, returnImmediately, TestUser.class);
				// end::json_pull[]

				assertThat(messages).hasSize(1);

				// tag::json_pull[]

				ConvertedAcknowledgeablePubsubMessage<TestUser> message = messages.get(0);

				//acknowledge the message
				message.ack();

				TestUser receivedTestUser = message.getPayload();
				// end::json_pull[]

				assertThat(receivedTestUser.username).isEqualTo("John");
				assertThat(receivedTestUser.password).isEqualTo("password");
			});

		}, JsonPayloadTestConfiguration.class);
	}

	/**
	 * Beans for test.
	 */
	@Configuration
	static class JsonPayloadTestConfiguration {
		//tag::json_bean[]
		// Note: The ObjectMapper is used to convert Java POJOs to and from JSON.
		// You will have to configure your own instance if you are unable to depend
		// on the ObjectMapper provided by Spring Boot starters.
		@Bean
		public PubSubMessageConverter pubSubMessageConverter() {
			return new JacksonPubSubMessageConverter(new ObjectMapper());
		}
		//end::json_bean[]
	}

	/**
	 * A test JSON payload.
	 */
	// tag::json_convertible_class[]
	static class TestUser {

		String username;

		String password;

		public String getUsername() {
			return this.username;
		}

		void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return this.password;
		}

		void setPassword(String password) {
			this.password = password;
		}
	}
	// end::json_convertible_class[]

	interface PubSubTest {
		void run(PubSubTemplate pubSubTemplate, String subscription, String topic)
				throws ExecutionException, InterruptedException;
	}

	class Logger {
		List<String> messages = new ArrayList<>();

		void info(String message) {
			this.messages.add(message);
		}

		List<String> getMessages() {
			return this.messages;
		}
	}
}
