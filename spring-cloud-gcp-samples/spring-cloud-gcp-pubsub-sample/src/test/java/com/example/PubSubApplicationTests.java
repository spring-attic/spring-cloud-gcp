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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient.ListSubscriptionsPagedResponse;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminClient.ListTopicsPagedResponse;
import com.google.common.collect.ImmutableList;
import com.google.pubsub.v1.ProjectName;
import com.google.pubsub.v1.ProjectSubscriptionName;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PullRequest;
import com.google.pubsub.v1.PullResponse;
import com.google.pubsub.v1.Subscription;
import com.google.pubsub.v1.Topic;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = { PubSubApplication.class })
public class PubSubApplicationTests {

	private static final int PUBSUB_CLIENT_TIMEOUT_SECONDS = 5;

	private static final String SAMPLE_TEST_TOPIC = "pubsub-sample-test-exampleTopic";

	private static final String SAMPLE_TEST_SUBSCRIPTION1 = "pubsub-sample-test-exampleSubscription1";

	private static final String SAMPLE_TEST_SUBSCRIPTION2 = "pubsub-sample-test-exampleSubscription2";

	private static TopicAdminClient topicAdminClient;

	private static SubscriptionAdminClient subscriptionAdminClient;

	private static String projectName;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	private String appUrl;

	@BeforeClass
	public static void prepare() throws IOException {
		assumeThat(
				"PUB/SUB-sample integration tests are disabled. Please use '-Dit.pubsub=true' "
						+ "to enable them. ",
				System.getProperty("it.pubsub"), is("true"));

		projectName = ProjectName.of(ServiceOptions.getDefaultProjectId()).getProject();
		topicAdminClient = TopicAdminClient.create();
		subscriptionAdminClient = SubscriptionAdminClient.create();
	}

	@AfterClass
	public static void cleanupPubsubClients() {
		if (topicAdminClient != null) {
			topicAdminClient.close();
		}

		if (subscriptionAdminClient != null) {
			subscriptionAdminClient.close();
		}
	}

	@Before
	public void initializeAppUrl() throws IOException {
		this.appUrl = "http://localhost:" + this.port;
	}

	@Before
	@After
	public void cleanupPubsubTestResources() {
		List<String> projectTopics = getTopicNamesFromProject();
		String testTopicName = ProjectTopicName.format(projectName, SAMPLE_TEST_TOPIC);
		if (projectTopics.contains(testTopicName)) {
			topicAdminClient.deleteTopic(testTopicName);
		}

		List<String> testSubscriptions = ImmutableList.of(SAMPLE_TEST_SUBSCRIPTION1, SAMPLE_TEST_SUBSCRIPTION2);
		for (String testSubscription : testSubscriptions) {
			String testSubscriptionName = ProjectSubscriptionName.format(
					projectName, testSubscription);
			List<String> projectSubscriptions = getSubscriptionNamesFromProject();
			if (projectSubscriptions.contains(testSubscriptionName)) {
				subscriptionAdminClient.deleteSubscription(testSubscriptionName);
			}
		}
	}

