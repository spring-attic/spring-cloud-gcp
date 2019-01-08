/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.storage.it;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import com.google.api.gax.core.CredentialsProvider;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.storage.GoogleStorageProtocolResolver;
import org.springframework.cloud.gcp.storage.GoogleStorageResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assume.assumeThat;

/**
 * Integration for Google Cloud Storage.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
		GoogleStorageIntegrationTests.GoogleStorageIntegrationTestsConfiguration.class })
public class GoogleStorageIntegrationTests {

	private static final String CHILD_RELATIVE_NAME = "child";

	@Autowired
	private Storage storage;

	@Value("gs://${test.integration.storage.bucket}/integration-test")
	private Resource resource;

	@BeforeClass
	public static void checkToRun() {
		assumeThat(
				"Storage integration tests are disabled. Please use '-Dit.storage=true' "
						+ "to enable them. ",
				System.getProperty("it.storage"), is("true"));
	}

	private GoogleStorageResource thisResource() {
		return (GoogleStorageResource) this.resource;
	}

	private GoogleStorageResource getChildResource() throws IOException {
		return thisResource().createRelative(CHILD_RELATIVE_NAME);
	}

	private void deleteResource(GoogleStorageResource googleStorageResource)
			throws IOException {
		if (googleStorageResource.exists()) {
			this.storage.delete(googleStorageResource.getBlob().getBlobId());
		}
	}

	@Before
	public void setUp() throws IOException {
		deleteResource(thisResource());
		deleteResource(getChildResource());
	}

	@Test
	public void createAndWriteTest() throws IOException {

		String message = "test message";

		try (OutputStream os = thisResource().getOutputStream()) {
			os.write(message.getBytes());
		}

		assertThat(this.resource.exists()).isTrue();

		try (InputStream is = this.resource.getInputStream()) {
			assertThat(StreamUtils.copyToString(is, Charset.defaultCharset())).isEqualTo(message);
		}

		GoogleStorageResource childResource = getChildResource();

		assertThat(childResource.exists()).isFalse();

		try (OutputStream os = childResource.getOutputStream()) {
			os.write(message.getBytes());
		}

		assertThat(childResource.exists()).isTrue();

		try (InputStream is = childResource.getInputStream()) {
			assertThat(StreamUtils.copyToString(is, Charset.defaultCharset())).isEqualTo(message);
		}
	}

	/**
	 * Spring config for the tests.
	 */
	@Configuration
	@PropertySource("application-test.properties")
	@Import(GoogleStorageProtocolResolver.class)
	public static class GoogleStorageIntegrationTestsConfiguration {

		@Bean
		public static Storage storage(CredentialsProvider credentialsProvider,
				GcpProjectIdProvider projectIdProvider) throws IOException {
			return StorageOptions.newBuilder()
					.setCredentials(credentialsProvider.getCredentials())
					.setProjectId(projectIdProvider.getProjectId()).build().getService();
		}

		@Bean
		public GcpProjectIdProvider gcpProjectIdProvider() {
			return new DefaultGcpProjectIdProvider();
		}

		@Bean
		public CredentialsProvider credentialsProvider() {
			try {
				return new DefaultCredentialsProvider(Credentials::new);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}
}
