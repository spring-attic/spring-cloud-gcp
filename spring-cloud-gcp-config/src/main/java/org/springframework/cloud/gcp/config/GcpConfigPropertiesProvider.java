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

import org.springframework.cloud.gcp.core.Credentials;

/**
 * Provides the required properties for Spring Cloud GCP Runtime Configuration.
 *
 * @author João André Martins
 */
public interface GcpConfigPropertiesProvider {

	/**
	 * Determines if Spring Cloud GCP Runtime Configuration is enabled.
	 */
	boolean isEnabled();

	/**
	 * The name of the Spring Application being run.
	 */
	String getName();

	/**
	 * The profile running the app. For example, "prod", "dev", etc.
	 */
	String getProfile();

	/**
	 * The timeout for reaching Google Runtime Configuration API.
	 */
	int getTimeoutMillis();

	/**
	 * An override to the GCP project ID in the core module.
	 */
	String getProjectId();

	/**
	 * An override to the GCP credentials in the core module.
	 */
	Credentials getCredentials();
}
