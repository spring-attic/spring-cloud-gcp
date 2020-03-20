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
import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreDefaultClassMapper;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.transaction.ReactiveFirestoreTransactionManager;
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

	private static final UserAgentHeaderProvider USER_AGENT_HEADER_PROVIDER =
			new UserAgentHeaderProvider(GcpFirestoreAutoConfiguration.class);

	private final String projectId;

	private final CredentialsProvider credentialsProvider;

	private final String hostPort;

	private final String firestoreRootPath;

	GcpFirestoreAutoConfiguration(GcpFirestoreProperties gcpFirestoreProperties,
			GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {

		this.projectId = (gcpFirestoreProperties.getProjectId() != null)
				? gcpFirestoreProperties.getProjectId()
				: projectIdProvider.getProjectId();

		this.credentialsProvider = (gcpFirestoreProperties.getCredentials().hasKey()
				? new DefaultCredentialsProvider(gcpFirestoreProperties)
				: credentialsProvider);

		this.hostPort = gcpFirestoreProperties.getHostPort();
		this.firestoreRootPath = String.format(ROOT_PATH_FORMAT, this.projectId);
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreOptions firestoreOptions() {
		return FirestoreOptions.getDefaultInstance().toBuilder()
				.setCredentialsProvider(this.credentialsProvider)
				.setProjectId(this.projectId)
				.setHeaderProvider(USER_AGENT_HEADER_PROVIDER)
				.setChannelProvider(
						InstantiatingGrpcChannelProvider.newBuilder()
								.setEndpoint(this.hostPort)
								.build())
				.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public Firestore firestore(FirestoreOptions firestoreOptions) {
		return firestoreOptions.getService();
	}

	/**
	 * The Firestore reactive template and data repositories support auto-configuration.
	 */
	@ConditionalOnClass({ FirestoreGrpc.FirestoreStub.class, Flux.class })
	class FirestoreReactiveAutoConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public FirestoreGrpc.FirestoreStub firestoreGrpcStub(
				@Qualifier("firestoreManagedChannel") ManagedChannel firestoreManagedChannel) throws IOException {
			return FirestoreGrpc.newStub(firestoreManagedChannel)
					.withCallCredentials(MoreCallCredentials.from(
							GcpFirestoreAutoConfiguration.this.credentialsProvider.getCredentials()));
		}

		@Bean
		@ConditionalOnMissingBean
		public FirestoreMappingContext firestoreMappingContext() {
			return new FirestoreMappingContext();
		}

		@Bean
		@ConditionalOnMissingBean
		public FirestoreClassMapper getClassMapper() {
			return new FirestoreDefaultClassMapper();
		}

		@Bean
		@ConditionalOnMissingBean
		public FirestoreTemplate firestoreTemplate(FirestoreGrpc.FirestoreStub firestoreStub,
				FirestoreClassMapper classMapper) {
			return new FirestoreTemplate(firestoreStub, GcpFirestoreAutoConfiguration.this.firestoreRootPath,
					classMapper);
		}

		@Bean
		@ConditionalOnMissingBean
		public ReactiveFirestoreTransactionManager firestoreTransactionManager(
				FirestoreGrpc.FirestoreStub firestoreStub) {
			return new ReactiveFirestoreTransactionManager(firestoreStub,
					GcpFirestoreAutoConfiguration.this.firestoreRootPath);
		}

		@Bean
		@ConditionalOnMissingBean (name = "firestoreManagedChannel")
		public ManagedChannel firestoreManagedChannel() {
			return ManagedChannelBuilder
					.forTarget("dns:///" + GcpFirestoreAutoConfiguration.this.hostPort)
					.userAgent(USER_AGENT_HEADER_PROVIDER.getUserAgent())
					.build();
		}
	}
}
