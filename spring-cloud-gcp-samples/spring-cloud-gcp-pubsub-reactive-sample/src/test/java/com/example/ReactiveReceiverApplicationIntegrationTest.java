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

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for the Reactive Pub/Sub sample application.
 *
 * @author Elena Felder
 *
 * @since 1.2
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = ReactiveReceiverApplication.class)
public class ReactiveReceiverApplicationIntegrationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate testRestTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat("PUB/SUB-sample integration tests disabled. Use '-Dit.pubsub=true' to enable them.",
				System.getProperty("it.pubsub"), is("true"));
	}

	@Test
	public void testSample() throws UnsupportedEncodingException {
		String messagePostingUrl = UriComponentsBuilder.newInstance()
			.scheme("http")
			.host("localhost")
			.port(this.port)
			.path("/postMessage")
			.toUriString();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("message", "reactive test msg");
		map.add("count", "2");

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

		testRestTemplate.postForObject(messagePostingUrl, request, String.class);

		List<String> streamedMessages = WebClient.create("http://localhost:" + this.port).get()
			.uri("/getmessages")
			.accept(MediaType.TEXT_EVENT_STREAM)
			.retrieve()
			.bodyToFlux(String.class)
			.limitRequest(2)
			.collectList().block(Duration.ofSeconds(10));

		assertThat(streamedMessages).containsExactlyInAnyOrder("reactive test msg 0", "reactive test msg 1");
	}

}
