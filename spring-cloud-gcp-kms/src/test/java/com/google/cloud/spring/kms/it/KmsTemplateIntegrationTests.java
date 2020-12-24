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

package com.google.cloud.spring.kms.it;

import java.nio.charset.StandardCharsets;

import com.google.cloud.spring.kms.KmsTemplate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Integration tests for {@link KmsTemplate}.
 *
 * @author Emmanouil Gkatziouras
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { KmsTestConfiguration.class })
public class KmsTemplateIntegrationTests {

	@Autowired
	KmsTemplate kmsTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat(System.getProperty("it.kms"))
				.as("KMS integration tests are disabled. "
						+ "Please use '-Dit.kms=true' to enable them.")
				.isEqualTo("true");
	}

	@Test
	public void testEncryptDecryptText() {
		String kmsStr = "us-east1/integration-test-key-ring/test-key";
		byte[] encryptedBytes = kmsTemplate.encryptText(kmsStr, "1234");
		String decryptedText = kmsTemplate.decryptText(kmsStr, encryptedBytes);
		assertThat(decryptedText).isEqualTo("1234");
	}

	@Test
	public void testEncryptDecryptBytes() {
		String kmsStr = "us-east1/integration-test-key-ring/test-key";
		String originalText = "1234";
		byte[] bytesToEncrypt = originalText.getBytes(StandardCharsets.UTF_8);
		byte[] encryptedBytes = kmsTemplate.encryptBytes(kmsStr, bytesToEncrypt);
		byte[] decryptedBytes = kmsTemplate.decryptBytes(kmsStr, encryptedBytes);
		String resultText = new String(decryptedBytes, StandardCharsets.UTF_8);
		assertThat(resultText).isEqualTo(originalText);
	}

	@Test(expected = com.google.api.gax.rpc.InvalidArgumentException.class)
	public void testEncryptDecryptMissMatch() {
		String kmsStr = "us-east1/integration-test-key-ring/test-key";
		byte[] encryptedBytes = kmsTemplate.encryptText(kmsStr, "1234");

		String kmsStr2 = "us-east1/integration-test-key-ring/other-key";
		kmsTemplate.decryptText(kmsStr2, encryptedBytes);
	}
}
