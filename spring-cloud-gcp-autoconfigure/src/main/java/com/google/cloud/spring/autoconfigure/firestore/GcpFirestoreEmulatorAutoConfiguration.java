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

package com.google.cloud.spring.autoconfigure.firestore;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.auth.Credentials;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.spring.autoconfigure.firestore.GcpFirestoreAutoConfiguration.FirestoreReactiveAutoConfiguration;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import com.google.cloud.spring.data.firestore.mapping.FirestoreClassMapper;
import com.google.cloud.spring.data.firestore.mapping.FirestoreMappingContext;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides autoconfiguration to use the Firestore emulator if enabled.
 *
 * @since 1.2.3
 * @author Daniel Zou
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.cloud.gcp.firestore.emulator.enabled")
@AutoConfigureBefore(GcpFirestoreAutoConfiguration.class)
@EnableConfigurationProperties(GcpFirestoreProperties.class)
public class GcpFirestoreEmulatorAutoConfiguration {
	private final String hostPort;
	private final String projectId;
	private final String rootPath;

	GcpFirestoreEmulatorAutoConfiguration(GcpFirestoreProperties properties) {
		this.hostPort = properties.getHostPort();
		this.projectId = StringUtils.defaultIfEmpty(properties.getProjectId(), "unused");
		this.rootPath = String.format("projects/%s/databases/(default)", this.projectId);
	}

	@Bean
	@ConditionalOnMissingBean
	public FirestoreOptions firestoreOptions() {
		return FirestoreOptions.newBuilder()
				.setCredentials(emulatorCredentials())
				.setProjectId(projectId)
				.setChannelProvider(
						InstantiatingGrpcChannelProvider.newBuilder()
								.setEndpoint(this.hostPort)
								.setChannelConfigurator(input -> input.usePlaintext())
								.build())
				.build();
	}

	private Credentials emulatorCredentials() {
		final Map<String, List<String>> headerMap = new HashMap<>();
		headerMap.put("Authorization", Collections.singletonList("Bearer owner"));
		headerMap.put(
				"google-cloud-resource-prefix", Collections.singletonList(rootPath));

		return new Credentials() {
			@Override
			public String getAuthenticationType() {
				return null;
			}

			@Override
			public Map<String, List<String>> getRequestMetadata(URI uri) {
				return headerMap;
			}

			@Override
			public boolean hasRequestMetadata() {
				return true;
			}

			@Override
			public boolean hasRequestMetadataOnly() {
				return true;
			}

			@Override
			public void refresh() {
				// no-op
			}
		};
	}

	/**
	 * Reactive Firestore autoconfiguration to enable emulator use.
	 */
	@ConditionalOnClass({ FirestoreGrpc.FirestoreStub.class, Flux.class })
	@AutoConfigureBefore(FirestoreReactiveAutoConfiguration.class)
	class ReactiveFirestoreEmulatorAutoConfiguration {
		@Bean
		@ConditionalOnMissingBean
		public FirestoreTemplate firestoreTemplate(FirestoreGrpc.FirestoreStub firestoreStub,
				FirestoreClassMapper classMapper, FirestoreMappingContext firestoreMappingContext) {
			FirestoreTemplate template = new FirestoreTemplate(
					firestoreStub,
					rootPath + "/documents",
					classMapper,
					firestoreMappingContext);
			return template;
		}

		@Bean
		@ConditionalOnMissingBean
		public FirestoreGrpc.FirestoreStub firestoreGrpcStub() throws IOException {
			ManagedChannel channel = ManagedChannelBuilder
					.forTarget(GcpFirestoreEmulatorAutoConfiguration.this.hostPort)
					.usePlaintext()
					.build();

			return FirestoreGrpc.newStub(channel)
					.withCallCredentials(MoreCallCredentials.from(emulatorCredentials()))
					.withExecutor(Runnable::run);
		}
	}
}
