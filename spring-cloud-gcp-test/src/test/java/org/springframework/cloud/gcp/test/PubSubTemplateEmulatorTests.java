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

package org.springframework.cloud.gcp.test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import org.assertj.core.api.Assumptions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubEmulatorAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.PubSubAdmin;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.test.pubsub.PubSubEmulator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pub/Sub emulator tests.
 *
 * @author Dmitry Solomakha
 */
public class PubSubTemplateEmulatorTests {

	/**
	* Emulator class rule.
	*/
	@ClassRule
	public static EmulatorRule emulator = new EmulatorRule(new PubSubEmulator());

	@BeforeClass
	public static void enableTests() {
		Assumptions.assumeThat(System.getProperty("it.emulator"))
				.as("Pub/Sub emulator tests are disabled. "
						+ "Please use '-Dit.emulator=true' to enable them. ")
				.isEqualTo("true");
	}

	@Test
	public void testCreatePublishPullNextAndDelete() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withPropertyValues("spring.cloud.gcp.pubsub.subscriber.max-ack-extension-period=0")
				.withPropertyValues("spring.cloud.gcp.pubsub.emulator-host=" + emulator.getEmulatorHostPort())
				.withConfiguration(AutoConfigurations.of(GcpContextAutoConfiguration.class,
						GcpPubSubAutoConfiguration.class, GcpPubSubEmulatorAutoConfiguration.class));

		contextRunner.run((context) -> {
			TransportChannelProvider transportChannelProvider = context.getBean(TransportChannelProvider.class);
			assertThat(transportChannelProvider.getTransportChannel().toString()).contains("target=dns:///localhost:8085");

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
		});
	}
}
