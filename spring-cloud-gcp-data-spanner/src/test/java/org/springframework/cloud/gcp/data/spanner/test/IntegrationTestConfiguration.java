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

package org.springframework.cloud.gcp.data.spanner.test;

import java.io.IOException;
import java.util.function.Supplier;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpScope;
import org.springframework.cloud.gcp.data.spanner.config.AbstractSpannerConfiguration;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.repository.config.EnableSpannerRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * @author Balint Pato
 */

@Configuration
@PropertySource("application-test.properties")
@EnableSpannerRepositories
public class IntegrationTestConfiguration extends AbstractSpannerConfiguration {

	@Value("${test.integration.spanner.db}")
	private String databaseName;

	@Value("${test.integration.spanner.instance}")
	private String instanceId;

	@Override
	protected String getDatabaseName() {
		return this.databaseName;
	}

	@Override
	protected String getInstanceId() {
		return this.instanceId;
	}

	@Override
	protected String getProjectId() {
		return getProjectIdProvider().getProjectId();
	}

	@Override
	protected com.google.auth.Credentials getCredentials() {
		return mockFailures(com.google.auth.Credentials.class,
				() -> {
					try {
						return new DefaultCredentialsProvider(() -> new Credentials(GcpScope.SPANNER.getUrl()))
								.getCredentials();
					}
					catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
	}

	@Override
	public Spanner spanner() {
		return mockFailures(Spanner.class, super::spanner);
	}

	@Override
	public Spanner spanner(SpannerOptions spannerOptions) {
		return mockFailures(Spanner.class, () -> super.spanner(spannerOptions));
	}

	@Override
	public DatabaseClient spannerDatabaseClient() {
		return mockFailures(DatabaseClient.class, super::spannerDatabaseClient);
	}

	@Override
	public SpannerOperations spannerOperations() {
		return mockFailures(SpannerOperations.class, super::spannerOperations);
	}

	@Override
	public SpannerOptions spannerOptions() {
		return mockFailures(SpannerOptions.class, super::spannerOptions);
	}

	@Bean
	public GcpProjectIdProvider getProjectIdProvider() {
		return new DefaultGcpProjectIdProvider();
	}

	@Bean
	public DatabaseAdminClient getDatabaseAdminClient() {
		return mockFailures(DatabaseAdminClient.class,
				() -> spanner().getDatabaseAdminClient());
	}

	@Override
	public DatabaseClient spannerDatabaseClient(Spanner spanner, DatabaseId databaseId) {
		return mockFailures(DatabaseClient.class, () -> super.spannerDatabaseClient(spanner, databaseId));
	}

	@Bean
	public SkipWhenNoSpanner getAssumingSpanner() {
		return new SkipWhenNoSpanner(getDatabaseAdminClient(), getInstanceId());
	}

	protected <T> T mockFailures(Class<T> beanClass, Supplier<T> function) {
		try {
			return function.get();
		}
		catch (Throwable e) {
			return Mockito.mock(beanClass, invocationOnMock -> {
				throw new RuntimeException("Error during integration test setup.", e);
			});
		}
	}

}
