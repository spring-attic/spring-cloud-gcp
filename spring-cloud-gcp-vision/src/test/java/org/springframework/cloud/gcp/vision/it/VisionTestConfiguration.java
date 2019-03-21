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

package org.springframework.cloud.gcp.vision.it;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.vision.DocumentOcrTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VisionTestConfiguration {

	private final String projectId;

	private final CredentialsProvider credentialsProvider;

	public VisionTestConfiguration() throws IOException {
		this.projectId = new DefaultGcpProjectIdProvider().getProjectId();
		this.credentialsProvider = new DefaultCredentialsProvider(Credentials::new);
	}

	@Bean
	@ConditionalOnMissingBean
	public ImageAnnotatorClient imageAnnotatorClient() throws IOException {
		ImageAnnotatorSettings clientSettings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.build();

		return ImageAnnotatorClient.create(clientSettings);
	}

	@Bean
	@ConditionalOnMissingBean
	public DocumentOcrTemplate documentOcrTemplate(
			ImageAnnotatorClient imageAnnotatorClient, Storage storage) {

		return new DocumentOcrTemplate(imageAnnotatorClient, storage, Runnable::run, 2);
	}

	@Bean
	@ConditionalOnMissingBean
	public Storage storage() throws IOException {
		return StorageOptions.newBuilder()
				.setCredentials(this.credentialsProvider.getCredentials())
				.setProjectId(this.projectId)
				.build()
				.getService();
	}
}
