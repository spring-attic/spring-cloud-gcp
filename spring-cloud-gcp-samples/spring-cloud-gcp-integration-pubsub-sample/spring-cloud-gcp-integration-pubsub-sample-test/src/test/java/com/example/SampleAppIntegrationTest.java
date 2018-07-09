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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;

import org.apache.commons.io.output.TeeOutputStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */

/*
 * This tests verifies that the pubsub-integration-sample works.
 */

public class SampleAppIntegrationTest {

	private RestTemplate restTemplate = new RestTemplate();

	private static PrintStream systemOut;

	private static ByteArrayOutputStream baos;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"PUB/SUB-sample integration tests are disabled. Please use '-Dit.pubsub=true' "
						+ "to enable them. ",
				System.getProperty("it.pubsub"), is("true"));

		systemOut = System.out;
		baos = new ByteArrayOutputStream();
		TeeOutputStream out = new TeeOutputStream(systemOut, baos);
		System.setOut(new PrintStream(out));
	}

	@AfterClass
	public static void bringBack() {
		System.setOut(systemOut);
	}

	@Test
	public void testSample() throws Exception {

		SpringApplicationBuilder sender = new SpringApplicationBuilder(SenderApplication.class)
				.properties("server.port=8082");
		sender.run();

		SpringApplicationBuilder receiver = new SpringApplicationBuilder(ReceiverApplication.class)
				.properties("server.port=8081");
		receiver.run();

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		String message = "test message " + UUID.randomUUID();
		map.add("message", message);

		this.restTemplate.postForObject("http://localhost:8082/postMessage", map, String.class);

		boolean messageReceived = false;
		for (int i = 0; i < 100; i++) {
			if (baos.toString().contains("Message arrived! Payload: " + message)) {
				messageReceived = true;
				break;
			}
			Thread.sleep(100);
		}
		assertThat(messageReceived).isTrue();
	}
}
