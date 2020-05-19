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

package org.springframework.cloud.gcp.secretmanager.it;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.secretmanager.SecretManagerTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * Integration tests for {@link SecretManagerTemplate}.
 *
 * @author Daniel Zou
 * @author Mike Eltsufin
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SecretManagerTestConfiguration.class })
public class SecretManagerTemplateIntegrationTests {

	@Autowired
	SecretManagerTemplate secretManagerTemplate;

	@BeforeClass
	public static void prepare() {
		assumeThat(System.getProperty("it.secretmanager"))
				.as("Secret manager integration tests are disabled. "
						+ "Please use '-Dit.secretmanager=true' to enable them.")
				.isEqualTo("true");
	}

	@Test
	public void testReadWriteSecrets() {
		secretManagerTemplate.createSecret("test-secret-1234", "1234");

		String secretString = secretManagerTemplate.getSecretString("test-secret-1234");
		assertThat(secretString).isEqualTo("1234");

		byte[] secretBytes = secretManagerTemplate.getSecretBytes("test-secret-1234");
		assertThat(secretBytes).isEqualTo("1234".getBytes());
	}

	@Test(expected = com.google.api.gax.rpc.NotFoundException.class)
	public void testReadMissingSecret() {
		secretManagerTemplate.getSecretBytes("test-NON-EXISTING-secret");
	}

	@Test
	public void testUpdateSecrets() {
		secretManagerTemplate.createSecret("test-update-secret", "5555");
		secretManagerTemplate.createSecret("test-update-secret", "6666");

		String secretString = secretManagerTemplate.getSecretString("test-update-secret");
		assertThat(secretString).isEqualTo("6666");

		byte[] secretBytes = secretManagerTemplate.getSecretBytes("test-update-secret");
		assertThat(secretBytes).isEqualTo("6666".getBytes());
	}
}
