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

import com.google.api.gax.grpc.InstantiatingGrpcChannelProvider;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.spring.autoconfigure.core.GcpContextAutoConfiguration;
import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Tests for Firestore Emulator autoconfiguration.
 *
 * @author Daniel Zou
 */
public class GcpFirestoreEmulatorAutoConfigurationIntegrationTests {

	ApplicationContextRunner contextRunner =
			new ApplicationContextRunner()
					.withConfiguration(AutoConfigurations.of(
							GcpFirestoreEmulatorAutoConfiguration.class,
							GcpContextAutoConfiguration.class,
							GcpFirestoreAutoConfiguration.class));

	@BeforeClass
	public static void checkToRun() {
		assumeThat("Firestore emulator integration tests are disabled. "
						+ "Please use '-Dit.firestore=true' to enable them. ",
				System.getProperty("it.firestore"), is("true"));
	}

	@Test
	public void testAutoConfigurationEnabled() {
		contextRunner
				.withPropertyValues(
						"spring.cloud.gcp.firestore.emulator.enabled=true",
						"spring.cloud.gcp.firestore.host-port=localhost:9000")
				.run(context -> {
					FirestoreOptions firestoreOptions = context.getBean(FirestoreOptions.class);
					String endpoint =
							((InstantiatingGrpcChannelProvider)
									firestoreOptions.getTransportChannelProvider()).getEndpoint();
					assertThat(endpoint).isEqualTo("localhost:9000");

					FirestoreTemplate firestoreTemplate = context.getBean(FirestoreTemplate.class);
				});
	}

	@Test
	public void testAutoConfigurationDisabled() {
		contextRunner
				.run(context -> {
					FirestoreOptions firestoreOptions = context.getBean(FirestoreOptions.class);
					String endpoint =
							((InstantiatingGrpcChannelProvider)
									firestoreOptions.getTransportChannelProvider()).getEndpoint();
					assertThat(endpoint).isEqualTo("firestore.googleapis.com:443");

					FirestoreTemplate firestoreTemplate = context.getBean(FirestoreTemplate.class);
				});
	}
}
