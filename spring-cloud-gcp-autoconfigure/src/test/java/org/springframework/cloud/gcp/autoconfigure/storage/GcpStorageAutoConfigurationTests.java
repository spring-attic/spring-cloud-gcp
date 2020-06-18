/*
 * Copyright 2017-2018 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.storage;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.storage.GoogleStorageResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Config for Storage auto config tests.
 *
 * @author Artem Bilan
 * @author Elena Felder
 */
public class GcpStorageAutoConfigurationTests {

	private static final String PROJECT_NAME = "hollow-light-of-the-sealed-land";

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpStorageAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.storage.project-id=" + PROJECT_NAME)
			.withUserConfiguration(TestConfiguration.class);

	@Test
	public void testValidObject() throws Exception {
		this.contextRunner.run((context) -> {
			Resource resource = context.getBean("mockResource", Resource.class);
			assertThat(resource.contentLength()).isEqualTo(4096);
		});
	}

	@Test
	public void testAutoCreateFilesTrueByDefault() throws IOException {
		this.contextRunner
			.run((context) -> {
				Resource resource = context.getBean("mockResource", Resource.class);
				assertThat(((GoogleStorageResource) resource).isAutoCreateFiles()).isTrue();
			});
	}

	@Test
	public void testAutoCreateFilesRespectsProperty() throws IOException {

		this.contextRunner
			.withPropertyValues("spring.cloud.gcp.storage.auto-create-files=false")
			.run((context) -> {
				Resource resource = context.getBean("mockResource", Resource.class);
				assertThat(((GoogleStorageResource) resource).isAutoCreateFiles()).isFalse();
			});

	}

	@Configuration
	static class TestConfiguration {

		@Value("gs://test-spring/images/spring.png")
		private Resource remoteResource;

		@Bean(name = "mockResource")
		public Resource getResource() throws IOException {
			return this.remoteResource;
		}

		@Bean
		public static Storage mockStorage() throws Exception {
			Storage storage = mock(Storage.class);
			BlobId validBlob = BlobId.of("test-spring", "images/spring.png");
			Blob mockedBlob = mock(Blob.class);
			when(mockedBlob.exists()).thenReturn(true);
			when(mockedBlob.getSize()).thenReturn(4096L);
			when(storage.get(eq(validBlob))).thenReturn(mockedBlob);
			return storage;
		}

		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

		@Bean
		public static GcpProjectIdProvider gcpProjectIdProvider() {
			return () -> "default-project";
		}
	}

}
