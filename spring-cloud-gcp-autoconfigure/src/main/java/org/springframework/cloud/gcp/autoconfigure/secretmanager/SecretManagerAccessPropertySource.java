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

package org.springframework.cloud.gcp.autoconfigure.secretmanager;

import com.google.api.gax.rpc.NotFoundException;
import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretVersionName;
import com.google.protobuf.ByteString;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.core.env.EnumerablePropertySource;


/**
 * A property source for Secret Manager which accesses the Secret Manager APIs when {@link #getProperty} is called.
 *
 * @author Daniel Zou
 * @since 1.2.3
 */
public class SecretManagerAccessPropertySource extends EnumerablePropertySource<SecretManagerServiceClient> {

	private final GcpProjectIdProvider projectIdProvider;

	public SecretManagerAccessPropertySource(
			String propertySourceName,
			SecretManagerServiceClient client,
			GcpProjectIdProvider projectIdProvider) {
		super(propertySourceName, client);

		this.projectIdProvider = projectIdProvider;
	}

	@Override
	public Object getProperty(String name) {
		SecretManagerPropertyIdentifier secretIdentifier =
				SecretManagerPropertyIdentifier.parseFromProperty(name, this.projectIdProvider);

		if (secretIdentifier != null) {
			return getSecretPayload(secretIdentifier);
		}
		else {
			return null;
		}
	}

	/**
	 * The {@link SecretManagerAccessPropertySource} is not enumerable, so this always returns an empty array.
	 * @return the empty array.
	 */
	@Override
	public String[] getPropertyNames() {
		return new String[0];
	}

	private ByteString getSecretPayload(SecretManagerPropertyIdentifier secretIdentifier) {

		try {
			SecretVersionName secretVersionName = SecretVersionName.newBuilder()
					.setProject(secretIdentifier.getProjectId())
					.setSecret(secretIdentifier.getSecretId())
					.setSecretVersion(secretIdentifier.getVersion())
					.build();

			AccessSecretVersionResponse response = getSource().accessSecretVersion(secretVersionName);
			return response.getPayload().getData();
		}
		catch (NotFoundException e) {
			return null;
		}
	}
}
