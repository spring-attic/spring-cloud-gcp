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
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
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

	private static final String ROOT_PATH_FORMAT = "projects/%s/databases/(default)/documents";

	private final String projectId;

	private final CredentialsProvider credentialsProvider;

	private final String firestoreRootPath;

	private final UserAgentHeaderProvider headerProvider =
			new UserAgentHeaderProvider(GcpFirestoreAutoConfiguration.class);

	GcpFirestoreAutoConfiguration(GcpFirestoreProperties gcpFirestoreProperties,
			GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {

		this.projectId = (gcpFirestoreProperties.getProjectId() != null)
				? gcpFirestoreProperties.getProjectId()
				: projectIdProvider.getProjectId();

		this.credentialsProvider = (gcpFirestoreProperties.getCredentials().hasKey()
				? new DefaultCredentialsProvider(gcpFirestoreProperties)
				: credentialsProvider);

		this.firestoreRootPath = String.format(ROOT_PATH_FORMAT, this.projectId);
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreOptions firestoreOptions(ObjectProvider<TransportOptions> transportOptionsObjectProvider,
			ObjectProvider<TransportChannelProvider> transportChannelProviderObjectProvider) {
		FirestoreOptions.Builder builder = FirestoreOptions.getDefaultInstance().toBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.setProjectId(this.projectId)
				.setHeaderProvider(this.headerProvider);

		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Firestore firestore(FirestoreOptions firestoreOptions) {
		return firestoreOptions.getService();
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreMappingContext firestoreMappingContext() {
		return new FirestoreMappingContext();
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreTemplate firestoreTemplate() throws IOException {
		ManagedChannel channel = ManagedChannelBuilder
				.forAddress("firestore.googleapis.com", 443)
				.userAgent(this.headerProvider.getUserAgent())
				.build();

		FirestoreGrpc.FirestoreStub firestoreStub =
				FirestoreGrpc.newStub(channel)
						.withCallCredentials(
								MoreCallCredentials.from(this.credentialsProvider.getCredentials()));

		return new FirestoreTemplate(firestoreStub, this.firestoreRootPath);
	}
}
