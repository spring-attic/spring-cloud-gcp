/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.core;

import com.google.cloud.ServiceOptions;

/**
 * A project ID provider that wraps {@link ServiceOptions#getDefaultProjectId()}.
 *
 * @author João André Martins
 */
public class DefaultGcpProjectIdProvider implements GcpProjectIdProvider {

	/**
	 * {@link ServiceOptions#getDefaultProjectId()} checks for the project ID in the
	 * {@code GOOGLE_CLOUD_PROJECT} environment variable and the Metadata Server, among others.
	 *
	 * @return the project ID in the context
	 */
	@Override
	public String getProjectId() {
		return ServiceOptions.getDefaultProjectId();
	}
}
