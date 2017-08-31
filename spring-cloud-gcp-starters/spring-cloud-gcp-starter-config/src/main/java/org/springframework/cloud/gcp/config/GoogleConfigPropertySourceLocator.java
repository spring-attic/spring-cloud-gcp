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

package org.springframework.cloud.gcp.config;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Custom {@link PropertySourceLocator} for Google Cloud Runtime Configurator API.
 *
 * @author Jisha Abubaker
 */
@Configuration
public class GoogleConfigPropertySourceLocator implements PropertySourceLocator {

	private static final String RUNTIMECONFIG_API_ROOT = "https://runtimeconfig.googleapis.com/v1beta1/";

	private static final String ALL_VARIABLES_PATH =
			"projects/{project}/configs/{name}_{profile}/variables?returnValues=true";

	private static final String PROPERTY_SOURCE_NAME = "spring-cloud-gcp";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String BEARER_TOKEN_PREFIX = "Bearer ";

	private String projectId;

	private Credentials credentials;

	private ConfigClientProperties configClientProperties;

	private int timeout;

	public GoogleConfigPropertySourceLocator(GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider,
			ConfigClientProperties configClientProperties,
			GcpConfigProperties gcpConfigProperties) throws Exception {
		Assert.notNull(credentialsProvider, "Credentials provider cannot be null");
		Assert.notNull(projectIdProvider, "Project ID provider cannot be null");
		this.credentials = credentialsProvider.getCredentials();
		this.projectId = projectIdProvider.getProjectId();
		Assert.notNull(this.credentials, "Credentials must not be null");
		Assert.notNull(this.projectId, "Project ID must not be null");

		this.timeout = 60000; // 1 minute in milliseconds
		if (gcpConfigProperties != null) {
			this.timeout = gcpConfigProperties.getTimeout();
		}
		this.configClientProperties = configClientProperties;
		Assert.notNull(this.configClientProperties, "Client Configuration must not be null");
		Assert.notNull(this.timeout, "Runtime Configurator API timeout must not be null");
	}

	private RestTemplate getSecureRestTemplate() throws Exception {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setReadTimeout(this.timeout);
		RestTemplate template = new RestTemplate(requestFactory);
		Map<String, String> headers = new HashMap<>();
		Map<String, List<String>> requestMetadata = this.credentials.getRequestMetadata();
		Assert.isTrue(requestMetadata.containsKey(AUTHORIZATION_HEADER), "Authorization header required");

		List<String> authorizationHeaders = requestMetadata.get(AUTHORIZATION_HEADER);

		String bearerToken = null;
		for (String authorizationHeader : authorizationHeaders) {
			if (authorizationHeader.startsWith(BEARER_TOKEN_PREFIX)) {
				bearerToken = authorizationHeader;
				break;
			}
		}
		Assert.notNull(bearerToken, "Valid Bearer token not found");

		headers.put(AUTHORIZATION_HEADER, bearerToken);

		template.setInterceptors(Collections
				.singletonList(new ConfigServicePropertySourceLocator.GenericRequestHeaderInterceptor(headers)));
		return template;
	}

	private GoogleConfigEnvironment getRemoteEnvironment(String name, String profile, RestTemplate restTemplate)
			throws IOException, HttpClientErrorException {
		ResponseEntity<GoogleConfigEnvironment> response = restTemplate.exchange(
				RUNTIMECONFIG_API_ROOT + ALL_VARIABLES_PATH, HttpMethod.GET,
				null, GoogleConfigEnvironment.class, this.projectId, name, profile);

		if (response == null || !response.getStatusCode().is2xxSuccessful()) {
			HttpStatus code = (response == null) ? HttpStatus.BAD_REQUEST : response.getStatusCode();
			throw new HttpClientErrorException(code, "Invalid response from Runtime Configurator API");
		}
		return response.getBody();
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		Map<String, Object> config;
		ConfigClientProperties properties = this.configClientProperties.override(environment);
		try {
			Assert.notNull(properties.getName(), "Config name must not be null");
			Assert.notNull(properties.getProfile(), "Config profile must not be null");
			RestTemplate restTemplate = getSecureRestTemplate();
			GoogleConfigEnvironment googleConfigEnvironment = getRemoteEnvironment(properties.getName(),
					properties.getProfile(), restTemplate);
			Assert.notNull(googleConfigEnvironment, "Configuration not in expected format.");
			config = googleConfigEnvironment.getConfig();
		}
		catch (Exception e) {
			String message = String.format("Error loading configuration for %s/%s_%s", this.projectId,
					properties.getName(), properties.getProfile());
			throw new RuntimeException(message, e);
		}
		return new MapPropertySource(PROPERTY_SOURCE_NAME, config);
	}
}
