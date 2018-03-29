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

package org.springframework.cloud.gcp.autoconfigure.storage;

import java.io.IOException;
import java.nio.file.Files;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author João André Martins
 */
public class GcpStorageAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpStorageAutoConfiguration.class,
					GcpContextAutoConfiguration.class))
			.withPropertyValues("spring.cloud.gcp.project-id=hollow light of the sealed land")
			.withUserConfiguration(MockCredsConfiguration.class);

	@Test
	public void testCfCredentials() throws IOException {
		Resource vcapServicesFile = new ClassPathResource("VCAP_SERVICES");
		String vcapServicesString =
				new String(Files.readAllBytes(vcapServicesFile.getFile().toPath()));
		this.contextRunner.withPropertyValues("VCAP_SERVICES=" + vcapServicesString)
				.run(context -> {
					GcpStorageAutoConfiguration autoConfiguration =
							context.getBean(GcpStorageAutoConfiguration.class);
					ServiceAccountCredentials credentials =
							(ServiceAccountCredentials) new DirectFieldAccessor(autoConfiguration)
									.getPropertyValue("CREDENTIALS");
					assertThat(credentials.getClientEmail()).isEqualTo("pcf-binding-5f5e625a@"
							+ "graphite-test-spring-cloud-gcp.iam.gserviceaccount.com");
				});
	}

	static class MockCredsConfiguration {

		@Bean
		public CredentialsProvider mockCredentialsProvider() {
			return () -> mock(Credentials.class);
		}
	}
}
