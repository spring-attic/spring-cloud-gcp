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

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceSettings;
import com.google.protobuf.ByteString;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Bootstrap Autoconfiguration for GCP Secret Manager which enables loading secrets as
 * properties into the application {@link org.springframework.core.env.Environment}.
 *
 * @author Daniel Zou
 * @since 1.2.2
 */
@Configuration
@EnableConfigurationProperties(GcpSecretManagerProperties.class)
@ConditionalOnClass(SecretManagerServiceClient.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.secretmanager.enabled", matchIfMissing = true)
public class GcpSecretManagerBootstrapConfiguration {

	private final GcpSecretManagerProperties properties;

	private final CredentialsProvider credentialsProvider;

	private final GcpProjectIdProvider gcpProjectIdProvider;

	public GcpSecretManagerBootstrapConfiguration(
			GcpSecretManagerProperties properties,
			ConfigurableEnvironment configurableEnvironment) throws IOException {

		this.properties = properties;
		this.credentialsProvider = new DefaultCredentialsProvider(properties);
		this.gcpProjectIdProvider = properties.getProjectId() != null
				? properties::getProjectId
				: new DefaultGcpProjectIdProvider();

		// Registers {@link ByteString} type converters to convert to String and byte[].
		configurableEnvironment.getConversionService().addConverter(
				new Converter<ByteString, String>() {
					@Override
					public String convert(ByteString source) {
						return source.toStringUtf8();
					}
				});

		configurableEnvironment.getConversionService().addConverter(
				new Converter<ByteString, byte[]>() {
					@Override
					public byte[] convert(ByteString source) {
						return source.toByteArray();
					}
				});
	}

	@Bean
	@ConditionalOnMissingBean
	public SecretManagerServiceClient secretManagerClient() throws IOException {
		SecretManagerServiceSettings settings = SecretManagerServiceSettings.newBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.setHeaderProvider(new UserAgentHeaderProvider(GcpSecretManagerBootstrapConfiguration.class))
				.build();

		return SecretManagerServiceClient.create(settings);
	}

	@Bean
	public PropertySourceLocator secretManagerPropertySourceLocator(SecretManagerServiceClient client) {
		return new SecretManagerPropertySourceLocator(
				client, this.gcpProjectIdProvider, this.properties.getSecretNamePrefix());
	}
}
