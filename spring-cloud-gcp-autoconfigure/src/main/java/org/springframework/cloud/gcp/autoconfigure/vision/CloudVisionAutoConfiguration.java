/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.vision;

import java.io.IOException;
import java.util.concurrent.Executor;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.storage.Storage;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;
import org.springframework.cloud.gcp.vision.CloudVisionTemplate;
import org.springframework.cloud.gcp.vision.DocumentOcrTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Provides Spring Beans for using Cloud Vision API.
 *
 * @author Daniel Zou
 * @since 1.1
 */
@Configuration
@EnableConfigurationProperties(CloudVisionProperties.class)
@ConditionalOnClass(CloudVisionTemplate.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.vision.enabled", matchIfMissing = true)
public class CloudVisionAutoConfiguration {

	private final CloudVisionProperties cloudVisionProperties;

	private final CredentialsProvider credentialsProvider;

	public CloudVisionAutoConfiguration(
			CloudVisionProperties properties, CredentialsProvider credentialsProvider)
			throws IOException {

		this.cloudVisionProperties = properties;

		if (properties.getCredentials().hasKey()) {
			this.credentialsProvider = new DefaultCredentialsProvider(properties);
		}
		else {
			this.credentialsProvider = credentialsProvider;
		}
	}

	/**
	 * Configure the Cloud Vision API client {@link ImageAnnotatorClient}. The
	 * spring-cloud-gcp-starter autowires a {@link CredentialsProvider} object that provides
	 * the GCP credentials, required to authenticate and authorize Vision API calls.
	 * <p>Cloud Vision API client implements {@link AutoCloseable}, which is automatically
	 * honored by Spring bean lifecycle.
	 * <p>Most of the Google Cloud API clients are thread-safe heavy objects. I.e., it's better
	 * to produce a singleton and re-using the client object for multiple requests.
	 * @return a Cloud Vision API client
	 * @throws IOException if an exception occurs creating the ImageAnnotatorClient
	 */
	@Bean
	@ConditionalOnMissingBean
	public ImageAnnotatorClient imageAnnotatorClient() throws IOException {
		ImageAnnotatorSettings clientSettings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.setHeaderProvider(new UserAgentHeaderProvider(CloudVisionAutoConfiguration.class))
				.build();

		return ImageAnnotatorClient.create(clientSettings);
	}

	@Bean
	@ConditionalOnMissingBean
	public CloudVisionTemplate cloudVisionTemplate(ImageAnnotatorClient imageAnnotatorClient) {
		return new CloudVisionTemplate(imageAnnotatorClient);
	}

	@Bean
	@ConditionalOnMissingBean
	public DocumentOcrTemplate documentOcrTemplate(
			ImageAnnotatorClient imageAnnotatorClient,
			Storage storage,
			@Qualifier("cloudVisionExecutor") Executor executor) {

		return new DocumentOcrTemplate(
				imageAnnotatorClient,
				storage,
				executor,
				this.cloudVisionProperties.getJsonOutputBatchSize());
	}

	@Bean
	@ConditionalOnMissingBean(name = "cloudVisionExecutor")
	public Executor cloudVisionExecutor() {
		ThreadPoolTaskExecutor ackExecutor = new ThreadPoolTaskExecutor();
		ackExecutor.setMaxPoolSize(this.cloudVisionProperties.getExecutorThreadsCount());
		ackExecutor.setThreadNamePrefix("gcp-cloud-vision-ocr-executor");
		return ackExecutor;
	}
}