	@Test
	public void testCreateAndDeleteTopicAndSubscriptions() {
		String projectTopicName = ProjectTopicName.format(projectName, SAMPLE_TEST_TOPIC);
		String projectSubscriptionName = ProjectSubscriptionName.format(projectName, SAMPLE_TEST_SUBSCRIPTION1);

		createTopic(SAMPLE_TEST_TOPIC);
		await().atMost(PUBSUB_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(
				() -> {
					List<String> projectTopics = getTopicNamesFromProject();
					assertThat(projectTopics).contains(projectTopicName);
				});

		createSubscription(SAMPLE_TEST_SUBSCRIPTION1, SAMPLE_TEST_TOPIC);
		await().atMost(PUBSUB_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(
				() -> {
					List<String> subscriptions = getSubscriptionNamesFromProject();
					assertThat(subscriptions).contains(projectSubscriptionName);
				});

		deleteSubscription(SAMPLE_TEST_SUBSCRIPTION1);
		await().atMost(PUBSUB_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(
				() -> {
					List<String> subscriptions = getSubscriptionNamesFromProject();
					assertThat(subscriptions).doesNotContain(projectSubscriptionName);
				});

		deleteTopic(SAMPLE_TEST_TOPIC);
		await().atMost(PUBSUB_CLIENT_TIMEOUT_SECONDS, TimeUnit.SECONDS).untilAsserted(
				() -> {
					List<String> projectTopics = getTopicNamesFromProject();
					assertThat(projectTopics).doesNotContain(projectTopicName);
				});
	}

	@Test
	public void testReceiveMessage() {
		createTopic(SAMPLE_TEST_TOPIC);
		createSubscription(SAMPLE_TEST_SUBSCRIPTION1, SAMPLE_TEST_TOPIC);
		postMessage("HelloWorld", SAMPLE_TEST_TOPIC);
		assertThat(getMessagesFromSubscription(SAMPLE_TEST_SUBSCRIPTION1)).containsExactly("HelloWorld");

		// After subscribing, the message will be acked by the application and no longer be present.
		subscribe(SAMPLE_TEST_SUBSCRIPTION1);
		assertThat(getMessagesFromSubscription(SAMPLE_TEST_SUBSCRIPTION1)).isEmpty();
	}

	@Test
	public void testMultiPull() {
		createTopic(SAMPLE_TEST_TOPIC);
		createSubscription(SAMPLE_TEST_SUBSCRIPTION1, SAMPLE_TEST_TOPIC);
		createSubscription(SAMPLE_TEST_SUBSCRIPTION2, SAMPLE_TEST_TOPIC);
		postMessage("HelloWorld", SAMPLE_TEST_TOPIC);
		assertThat(getMessagesFromSubscription(SAMPLE_TEST_SUBSCRIPTION1)).containsExactly("HelloWorld");
		assertThat(getMessagesFromSubscription(SAMPLE_TEST_SUBSCRIPTION2)).containsExactly("HelloWorld");

		// After multi pull, the message will be acked by both subscriptions and no longer be present.
		multiPull(SAMPLE_TEST_SUBSCRIPTION1, SAMPLE_TEST_SUBSCRIPTION2);
		assertThat(getMessagesFromSubscription(SAMPLE_TEST_SUBSCRIPTION1)).isEmpty();
		assertThat(getMessagesFromSubscription(SAMPLE_TEST_SUBSCRIPTION2)).isEmpty();
	}

	private List<String> getMessagesFromSubscription(String subscriptionName) {
		String projectSubscriptionName = ProjectSubscriptionName.format(
				projectName, subscriptionName);

		PullRequest pullRequest = PullRequest.newBuilder()
				.setMaxMessages(10)
				.setReturnImmediately(true)
				.setSubscription(projectSubscriptionName)
				.build();

		PullResponse pullResponse = subscriptionAdminClient.getStub().pullCallable().call(pullRequest);
		return pullResponse.getReceivedMessagesList().stream()
				.map(message -> message.getMessage().getData().toStringUtf8())
				.collect(Collectors.toList());
	}

	private void createTopic(String topicName) {
		String url = UriComponentsBuilder.fromHttpUrl(appUrl + "/createTopic")
				.queryParam("topicName", topicName)
				.toUriString();
		ResponseEntity<String> response = testRestTemplate.postForEntity(url, null, String.class);
	}

	private void deleteTopic(String topicName) {
		String url = UriComponentsBuilder.fromHttpUrl(appUrl + "/deleteTopic")
				.queryParam("topic", topicName)
				.toUriString();
		testRestTemplate.postForEntity(url, null, String.class);
	}

	private void createSubscription(String subscriptionName, String topicName) {
		String url = UriComponentsBuilder.fromHttpUrl(appUrl + "/createSubscription")
				.queryParam("topicName", topicName)
				.queryParam("subscriptionName", subscriptionName)
				.toUriString();
		testRestTemplate.postForEntity(url, null, String.class);
	}

	private void deleteSubscription(String subscriptionName) {
		String url = UriComponentsBuilder.fromHttpUrl(appUrl + "/deleteSubscription")
				.queryParam("subscription", subscriptionName)
				.toUriString();
		testRestTemplate.postForEntity(url, null, String.class);
	}

	private void subscribe(String subscriptionName) {
		String url = UriComponentsBuilder.fromHttpUrl(appUrl + "/subscribe")
				.queryParam("subscription", subscriptionName)
				.toUriString();
		testRestTemplate.getForEntity(url, null, String.class);
	}

	private void postMessage(String message, String topicName) {
		String url = UriComponentsBuilder.fromHttpUrl(appUrl + "/postMessage")
				.queryParam("message", message)
				.queryParam("topicName", topicName)
				.queryParam("count", 1)
				.toUriString();
		testRestTemplate.getForEntity(url, null, String.class);
	}

	private void multiPull(String subscription1, String subscription2) {
		String url = UriComponentsBuilder.fromHttpUrl(appUrl + "/multipull")
				.queryParam("subscription1", subscription1)
				.queryParam("subscription2", subscription2)
				.toUriString();
		testRestTemplate.getForEntity(url, null, String.class);
	}

	private List<String> getTopicNamesFromProject() {
		ListTopicsPagedResponse listTopicsResponse = topicAdminClient.listTopics("projects/" + projectName);
		return StreamSupport.stream(listTopicsResponse.iterateAll().spliterator(), false)
				.map(Topic::getName)
				.collect(Collectors.toList());
	}

	private List<String> getSubscriptionNamesFromProject() {
		ListSubscriptionsPagedResponse response = subscriptionAdminClient.listSubscriptions("projects/" + projectName);
		return StreamSupport.stream(response.iterateAll().spliterator(), false)
				.map(Subscription::getName)
				.collect(Collectors.toList());
	}
}
