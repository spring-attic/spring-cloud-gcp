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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.api.gax.rpc.HeaderProvider;
import com.google.common.annotations.VisibleForTesting;

import org.springframework.stereotype.Component;

/**
 * Provides the User-Agent header to signal to the Google Cloud Client Libraries that
 * requests originate from a Spring Integration.
 *
 * @author João André Martins
 * @author Chengyuan Zhao
 */
@Component
public class UsageTrackingHeaderProvider implements HeaderProvider {

	/** Class whose project name and version will be used in the header. */
	private Class clazz;

	private Properties trackingProperties;

	public UsageTrackingHeaderProvider(Class clazz) {
		this.clazz = clazz;
		String implementationVersion;
	}

	private String getImplementationVersion() {
		try {
			return loadTrackingProperties().getProperty("metrics.version");
		}
		catch (IOException e) {
			return this.clazz.getPackage().getImplementationVersion();
		}
	}

	@VisibleForTesting
	Properties loadTrackingProperties() throws IOException {
		if (this.trackingProperties == null) {
			this.trackingProperties = new Properties();
			this.trackingProperties.load(this.getClass().getClassLoader()
					.getResourceAsStream("tracking-header.properties"));

		}
		return this.trackingProperties;
	}

	/**
	 * Returns the "User-Agent" header whose value should be added to the google-cloud-java REST API calls.
	 * e.g., {@code User-Agent: Spring/1.0.0.RELEASE spring-cloud-gcp-pubsub/1.0.0.RELEASE}.
	 */
	@Override
	public Map<String, String> getHeaders() {
		Map<String, String> headers = new HashMap<>();

		String[] packageTokens = this.clazz.getPackage().getName().split("\\.");
		String springLibrary = "spring-cloud-gcp-" + packageTokens[packageTokens.length - 1];

		headers.put("User-Agent",
				"Spring/" + this.getClass().getPackage().getImplementationVersion()
						+ " " + springLibrary + "/" + getImplementationVersion());

		return headers;
	}
}
