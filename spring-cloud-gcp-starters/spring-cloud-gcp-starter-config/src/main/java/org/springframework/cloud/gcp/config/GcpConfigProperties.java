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

import java.util.Collections;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gcp.core.AbstractCredentialsProperty;
import org.springframework.cloud.gcp.core.GcpScope;

/**
 * Configuration for {@link GoogleConfigPropertySourceLocator}.
 *
 * @author Jisha Abubaker
 */
@ConfigurationProperties("spring.cloud.gcp.config")
public class GcpConfigProperties {

	private boolean enabled = true;

	private String name;

	private String profile = "default";

	// Config server API time out in milliseconds, default = 1 minute
	private int timeout = 60000;

	private String projectId;

	private Credentials credentials;

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

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getTimeout() {
		return this.timeout;
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

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public static class Credentials extends AbstractCredentialsProperty {
		public Credentials() {
			setScopes(Collections.singletonList(GcpScope.RUNTIME_CONFIG_SCOPE.getUrl()));
		}
	}
}
