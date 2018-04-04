/*
 *  Copyright 2018 original author or authors.
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

package org.springframework.cloud.gcp.core.cloudfoundry;

import java.io.IOException;
import java.nio.file.Files;

import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author João André Martins
 */
public class DefaultCfConfigurationTests {

	@Test
	public void testCfPubSubCredentials() throws IOException {
		CfConfiguration cfConfiguration = new DefaultCfConfiguration(
				new String(Files.readAllBytes(
						new ClassPathResource("VCAP_SERVICES").getFile().toPath())));

		ServiceAccountCredentials credentials =
				(ServiceAccountCredentials) cfConfiguration.getCloudSqlMySqlCredentialsProvider()
						.getCredentials();
		assertThat(credentials.getClientEmail()).isEqualTo(
				"pcf-binding-f3a128f5@graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");

		credentials = (ServiceAccountCredentials) cfConfiguration
				.getCloudSqlPostgreSqlCredentialsProvider().getCredentials();
		assertThat(credentials.getClientEmail()).isEqualTo(
				"pcf-binding-fa6bb781@graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");

		credentials = (ServiceAccountCredentials) cfConfiguration.getPubSubCredentialsProvider()
				.getCredentials();
		assertThat(credentials.getClientEmail()).isEqualTo(
				"pcf-binding-3352ec74@graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");

		credentials = (ServiceAccountCredentials) cfConfiguration.getSpannerCredentialsProvider()
				.getCredentials();
		assertThat(credentials.getClientEmail()).isEqualTo(
				"pcf-binding-2e9720a8@graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");

		credentials = (ServiceAccountCredentials) cfConfiguration.getStorageCredentialsProvider()
				.getCredentials();
		assertThat(credentials.getClientEmail()).isEqualTo(
				"pcf-binding-5f5e625a@graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");

		credentials = (ServiceAccountCredentials) cfConfiguration.getTraceCredentialsProvider()
				.getCredentials();
		assertThat(credentials.getClientEmail()).isEqualTo(
				"pcf-binding-5df95a11@graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");
	}

	@Test
	public void testServiceBoundTwice() throws IOException {
		Resource vcapServicesFile = new ClassPathResource("VCAP_SERVICES_multiple_services");
		DefaultCfConfiguration cfConfiguration = new DefaultCfConfiguration(
				new String(Files.readAllBytes(vcapServicesFile.getFile().toPath())));

		try {
			cfConfiguration.getPubSubCredentialsProvider();
			fail("An exception should've been thrown because Pub/Sub is bound twice to the "
					+ "application, and we have no way to decide on the correct credentials.");
		}
		catch (RuntimeException rte) {
			assertThat(rte.getMessage()).isEqualTo("The service google-pubsub can only be bound "
					+ "to an application once.");
		}
	}

	@Test
	public void testNoService() throws IOException {
		// This file lacks the "google-spanner" element.
		Resource vcapServicesFile = new ClassPathResource("VCAP_SERVICES_no_service");
		DefaultCfConfiguration cfConfiguration = new DefaultCfConfiguration(
				new String(Files.readAllBytes(vcapServicesFile.getFile().toPath())));

		try {
			cfConfiguration.getSpannerCredentialsProvider();
			fail("An exception should've been thrown because this JSON doesn't contain the "
					+ "google-spanner service element.");
		}
		catch (RuntimeException rte) {
			assertThat("The service google-spanner is not bound to this Cloud Foundry "
					+ "application.");
		}
	}
}
