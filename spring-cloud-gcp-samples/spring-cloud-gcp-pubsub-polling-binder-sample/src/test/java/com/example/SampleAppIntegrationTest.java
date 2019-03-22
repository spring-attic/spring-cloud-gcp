/*
 * Copyright 2017-2019 the original author or authors.
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

package com.example;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.output.TeeOutputStream;
import org.awaitility.Awaitility;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * These tests verifies that the pubsub-polling-binder-sample works.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.cloud.stream.bindings.input.destination=sub1",
		"spring.cloud.stream.bindings.output.destination=sub1" })
@DirtiesContext
public class SampleAppIntegrationTest {

	@Autowired
	private TestRestTemplate restTemplate;

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
		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		String message = "test message " + UUID.randomUUID();

		map.add("messageBody", message);
		map.add("username", "testUserName");

		this.restTemplate.postForObject("/newMessage", map, String.class);

		Callable<Boolean> logCheck = () -> baos.toString().contains("New message received from testUserName via polling: " + message);
		Awaitility.await().atMost(10, TimeUnit.SECONDS)
				.until(logCheck);

		assertThat(logCheck.call()).isTrue();
	}
}
