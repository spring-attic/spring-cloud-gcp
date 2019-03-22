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
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

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
public class ReactiveReceiverApplicationTests {

	@LocalServerPort
	private int port;


	@Autowired
	private TestRestTemplate testRestTemplate;

	@Test
	public void testSample() throws UnsupportedEncodingException {
		String url = UriComponentsBuilder.newInstance()
			.scheme("http")
			.host("localhost")
			.port(this.port)
			.path("/postMessage")
			.queryParam("message", "reactive test msg")
			.queryParam("count", 2)
			.toUriString();
		testRestTemplate.postForObject(url, null, String.class);

		List<String> messages = WebClient.create("http://localhost:" + this.port).get()
			.uri("/getmessages")
			.accept(MediaType.TEXT_EVENT_STREAM)
			.retrieve()
			.bodyToFlux(String.class)
			.limitRequest(2)
			.map(ReactiveReceiverApplicationTests::decode)
			.collectList().block(Duration.ofSeconds(10));

		assertThat(messages).containsExactlyInAnyOrder("reactive test msg 0", "reactive test msg 1");
	}

	static String decode(String s) {
		try {
			return URLDecoder.decode(s, Charset.defaultCharset().name());
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unable to decode string " + s, e);
		}
	}

}
