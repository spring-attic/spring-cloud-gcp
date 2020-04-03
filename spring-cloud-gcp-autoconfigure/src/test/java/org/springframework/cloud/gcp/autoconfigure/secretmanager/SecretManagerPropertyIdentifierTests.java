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

package org.springframework.cloud.gcp.autoconfigure.secretmanager;

import org.junit.Test;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class SecretManagerPropertyIdentifierTests {

	private static final GcpProjectIdProvider DEFAULT_PROJECT_ID_PROVIDER = () -> "defaultProject";

	@Test
	public void testNonSecret() {
		String property = "spring.cloud.datasource";
		SecretManagerPropertyIdentifier secretIdentifier =
				SecretManagerPropertyIdentifier.parseFromProperty(property, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(secretIdentifier).isNull();
	}

	@Test
	public void testShortProperty_secretId() {
		String property = "gcp-secret/the-secret";
		SecretManagerPropertyIdentifier secretIdentifier =
				SecretManagerPropertyIdentifier.parseFromProperty(property, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(secretIdentifier.getProjectId()).isEqualTo("defaultProject");
		assertThat(secretIdentifier.getSecretId()).isEqualTo("the-secret");
		assertThat(secretIdentifier.getVersion()).isEqualTo("latest");
	}

	@Test
	public void testShortProperty_projectSecretId() {
		String property = "gcp-secret/my-project/the-secret";
		SecretManagerPropertyIdentifier secretIdentifier =
				SecretManagerPropertyIdentifier.parseFromProperty(property, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(secretIdentifier.getProjectId()).isEqualTo("my-project");
		assertThat(secretIdentifier.getSecretId()).isEqualTo("the-secret");
		assertThat(secretIdentifier.getVersion()).isEqualTo("latest");
	}

	@Test
	public void testShortProperty_projectSecretIdVersion() {
		String property = "gcp-secret/my-project/the-secret/2";
		SecretManagerPropertyIdentifier secretIdentifier =
				SecretManagerPropertyIdentifier.parseFromProperty(property, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(secretIdentifier.getProjectId()).isEqualTo("my-project");
		assertThat(secretIdentifier.getSecretId()).isEqualTo("the-secret");
		assertThat(secretIdentifier.getVersion()).isEqualTo("2");
	}

	@Test
	public void testLongProperty_projectSecret() {
		String property = "gcp-secret/projects/my-project/secrets/the-secret";
		SecretManagerPropertyIdentifier secretIdentifier =
				SecretManagerPropertyIdentifier.parseFromProperty(property, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(secretIdentifier.getProjectId()).isEqualTo("my-project");
		assertThat(secretIdentifier.getSecretId()).isEqualTo("the-secret");
		assertThat(secretIdentifier.getVersion()).isEqualTo("latest");
	}

	@Test
	public void testLongProperty_projectSecretVersion() {
		String property = "gcp-secret/projects/my-project/secrets/the-secret/versions/3";
		SecretManagerPropertyIdentifier secretIdentifier =
				SecretManagerPropertyIdentifier.parseFromProperty(property, DEFAULT_PROJECT_ID_PROVIDER);

		assertThat(secretIdentifier.getProjectId()).isEqualTo("my-project");
		assertThat(secretIdentifier.getSecretId()).isEqualTo("the-secret");
		assertThat(secretIdentifier.getVersion()).isEqualTo("3");
	}
}
