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

package org.springframework.cloud.gcp.core;

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
import com.google.auth.Credentials;
import com.google.auth.oauth2.ComputeEngineCredentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link CredentialsProvider} implementation that wraps credentials based on
 * user-provided properties and defaults.
 *
 * @author João André Martins
 * @author Mike Eltsufin
 */
public class DefaultCredentialsProvider implements CredentialsProvider {

	private static final Log LOGGER = LogFactory.getLog(DefaultCredentialsProvider.class);

	private static final String DEFAULT_SCOPES_PLACEHOLDER = "DEFAULT_SCOPES";

	private static final List<String> CREDENTIALS_SCOPES_LIST = Collections.unmodifiableList(
			Arrays.stream(GcpScope.values())
					.map(GcpScope::getUrl)
					.collect(Collectors.toList()));

	private CredentialsProvider wrappedCredentialsProvider;

	@Override
	public Credentials getCredentials() throws IOException {
		return this.wrappedCredentialsProvider.getCredentials();
	}

	/**
	 *
	 * @param credentialsSupplier Provides properties that can override OAuth2
	 * scopes list used by the credentials, and the location of the OAuth2 credentials private
	 * key.
	 * @throws IOException
	 */
	public DefaultCredentialsProvider(CredentialsSupplier credentialsSupplier) throws IOException {
		List<String> scopes = resolveScopes(credentialsSupplier.getCredentials().getScopes());
		Resource providedLocation = credentialsSupplier.getCredentials().getLocation();

		if (!StringUtils.isEmpty(providedLocation)) {
			this.wrappedCredentialsProvider = FixedCredentialsProvider
					.create(GoogleCredentials.fromStream(
							providedLocation.getInputStream())
							.createScoped(scopes));
		}
		else {
			this.wrappedCredentialsProvider = GoogleCredentialsProvider.newBuilder()
					.setScopesToApply(scopes)
					.build();
		}

		try {
			Credentials credentials = this.wrappedCredentialsProvider.getCredentials();

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
	}

	static List<String> resolveScopes(List<String> scopes) {
		if (!ObjectUtils.isEmpty(scopes)) {
			Set<String> resolvedScopes = new HashSet<>();
			scopes.forEach(scope -> {
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
}
