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

package org.springframework.cloud.gcp.autoconfigure.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.GoogleCredentialsProvider;
import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 *
 * Base starter for Google Cloud Projects. Provides defaults for {@link GoogleCredentials}.
 * Binds properties from {@link GcpProperties}
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 */
@Configuration
@ConditionalOnClass(GoogleCredentials.class)
@EnableConfigurationProperties(GcpProperties.class)
public class GcpContextAutoConfiguration {
	private static final String DEFAULT_SCOPES_PLACEHOLDER = "DEFAULT_SCOPES";

	private static final List<String> CREDENTIALS_SCOPES_LIST =
			Collections.unmodifiableList(
					Arrays.stream(GcpScope.values())
						.map(GcpScope::getUrl)
						.collect(Collectors.toList()));

	private static final Log LOGGER = LogFactory.getLog(GcpContextAutoConfiguration.class);

	private final GcpProperties gcpProperties;

	public GcpContextAutoConfiguration(GcpProperties gcpProperties) {
		this.gcpProperties = gcpProperties;
	}

	protected List<String> resolveScopes() {
		Credentials propertyCredentials = this.gcpProperties.getCredentials();
		if (!ObjectUtils.isEmpty(propertyCredentials.getScopes())) {
			Set<String> resolvedScopes = new HashSet<>();
			propertyCredentials.getScopes().forEach(scope -> {
				if (DEFAULT_SCOPES_PLACEHOLDER.equals(scope)) {
					resolvedScopes.addAll(CREDENTIALS_SCOPES_LIST);
				}
				else {
					resolvedScopes.add(scope);
				}
			});

			return Collections.unmodifiableList(new ArrayList<>(resolvedScopes));
		}

		return CREDENTIALS_SCOPES_LIST;
	}

	@Bean
	@ConditionalOnMissingBean
	public CredentialsProvider googleCredentials() throws Exception {
		CredentialsProvider credentialsProvider;

		Credentials propertyCredentials = this.gcpProperties.getCredentials();

		List<String> scopes = resolveScopes();

		if (!StringUtils.isEmpty(propertyCredentials.getLocation())) {
			credentialsProvider = FixedCredentialsProvider
					.create(GoogleCredentials.fromStream(
							propertyCredentials.getLocation().getInputStream())
					.createScoped(scopes));
		}
		else {
			credentialsProvider = GoogleCredentialsProvider.newBuilder()
					.setScopesToApply(scopes)
					.build();
		}

		try {
			com.google.auth.Credentials credentials = credentialsProvider.getCredentials();

			if (LOGGER.isInfoEnabled()) {
				if (credentials instanceof UserCredentials) {
					LOGGER.info("Default credentials provider for user "
							+ ((UserCredentials) credentials).getClientId());
				}
				else if (credentials instanceof ServiceAccountCredentials) {
					LOGGER.info("Default credentials provider for service account "
							+ ((ServiceAccountCredentials) credentials).getClientEmail());
				}
				else if (credentials instanceof ComputeEngineCredentials) {
					LOGGER.info("Default credentials provider for Google Compute Engine.");
				}
				LOGGER.info("Scopes in use by default credentials: " + scopes.toString());
			}
		}
		catch (IOException ioe) {
			LOGGER.error("No credentials were found.", ioe);
		}

		return credentialsProvider;
	}

	/**
	 * @return a {@link GcpProjectIdProvider} that returns the project ID in the properties or, if
	 * none, the project ID from the GOOGLE_CLOUD_PROJECT envvar and Metadata Server
	 */
	@Bean
	@ConditionalOnMissingBean
	public GcpProjectIdProvider gcpProjectIdProvider() {
		GcpProjectIdProvider projectIdProvider =
				this.gcpProperties.getProjectId() != null
						? () -> this.gcpProperties.getProjectId()
						: new DefaultGcpProjectIdProvider();

		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("The default project ID is " + projectIdProvider.getProjectId());
		}

		return projectIdProvider;
	}
}
