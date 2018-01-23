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

import java.util.HashMap;
import java.util.Map;

import com.google.api.gax.rpc.HeaderProvider;

/**
 * Provides the User-Agent header to signal to the Google Cloud Client Libraries that requests originate from a Spring
 * Integration.
 *
 * @author João André Martins
 */
public class UsageTrackingHeaderProvider implements HeaderProvider {

	/** Class whose project name and version will be used in the header */
	private Class clazz;

	public UsageTrackingHeaderProvider(Class clazz) {
		this.clazz = clazz;
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
				+ " " + springLibrary + "/" + this.clazz.getPackage().getImplementationVersion());

		return headers;
	}
}
