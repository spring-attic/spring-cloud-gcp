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

package com.google.cloud.spring.autoconfigure.kms;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyManagementServiceSettings;
import com.google.cloud.spring.core.DefaultCredentialsProvider;
import com.google.cloud.spring.core.DefaultGcpProjectIdProvider;
import com.google.cloud.spring.core.GcpProjectIdProvider;
import com.google.cloud.spring.core.UserAgentHeaderProvider;
import com.google.cloud.spring.kms.KmsTemplate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Autoconfiguration for GCP KMS which enables data encryption and decryption.
 *
 * @author Emmanouil Gkatziouras
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GcpKmsProperties.class)
@ConditionalOnClass({KeyManagementServiceClient.class, KmsTemplate.class})
@ConditionalOnProperty(value = "spring.cloud.gcp.kms.enabled", matchIfMissing = true)
public class GcpKmsAutoConfiguration {

	private final GcpProjectIdProvider gcpProjectIdProvider;

	public GcpKmsAutoConfiguration(GcpKmsProperties properties) {
		this.gcpProjectIdProvider = properties.getProjectId() != null
				? properties::getProjectId
				: new DefaultGcpProjectIdProvider();
	}

	@Bean
	@ConditionalOnMissingBean
	public CredentialsProvider googleCredentials(GcpKmsProperties kmsProperties) throws Exception {
		return new DefaultCredentialsProvider(kmsProperties);
	}

	@Bean
	@ConditionalOnMissingBean
	public KeyManagementServiceClient keyManagementClient(CredentialsProvider googleCredentials) throws IOException {
		KeyManagementServiceSettings settings = KeyManagementServiceSettings.newBuilder()
				.setCredentialsProvider(googleCredentials)
				.setHeaderProvider(new UserAgentHeaderProvider(GcpKmsAutoConfiguration.class))
				.build();

		return KeyManagementServiceClient.create(settings);
	}

	@Bean
	@ConditionalOnMissingBean
	public KmsTemplate kmsTemplate(KeyManagementServiceClient client) {
		return new KmsTemplate(client, gcpProjectIdProvider);
	}
}
