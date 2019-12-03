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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = FirestoreSampleApplication.class)
@TestPropertySource("classpath:application-test.properties")
public class FirestoreSampleApplicationTests {

	@Autowired
	FirestoreTemplate firestoreTemplate;

	@Autowired
	TestRestTemplate restTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat("Firestore Spring Data tests are "
				+ "disabled. Please use '-Dit.firestore=true' to enable them. ",
				System.getProperty("it.firestore"), is("true"));
	}

	@Before
	public void cleanupEnvironment() {
		firestoreTemplate.deleteAll(User.class).block();
	}

	@Test
	public void saveUserTest() {
		User[] users = restTemplate.getForObject("/users", User[].class);
		assertThat(users).isEmpty();

		sendPostRequestForUser("Alpha", 49);
		sendPostRequestForUser("Beta", 23);
		sendPostRequestForUser("Delta", 49);

		users = restTemplate.getForObject("/users", User[].class);
		List<String> names = Arrays.stream(users).map(User::getName).collect(Collectors.toList());
		assertThat(names).containsExactlyInAnyOrder("Alpha", "Beta", "Delta");

		users = restTemplate.getForObject("/users/age?age=49", User[].class);
		List<String> filterNames = Arrays.stream(users).map(User::getName).collect(Collectors.toList());
		assertThat(filterNames).containsExactlyInAnyOrder("Alpha", "Delta");
	}

	/**
	 * Sends a POST request to the server which will create a new User in Firestore.
	 */
	private void sendPostRequestForUser(String name, int age) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
		map.add("name", name);
		map.add("age", age);

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(map, headers);
		this.restTemplate.postForEntity("/users/saveUser", request, String.class);
	}
}
