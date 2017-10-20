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

package org.springframework.cloud.gcp.storage;

import java.util.Collections;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.gcp.core.AbstractCredentialsProperty;
import org.springframework.cloud.gcp.core.GcpScope;

/**
 * @author João André Martins
 */
@ConfigurationProperties("spring.cloud.gcp.storage")
public class GcpStorageProperties extends GoogleStorageProtocolResolverSettings {

	private Credentials credentials;

	public Credentials getCredentials() {
		return this.credentials;
	}

	public void setCredentials(Credentials credentials) {
		this.credentials = credentials;
	}

	public static class Credentials extends AbstractCredentialsProperty {
		public Credentials() {
			setScopes(Collections.singletonList(GcpScope.STORAGE_READ_WRITE.getUrl()));
		}
	}
}
