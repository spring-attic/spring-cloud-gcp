/*
 * Copyright 2019-2019 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.firestore;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.TransportOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides classes to use with Cloud Firestore.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.2
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.firestore.enabled", matchIfMissing = true)
@ConditionalOnClass({ Firestore.class })
@EnableConfigurationProperties(GcpFirestoreProperties.class)
public class GcpFirestoreAutoConfiguration {

	private final String projectId;

	private final CredentialsProvider credentialsProvider;

	GcpFirestoreAutoConfiguration(GcpFirestoreProperties gcpFirestoreProperties,
			GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {

		this.projectId = (gcpFirestoreProperties.getProjectId() != null)
				? gcpFirestoreProperties.getProjectId()
				: projectIdProvider.getProjectId();

		this.credentialsProvider = (gcpFirestoreProperties.getCredentials().hasKey()
				? new DefaultCredentialsProvider(gcpFirestoreProperties)
				: credentialsProvider);
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreOptions firestoreOptions(ObjectProvider<TransportOptions> transportOptionsObjectProvider,
			ObjectProvider<TransportChannelProvider> transportChannelProviderObjectProvider) {
		FirestoreOptions.Builder builder = FirestoreOptions.getDefaultInstance().toBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.setProjectId(this.projectId);

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Firestore firestore(FirestoreOptions firestoreOptions) {
		return firestoreOptions.getService();
	}

}
