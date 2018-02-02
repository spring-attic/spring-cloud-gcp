/*
 *  Copyright 2017 original author or authors.
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

package com.example;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * @author João André Martins
 */
@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * Configure the Cloud Vision API client {@link ImageAnnotatorClient}. The
	 * spring-cloud-gcp-starter autowires a {@link CredentialsProvider} object that provides
	 * the GCP credentials, required to authenticate and authorize Vision API calls.
	 *
	 * <p>Cloud Vision API client implements {@link AutoCloseable}, which is automatically
	 * honored by Spring bean lifecycle.
	 *
	 * <p>Most of the Google Cloud API clients are thread-safe heavy objects. I.e., it's better
	 * to produce a singleton and re-using the client object for multiple requests.
	 *
	 * @param credentialsProvider GCP Credential to use to access Cloud Vision API
	 * @return A Cloud Vision API client
	 * @throws IOException
	 */
	@Bean
	public ImageAnnotatorClient imageAnnotatorClient(CredentialsProvider credentialsProvider) throws IOException {
		ImageAnnotatorSettings clientSettings = ImageAnnotatorSettings.newBuilder()
				.setCredentialsProvider(credentialsProvider)
				.build();

		return ImageAnnotatorClient.create(clientSettings);
	}
}
