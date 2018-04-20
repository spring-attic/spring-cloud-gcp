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

			assertThat(pubSubAdmin.getTopic("tarkus")).isNull();
			assertThat(pubSubAdmin.getSubscription("zatoichi"))
					.isNull();
			pubSubAdmin.createTopic("tarkus");
			pubSubAdmin.createSubscription("zatoichi", "tarkus");

			Map<String, String> headers = new HashMap<>();
			headers.put("cactuar", "tonberry");
			headers.put("fujin", "raijin");
			pubSubTemplate.publish("tarkus", "tatatatata", headers);
			PubsubMessage pubsubMessage = pubSubTemplate.pullNext("zatoichi");

			assertThat(pubsubMessage.getData()).isEqualTo(ByteString.copyFromUtf8("tatatatata"));
			assertThat(pubsubMessage.getAttributesCount()).isEqualTo(2);
			assertThat(pubsubMessage.getAttributesOrThrow("cactuar")).isEqualTo("tonberry");
			assertThat(pubsubMessage.getAttributesOrThrow("fujin")).isEqualTo("raijin");

			assertThat(pubSubAdmin.getTopic("tarkus")).isNotNull();
			assertThat(pubSubAdmin.getSubscription("zatoichi")).isNotNull();
			assertThat(pubSubAdmin.listTopics().size()).isEqualTo(1);
			assertThat(pubSubAdmin.listSubscriptions().size()).isEqualTo(1);
			pubSubAdmin.deleteSubscription("zatoichi");
			pubSubAdmin.deleteTopic("tarkus");
			assertThat(pubSubAdmin.getTopic("tarkus")).isNull();
			assertThat(pubSubAdmin.getSubscription("zatoichi")).isNull();
			assertThat(pubSubAdmin.listTopics().size()).isEqualTo(0);
			assertThat(pubSubAdmin.listSubscriptions().size()).isEqualTo(0);
		});
	}
}
