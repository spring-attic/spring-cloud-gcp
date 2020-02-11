/*
 * Copyright 2017-2020 the original author or authors.
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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Application.class)
public class SecretManagerSampleTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat(
				"Secret Manager integration tests are disabled. "
						+ "Please use '-Dit.secretmanager=true' to enable them.",
				System.getProperty("it.secretmanager"), is("true"));
	}

	@Test
	public void testApplicationStartup() {
		ResponseEntity<String> response = this.testRestTemplate.getForEntity("/", String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).isEqualTo(
				"<h1>Secret Manager Sample Application</h1>"
						+ "The secret property is: Hello world.<br/>"
						+ "You can also access secrets using @Value: Hello world.<br/>");
	}
}
