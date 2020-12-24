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

package com.google.cloud.spring.kms;

import com.google.cloud.kms.v1.CryptoKeyName;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class KmsPropertyUtilsTests {

	private static final GcpProjectIdProvider DEFAULT_PROJECT_ID_PROVIDER = () -> "defaultProject";


	@Test
	public void testInvalidKmsFormat_missingValues() {
		String cryptoKeyNameStr = "some-key";

		assertThatThrownBy(() ->
				KmsPropertyUtils.getCryptoKeyName(cryptoKeyNameStr, DEFAULT_PROJECT_ID_PROVIDER))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unrecognized format for specifying a GCP KMS");
	}

	@Test
	public void testKmsFormat_globalDefault() {
		String cryptoKeyNameStr = "key-ring-id/key-id";

		CryptoKeyName cryptoKeyName = KmsPropertyUtils.getCryptoKeyName(cryptoKeyNameStr, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(cryptoKeyName.getProject()).isEqualTo(DEFAULT_PROJECT_ID_PROVIDER.getProjectId());
		assertThat(cryptoKeyName.getLocation()).isEqualTo("global");
		assertThat(cryptoKeyName.getKeyRing()).isEqualTo("key-ring-id");
		assertThat(cryptoKeyName.getCryptoKey()).isEqualTo("key-id");
	}

	@Test
	public void testKmsFormat_noProject() {
		String cryptoKeyNameStr = "europe-west2/key-ring-id/key-id";

		CryptoKeyName cryptoKeyName = KmsPropertyUtils.getCryptoKeyName(cryptoKeyNameStr, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(cryptoKeyName.getProject()).isEqualTo(DEFAULT_PROJECT_ID_PROVIDER.getProjectId());
		assertThat(cryptoKeyName.getLocation()).isEqualTo("europe-west2");
		assertThat(cryptoKeyName.getKeyRing()).isEqualTo("key-ring-id");
		assertThat(cryptoKeyName.getCryptoKey()).isEqualTo("key-id");
	}

	@Test
	public void testKmsFormat_lean() {
		String cryptoKeyNameStr = "test-project/europe-west2/key-ring-id/key-id";

		CryptoKeyName cryptoKeyName = KmsPropertyUtils.getCryptoKeyName(cryptoKeyNameStr, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(cryptoKeyName.getProject()).isEqualTo("test-project");
		assertThat(cryptoKeyName.getLocation()).isEqualTo("europe-west2");
		assertThat(cryptoKeyName.getKeyRing()).isEqualTo("key-ring-id");
		assertThat(cryptoKeyName.getCryptoKey()).isEqualTo("key-id");
	}

	@Test
	public void testKmsFormat_verbose() {
		String cryptoKeyNameStr = "projects/test-project/locations/europe-west2/keyRings/key-ring-id/cryptoKeys/key-id";

		CryptoKeyName cryptoKeyName = KmsPropertyUtils.getCryptoKeyName(cryptoKeyNameStr, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(cryptoKeyName.getProject()).isEqualTo("test-project");
		assertThat(cryptoKeyName.getLocation()).isEqualTo("europe-west2");
		assertThat(cryptoKeyName.getKeyRing()).isEqualTo("key-ring-id");
		assertThat(cryptoKeyName.getCryptoKey()).isEqualTo("key-id");
	}

}
