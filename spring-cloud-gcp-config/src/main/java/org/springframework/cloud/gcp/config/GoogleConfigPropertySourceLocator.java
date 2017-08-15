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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.ServiceOptions;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Custom property source locator for Google Cloud Runtime Configurator.
 *
 * @author Jisha Abubaker
 */
@Order(0)
public final class GoogleConfigPropertySourceLocator implements PropertySourceLocator {

	private static final String RUNTIMECONFIG_API_ROOT = "https://runtimeconfig.googleapis.com/v1beta1/";

	private static final String RUNTIMECONFIG_SCOPE = "https://www.googleapis.com/auth/cloudruntimeconfig";

	private static final String ALL_VARIABLES_PATH =
			"projects/{project}/configs/{name}_{profile}/variables?returnValues=true";

	private static final String PROPERTY_SOURCE_NAME = "spring-cloud-gcp";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private static final String BEARER_TOKEN_PREFIX = "Bearer ";

	private static final Log logger = LogFactory.getLog(GoogleConfigPropertySourceLocator.class);

	private ConfigClientProperties defaultProperties;

	private GoogleCredentials credentials;

	private String projectId;

	public GoogleConfigPropertySourceLocator(ConfigClientProperties defaultProperties) throws IOException {
		this.defaultProperties = defaultProperties;
		this.credentials = ServiceAccountCredentials.getApplicationDefault()
				.createScoped(Collections.singleton(RUNTIMECONFIG_SCOPE));
		this.projectId = ServiceOptions.getDefaultProjectId();
	}

	private RestTemplate getSecureRestTemplate() throws Exception {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setReadTimeout((60 * 1000 * 3) + 5000);
		RestTemplate template = new RestTemplate(requestFactory);
		Map<String, String> headers = new HashMap<>();

		AccessToken accessToken = this.credentials.getAccessToken();
		if (accessToken == null
				|| accessToken.getExpirationTime().compareTo(new Date(System.currentTimeMillis())) <= 0) {
			this.credentials.refresh();
			accessToken = this.credentials.getAccessToken();
		}
		if (accessToken == null) {
			throw new IllegalAccessException("Error refreshing access token");
		}
		headers.put(AUTHORIZATION_HEADER, BEARER_TOKEN_PREFIX + accessToken.getTokenValue());

		if (!headers.isEmpty()) {
			template.setInterceptors(Collections.singletonList(new GenericRequestHeaderInterceptor(headers)));
		}
		return template;
	}

	private GoogleConfigEnvironment getRemoteEnvironment(RestTemplate restTemplate, ConfigClientProperties properties)
			throws IOException {
		String name = properties.getName();
		String profile = properties.getProfile();

		ResponseEntity<GoogleConfigEnvironment> response = null;

		try {
			response = restTemplate.exchange(RUNTIMECONFIG_API_ROOT + ALL_VARIABLES_PATH, HttpMethod.GET,
					null, GoogleConfigEnvironment.class, this.projectId, name, profile);
		}
		catch (HttpClientErrorException e) {
			if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
				throw e;
			}
		}

		if (response == null || response.getStatusCode() != HttpStatus.OK) {
			return null;
		}
		return response.getBody();
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		ConfigClientProperties properties = this.defaultProperties.override(environment);
		Map<String, Object> config = new HashMap<>();
		try {

			RestTemplate restTemplate = getSecureRestTemplate();
			GoogleConfigEnvironment googleConfigEnvironment = getRemoteEnvironment(restTemplate, properties);
			if (googleConfigEnvironment != null) {

				for (Variable variable : googleConfigEnvironment.getVariables()) {
					Object value = (variable.getText() != null) ? variable.getText() : variable.getValue();
					config.put(variable.getName(), value);
				}
			}
		}
		catch (Exception e) {
			String message = String.format("Error loading configuration for %s/%s_%s", this.projectId,
					properties.getName(), properties.getProfile());
			logger.error(message, e);
		}
		return new MapPropertySource(PROPERTY_SOURCE_NAME, config);
	}

	public static class GenericRequestHeaderInterceptor implements ClientHttpRequestInterceptor {
		private final Map<String, String> headers;

		GenericRequestHeaderInterceptor(Map<String, String> headers) {
			this.headers = headers;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {
			for (Map.Entry<String, String> header : this.headers.entrySet()) {
				request.getHeaders().add(header.getKey(), header.getValue());
			}
			return execution.execute(request, body);
		}
	}
}
