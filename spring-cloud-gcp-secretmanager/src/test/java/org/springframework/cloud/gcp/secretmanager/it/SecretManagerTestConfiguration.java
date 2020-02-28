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

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceSettings;

import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.secretmanager.SecretManagerTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretManagerTestConfiguration {

	private final GcpProjectIdProvider projectIdProvider;

	private final CredentialsProvider credentialsProvider;

	public SecretManagerTestConfiguration() throws IOException {
		this.projectIdProvider = new DefaultGcpProjectIdProvider();
		this.credentialsProvider = new DefaultCredentialsProvider(Credentials::new);
	}

	@Bean
	public SecretManagerTemplate secretManagerTemplate() throws IOException {
		SecretManagerServiceSettings settings = SecretManagerServiceSettings.newBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.build();

		return new SecretManagerTemplate(SecretManagerServiceClient.create(settings), projectIdProvider);
	}
}
