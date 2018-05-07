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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * @author João André Martins
 * @author Chengyuan Zhao
 */
public class PubSubTemplateIntegrationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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
			pubSubTemplate.publish(topicName, "tatatatata", headers);
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
}
