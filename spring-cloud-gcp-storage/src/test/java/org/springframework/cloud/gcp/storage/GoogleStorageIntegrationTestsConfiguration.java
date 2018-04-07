/*
 *  Copyright 2018 original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.cloud.gcp.storage;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Chengyuan Zhao
 */
@Configuration
@PropertySource("application-test.properties")
@Import(GoogleStorageProtocolResolver.class)
public class GoogleStorageIntegrationTestsConfiguration {

	@Bean
	public static Storage storage(CredentialsProvider credentialsProvider,
			GcpProjectIdProvider projectIdProvider) throws IOException {
		return StorageOptions.newBuilder()
				.setCredentials(credentialsProvider.getCredentials())
				.setProjectId(projectIdProvider.getProjectId()).build().getService();
	}

	@Bean
	public GcpProjectIdProvider gcpProjectIdProvider() {
		return new DefaultGcpProjectIdProvider();
	}

	@Bean
	public CredentialsProvider credentialsProvider() {
		try {
			return new DefaultCredentialsProvider(Credentials::new);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public GoogleStorageProtocolResolverSettings googleStorageProtocolResolverSettings() {
		return new GoogleStorageProtocolResolverSettings();
	}
}
