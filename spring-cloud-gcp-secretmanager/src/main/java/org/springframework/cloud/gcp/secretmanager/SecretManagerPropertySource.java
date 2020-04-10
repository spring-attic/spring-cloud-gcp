/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.gcp.secretmanager;

import com.google.cloud.secretmanager.v1beta1.SecretVersionName;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * A property source for Secret Manager which accesses the Secret Manager APIs when {@link #getProperty} is called.
 *
 * @author Daniel Zou
 * @author Eddú Meléndez
 * @since 1.2.2
 */
public class SecretManagerPropertySource extends EnumerablePropertySource<SecretManagerTemplate> {

	private final GcpProjectIdProvider projectIdProvider;

	public SecretManagerPropertySource(
			String propertySourceName,
			SecretManagerTemplate secretManagerTemplate,
			GcpProjectIdProvider projectIdProvider) {
		super(propertySourceName, secretManagerTemplate);

		this.projectIdProvider = projectIdProvider;
	}

	@Override
	public Object getProperty(String name) {
		SecretVersionName secretIdentifier =
				SecretManagerPropertyUtils.getSecretVersionName(name, this.projectIdProvider);

		if (secretIdentifier != null) {
			return getSource().getSecretByteString(secretIdentifier);
		}
		else {
			return null;
		}
	}

	/**
	 * The {@link SecretManagerPropertySource} is not enumerable, so this always returns an empty array.
	 * @return the empty array.
	 */
	@Override
	public String[] getPropertyNames() {
		return new String[0];
	}
}
