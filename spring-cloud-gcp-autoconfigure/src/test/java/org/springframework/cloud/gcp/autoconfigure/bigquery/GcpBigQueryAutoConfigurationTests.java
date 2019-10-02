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

package org.springframework.cloud.gcp.autoconfigure.bigquery;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.bigquery.core.BigQueryTemplate;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class GcpBigQueryAutoConfigurationTests {

	private static final GoogleCredentials MOCK_CREDENTIALS = mock(GoogleCredentials.class);

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(
					GcpBigQueryAutoConfiguration.class, GcpContextAutoConfiguration.class))
			.withUserConfiguration(TestConfiguration.class)
			.withPropertyValues("spring.cloud.gcp.bigquery.project-id=test-project")
			.withPropertyValues("spring.cloud.gcp.bigquery.datasetName=test-dataset");

	@Test
	public void testSettingBigQueryOptions() {
		this.contextRunner.run((context) -> {
			BigQueryOptions bigQueryOptions = context.getBean(BigQuery.class).getOptions();
			assertThat(bigQueryOptions.getProjectId()).isEqualTo("test-project");
			assertThat(bigQueryOptions.getCredentials()).isEqualTo(MOCK_CREDENTIALS);

			BigQueryTemplate bigQueryTemplate = context.getBean(BigQueryTemplate.class);
			assertThat(bigQueryTemplate.getDatasetName()).isEqualTo("test-dataset");
		});
	}

	/**
	 * Spring Boot config for tests.
	 */
	@AutoConfigurationPackage
	static class TestConfiguration {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> MOCK_CREDENTIALS;
		}
	}
}
