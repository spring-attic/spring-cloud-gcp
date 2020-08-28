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
import org.springframework.cloud.gcp.secretmanager.SecretManagerTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assume.assumeThat;

@RunWith(SpringRunner.class)
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = SecretManagerApplication.class,
		properties = {"spring.config.use-legacy-processing=true"})
public class SecretManagerSampleIntegrationTests {

	private static final String SECRET_TO_DELETE = "secret-manager-sample-delete-secret";

	@Autowired
	private SecretManagerTemplate secretManagerTemplate;

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
		assertThat(response.getBody()).contains("<b>application-secret:</b> <i>Hello world.</i>");
	}

	@Test
	public void testCreateReadSecret() {
		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		params.add("secretId", "secret-manager-sample-secret");
		params.add("projectId", "");
		params.add("secretPayload", "12345");
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, new HttpHeaders());

		ResponseEntity<String> response = this.testRestTemplate.postForEntity("/createSecret", request, String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();

		response = this.testRestTemplate.getForEntity(
				"/getSecret?secretId=secret-manager-sample-secret", String.class);
		assertThat(response.getBody()).contains(
				"Secret ID: secret-manager-sample-secret | Value: 12345");
	}

	@Test
	public void testDeleteSecret() {
		secretManagerTemplate.createSecret(SECRET_TO_DELETE, "test");

		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		params.add("secretId", SECRET_TO_DELETE);
		params.add("projectId", "");
		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, new HttpHeaders());

		ResponseEntity<String> response = this.testRestTemplate.postForEntity("/deleteSecret", request, String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
	}
}
