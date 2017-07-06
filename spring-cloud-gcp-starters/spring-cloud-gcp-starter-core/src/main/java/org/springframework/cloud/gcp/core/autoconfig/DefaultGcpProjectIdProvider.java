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

package org.springframework.cloud.gcp.core.autoconfig;

import java.util.Optional;

import com.google.cloud.ServiceOptions;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProperties;

/**
 * A project ID provider that wraps {@link GcpProperties} and
 * {@link ServiceOptions#getDefaultProjectId()}.
 *
 * @author João André Martins
 */
public class DefaultGcpProjectIdProvider implements GcpProjectIdProvider {

	private GcpProperties gcpProperties;

	public DefaultGcpProjectIdProvider(GcpProperties gcpProperties) {
		this.gcpProperties = gcpProperties;
	}

	/**
	 * {@link ServiceOptions#getDefaultProjectId()} checks for the project ID in the
	 * {@code GOOGLE_CLOUD_PROJECT} environment variable and the Metadata Server, among others.
	 *
	 * @return the project ID in the context
	 */
	@Override
	public Optional<String> getProjectId() {
		if (this.gcpProperties != null) {
			String propertiesProjectId = this.gcpProperties.getProjectId();
			if (propertiesProjectId != null) {
				return Optional.of(propertiesProjectId);
			}
		}

		String serviceOptionsProjectId = ServiceOptions.getDefaultProjectId();
		if (serviceOptionsProjectId != null) {
			return Optional.of(serviceOptionsProjectId);
		}

		return Optional.empty();
	}
}
