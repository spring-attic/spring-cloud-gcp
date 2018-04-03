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

package org.springframework.cloud.gcp.autoconfigure.spanner;

import java.io.IOException;
import java.nio.file.Files;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 * @author João André Martins
 */
public class GcpSpannerAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(GcpSpannerAutoConfiguration.class,
					GcpContextAutoConfiguration.class,
					SpannerRepositoriesAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.spanner.project-id=test-project",
					"spring.cloud.gcp.spanner.instance-id=testInstance",
					"spring.cloud.gcp.spanner.database=testDatabase");

	@Test
	public void testSpannerOperationsCreated() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(SpannerOperations.class)).isNotNull();
		});
	}

	@Test
	public void testTestRepositoryCreated() {
		this.contextRunner.run(context -> {
			assertThat(context.getBean(TestRepository.class)).isNotNull();
		});
	}

	@Test
	public void testCfCredentials() throws IOException {
		Resource vcapServicesFile = new ClassPathResource("VCAP_SERVICES");
		String vcapServicesString =
				new String(Files.readAllBytes(vcapServicesFile.getFile().toPath()));
		this.contextRunner.withSystemProperties("VCAP_SERVICES=" + vcapServicesString)
				.run(context -> {
					GcpSpannerAutoConfiguration spannerAutoConfiguration =
							context.getBean(GcpSpannerAutoConfiguration.class);
					ServiceAccountCredentials credentials =
							(ServiceAccountCredentials) new DirectFieldAccessor(
									spannerAutoConfiguration)
									.getPropertyValue("credentials");
					assertThat(credentials.getClientEmail()).isEqualTo(
							"pcf-binding-2e9720a8@graphite-test-spring-cloud-gcp.iam."
									+ "gserviceaccount.com");
				});
	}

	@AutoConfigurationPackage
	static class TestConfiguration {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}
	}
}
