/*
 *  Copyright 2017-2018 original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Custom {@link PropertySourceLocator} for Google Cloud Runtime Configurator API.
 *
 * @author Jisha Abubaker
 * @author Mike Eltsufin
 */
public class GoogleConfigPropertySourceLocator implements PropertySourceLocator {

	private static final String RUNTIMECONFIG_API_ROOT = "https://runtimeconfig.googleapis.com/v1beta1/";

	private static final String ALL_VARIABLES_PATH =
			"projects/{project}/configs/{name}_{profile}/variables?returnValues=true";

	private static final String PROPERTY_SOURCE_NAME = "spring-cloud-gcp";

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private String projectId;

	private Credentials credentials;

	private String name;

	private String profile;

	private int timeout;

	private boolean enabled;

	public GoogleConfigPropertySourceLocator(GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider,
			GcpConfigProperties gcpConfigProperties) throws IOException {
		Assert.notNull(gcpConfigProperties, "Google Config properties must not be null");

		if (gcpConfigProperties.isEnabled()) {
			Assert.notNull(credentialsProvider, "Credentials provider cannot be null");
			Assert.notNull(projectIdProvider, "Project ID provider cannot be null");
			this.credentials = gcpConfigProperties.getCredentials().getLocation() != null
					? new DefaultCredentialsProvider(gcpConfigProperties).getCredentials()
					: credentialsProvider.getCredentials();
			this.projectId = gcpConfigProperties.getProjectId() != null
					? gcpConfigProperties.getProjectId()
					: projectIdProvider.getProjectId();
			Assert.notNull(this.credentials, "Credentials must not be null");

			Assert.notNull(this.projectId, "Project ID must not be null");

			this.timeout = gcpConfigProperties.getTimeoutMillis();
			this.name = gcpConfigProperties.getName();
			this.profile = gcpConfigProperties.getProfile();
			this.enabled = gcpConfigProperties.isEnabled();
			Assert.notNull(this.name, "Config name must not be null");
			Assert.notNull(this.profile, "Config profile must not be null");
		}
	}

	private HttpEntity<Void> getAuthorizedRequest() throws IOException {
		HttpHeaders headers = new HttpHeaders();
		Map<String, List<String>> credentialHeaders = this.credentials.getRequestMetadata();
		Assert.notNull(credentialHeaders, "No valid credential header(s) found");

		credentialHeaders.forEach((key, values) -> values.forEach(value -> headers.add(key, value)));

		Assert.isTrue(headers.containsKey(AUTHORIZATION_HEADER), "Authorization header required");

		// Adds usage tracking header.
		new UsageTrackingHeaderProvider(this.getClass()).getHeaders().forEach(headers::add);

		return new HttpEntity<>(headers);
	}

	GoogleConfigEnvironment getRemoteEnvironment() throws IOException, HttpClientErrorException {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setReadTimeout(this.timeout);
		RestTemplate template = new RestTemplate(requestFactory);
		HttpEntity<Void> requestEntity = getAuthorizedRequest();
		ResponseEntity<GoogleConfigEnvironment> response = template.exchange(
				RUNTIMECONFIG_API_ROOT + ALL_VARIABLES_PATH, HttpMethod.GET, requestEntity,
				GoogleConfigEnvironment.class, this.projectId, this.name, this.profile);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new HttpClientErrorException(response.getStatusCode(),
					"Invalid response from Runtime Configurator API");
		}
		return response.getBody();
	}

	@Override
	public PropertySource<?> locate(Environment environment) {
		if (!this.enabled) {
			return new MapPropertySource(PROPERTY_SOURCE_NAME, Collections.emptyMap());
		}
		Map<String, Object> config;
		try {
			GoogleConfigEnvironment googleConfigEnvironment = getRemoteEnvironment();
			Assert.notNull(googleConfigEnvironment, "Configuration not in expected format.");
			config = googleConfigEnvironment.getConfig();
		}
		catch (Exception e) {
			String message = String.format("Error loading configuration for %s/%s_%s", this.projectId,
					this.name, this.profile);
			throw new RuntimeException(message, e);
		}
		return new MapPropertySource(PROPERTY_SOURCE_NAME, config);
	}

	public String getProjectId() {
		return this.projectId;
	}
}
