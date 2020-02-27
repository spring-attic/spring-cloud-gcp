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

import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * Implementation of {@link PropertySourceLocator} which provides GCP Secret Manager as a
 * property source.
 *
 * @author Daniel Zou
 * @since 1.2.2
 */
public class SecretManagerPropertySourceLocator implements PropertySourceLocator {

	private static final String SECRET_MANAGER_NAME = "spring-cloud-gcp-secret-manager";

	private final SecretManagerServiceClient client;

	private final GcpProjectIdProvider projectIdProvider;

	private final String secretsPrefix;

	SecretManagerPropertySourceLocator(
			SecretManagerServiceClient client,
			GcpProjectIdProvider projectIdProvider,
			String secretsPrefix) {
		this.client = client;
		this.projectIdProvider = projectIdProvider;
		this.secretsPrefix = secretsPrefix;
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		return new SecretManagerPropertySource(
				SECRET_MANAGER_NAME,
				this.client,
				this.projectIdProvider,
				this.secretsPrefix);
	}
}
