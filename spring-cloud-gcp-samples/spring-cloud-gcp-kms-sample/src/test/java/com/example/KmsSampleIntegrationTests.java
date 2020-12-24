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
		classes = KmsApplication.class)
public class KmsSampleIntegrationTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat("KMS integration tests are disabled. "
						+ "Please use '-Dit.kms=true' to enable them.",
				System.getProperty("it.kms"), is("true"));
	}

	@Test
	public void testApplicationStartup() {
		ResponseEntity<String> response = this.testRestTemplate.getForEntity("/", String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
	}

	@Test
	public void testEncrypt() {
		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		params.add("keyId", "spring-cloud-gcp-ci/us-east1/integration-test-key-ring/test-key");
		params.add("text", "12345");

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, new HttpHeaders());

		ResponseEntity<String> response = this.testRestTemplate.postForEntity("/encrypt", request, String.class);
		System.out.println(response.getBody());
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
	}

	@Test
	public void testDecrypt() {
		MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
		params.add("keyId", "spring-cloud-gcp-ci/us-east1/integration-test-key-ring/test-key");
		params.add("encryptedText", "CiQA9oGpAZWS7YfHvtvl3gD42KD3cpaPtVb/OvaQvx/T5wikp2sSLgDPaDHEgKQWhD5HPNKqYiFGDP5SofmM0Nec5q/AyyYgRUBimEmG8i6vrpiEf9o=");

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, new HttpHeaders());

		ResponseEntity<String> response = this.testRestTemplate.postForEntity("/decrypt", request, String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
	}

}
