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

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * @author Dmitry Solomakha
 */

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SenderIntegrationTest {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpContextAutoConfiguration.class,
					GcpPubSubAutoConfiguration.class));

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"PUB/SUB-sample integration tests are disabled. Please use '-Dit.pubsub=true' "
						+ "to enable them. ",
				System.getProperty("it.pubsub"), is("true"));
	}

	@Test
	public void testSample() throws Exception {

		SpringApplicationBuilder sender = new SpringApplicationBuilder(SenderApplication.class)
				.properties("server.port=8082");
		sender.run();

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("message", "test message 123");

		this.restTemplate.postForObject("/postMessage", map, String.class);

		Thread.sleep(2000);

		this.contextRunner.run(context -> {
			PubSubTemplate pubSubTemplate = context.getBean(PubSubTemplate.class);
			List<AcknowledgeablePubsubMessage> messages;

			boolean messageReceived = false;
			for (int i = 0; i < 20; i++) {
				messages = pubSubTemplate.pull("exampleSubscription", 10, true);
				messages.forEach(AcknowledgeablePubsubMessage::ack);

				if (messages.stream()
						.anyMatch(m -> m.getMessage().getData().toStringUtf8().equals("\"test message 123\""))) {
					messageReceived = true;
					break;
				}
				Thread.sleep(100);
			}
			assertThat(messageReceived).isTrue();
		});

	}
}
