/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.cloud.gcp.data.firestore.it;

import java.io.IOException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firestore.v1.FirestoreGrpc;
import io.grpc.CallCredentials;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.auth.MoreCallCredentials;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.data.firestore.FirestoreTemplate;
import org.springframework.cloud.gcp.data.firestore.mapping.FirestoreMappingContext;
import org.springframework.cloud.gcp.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.cloud.gcp.data.firestore.transaction.ReactiveFirestoreTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring config for the integration tests.
 */
@Configuration
@PropertySource("application-test.properties")
@EnableReactiveFirestoreRepositories
public class FirestoreIntegrationTestsConfiguration {
	@Value("projects/${test.integration.firestore.project-id}/databases/(default)/documents")
	String defaultParent;

	@Bean
	FirestoreGrpc.FirestoreStub firestoreStub()  throws IOException {
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
		CallCredentials callCredentials = MoreCallCredentials.from(credentials);

		// Create a channel
		ManagedChannel channel = ManagedChannelBuilder
				.forAddress("firestore.googleapis.com", 443)
				.build();
		return FirestoreGrpc.newStub(channel).withCallCredentials(callCredentials);
	}

	@Bean
	public FirestoreTemplate firestoreTemplate(FirestoreGrpc.FirestoreStub firestoreStub) {

		return new FirestoreTemplate(firestoreStub, this.defaultParent);
	}

	@Bean
	public FirestoreMappingContext firestoreMappingContext() {
		return new FirestoreMappingContext();
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveFirestoreTransactionManager firestoreTransactionManager(
			FirestoreGrpc.FirestoreStub firestoreStub) {
		return new ReactiveFirestoreTransactionManager(firestoreStub, this.defaultParent);
	}

}
