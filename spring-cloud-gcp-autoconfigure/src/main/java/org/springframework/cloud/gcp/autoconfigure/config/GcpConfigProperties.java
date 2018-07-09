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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.CredentialsSupplier;
import org.springframework.cloud.gcp.core.GcpScope;
import org.springframework.core.env.Environment;

/**
 * Configuration for {@link GoogleConfigPropertySourceLocator}.
 *
 * @author Jisha Abubaker
 * @author João André Martins
 * @author Mike Eltsufin
 *
 * @since 1.1
 */
@ConfigurationProperties("spring.cloud.gcp.config")
public class GcpConfigProperties implements CredentialsSupplier {

	/**
	 *  Enables Spring Cloud GCP Config.
	 */
	private boolean enabled;

	/**
	 *  Name of the application.
	 */
	@Value("${spring.application.name:application}")
	private String name;

	/**
	 *  Comma-delimited string of profiles under which the app is running.
	 *  Gets its default value from the {@code spring.profiles.active} property, falling back on the
	 *  {@code spring.profiles.default} property.
	 */
	private String profile;

	/**
	 *  Timeout for Google Runtime Configuration API calls.
	 */
	private int timeoutMillis = 60000;

	/**
	 *  Overrides the GCP project ID specified in the Core module.
	 */
	private String projectId;

	/**
	 * Overrides the GCP OAuth2 credentials specified in the Core module.
	 */
	@NestedConfigurationProperty
	private final Credentials credentials = new Credentials(GcpScope.RUNTIME_CONFIG_SCOPE.getUrl());

	public GcpConfigProperties(Environment environment) {
		if (this.profile == null) {
			String[] profiles = environment.getActiveProfiles();
			if (profiles.length == 0) {
				profiles = environment.getDefaultProfiles();
			}

			if (profiles.length > 0) {
				this.profile = profiles[profiles.length - 1];
			}
			else {
				this.profile = "default";
			}
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}

	public void setProfile(String profile) {
		this.profile = profile;
	}

	public String getProfile() {
		return this.profile;
	}

	public void setTimeoutMillis(int timeoutMillis) {
		this.timeoutMillis = timeoutMillis;
	}

	public int getTimeoutMillis() {
		return this.timeoutMillis;
	}

	public String getProjectId() {
		return this.projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public Credentials getCredentials() {
		return this.credentials;
	}

}
