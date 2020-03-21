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

import java.util.HashMap;
import java.util.Map;

import com.google.cloud.secretmanager.v1beta1.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta1.ProjectName;
import com.google.cloud.secretmanager.v1beta1.Secret;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta1.SecretManagerServiceClient.ListSecretsPagedResponse;
import com.google.cloud.secretmanager.v1beta1.SecretVersionName;
import com.google.protobuf.ByteString;

import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.util.StringUtils;

/**
 * Retrieves secrets from GCP Secret Manager under the current GCP project.
 *
 * @author Daniel Zou
 * @author Eddú Meléndez
 * @since 1.2.2
 */
public class SecretManagerPropertySource extends EnumerablePropertySource<SecretManagerServiceClient> {

	private static final String LATEST_VERSION_STRING = "latest";

	private final Map<String, Object> properties;

	private final String[] propertyNames;

	public SecretManagerPropertySource(
			String propertySourceName,
			SecretManagerServiceClient client,
			GcpProjectIdProvider projectIdProvider,
			String secretsPrefix) {
		super(propertySourceName, client);

		Map<String, Object> propertiesMap = createSecretsPropertiesMap(
				client, projectIdProvider.getProjectId(), secretsPrefix, null, null);

		this.properties = propertiesMap;
		this.propertyNames = propertiesMap.keySet().toArray(new String[propertiesMap.size()]);
	}

	public SecretManagerPropertySource(
			String propertySourceName,
			SecretManagerServiceClient client,
			GcpProjectIdProvider projectIdProvider,
			String secretsPrefix,
			String version,
			Map<String, String> versions) {
		super(propertySourceName, client);

		Map<String, Object> propertiesMap = createSecretsPropertiesMap(
				client, projectIdProvider.getProjectId(), secretsPrefix, version, versions);

		this.properties = propertiesMap;
		this.propertyNames = propertiesMap.keySet().toArray(new String[propertiesMap.size()]);
	}

	@Override
	public String[] getPropertyNames() {
		return propertyNames;
	}

	@Override
	public Object getProperty(String name) {
		return properties.get(name);
	}

	private static Map<String, Object> createSecretsPropertiesMap(
			SecretManagerServiceClient client, String projectId, String secretsPrefix, String version, Map<String, String> versions) {

		ListSecretsPagedResponse response = client.listSecrets(ProjectName.of(projectId));

		Map<String, Object> secretsMap = new HashMap<>();
		for (Secret secret : response.iterateAll()) {
			String secretId = extractSecretId(secret);
			ByteString secretPayload = resolveSecretVersion(client, projectId, version, versions, secretId);
			secretsMap.put(secretsPrefix + secretId, secretPayload);
		}

		return secretsMap;
	}

	private static ByteString resolveSecretVersion(SecretManagerServiceClient client, String projectId, String version, Map<String, String> versions, String secretId) {
		if (!versions.isEmpty() && versions.containsKey(secretId)) {
			String secretVersion = versions.get(secretId);
			return getSecretPayload(client, projectId, secretId, secretVersion);
		}
		return getSecretPayload(client, projectId, secretId, resolveVersion(version));
	}

	private static String resolveVersion(String version) {
		return StringUtils.hasText(version) ? version : LATEST_VERSION_STRING;
	}

	private static ByteString getSecretPayload(
			SecretManagerServiceClient client, String projectId, String secretId, String version) {

		SecretVersionName secretVersionName = SecretVersionName.newBuilder()
				.setProject(projectId)
				.setSecret(secretId)
				.setSecretVersion(version)
				.build();

		AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
		return response.getPayload().getData();
	}

	/**
	 * Extracts the Secret ID from the {@link Secret}. The secret ID refers to the unique ID
	 * given to the secret when it is saved under a GCP project.
	 *
	 * <p>
	 * The secret ID is extracted from the full secret name of the form:
	 * projects/${PROJECT_ID}/secrets/${SECRET_ID}
	 */
	private static String extractSecretId(Secret secret) {
		String[] secretNameTokens = secret.getName().split("/");
		return secretNameTokens[secretNameTokens.length - 1];
	}
}
