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

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.core.Credentials;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactory;
import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactoryImpl;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
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
public class IntegrationTestConfiguration {

	@Value("${test.integration.spanner.db}")
	private String databaseName;

	@Value("${test.integration.spanner.instance}")
	private String instanceId;

	@Bean
	public String getDatabaseName() {
		return this.databaseName;
	}

	@Bean
	public String getInstanceId() {
		return this.instanceId;
	}

	@Bean
	public String getProjectId() {
		return new DefaultGcpProjectIdProvider().getProjectId();
	}

	@Bean
	public com.google.auth.Credentials getCredentials() {

		try {
			return new DefaultCredentialsProvider(Credentials::new)
					.getCredentials();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public SpannerOptions spannerOptions() {
		return SpannerOptions.newBuilder().setProjectId(getProjectId())
				.setCredentials(getCredentials()).build();
	}

	@Bean
	public DatabaseId databaseId() {
		return DatabaseId.of(getProjectId(), this.instanceId, this.databaseName);
	}

	@Bean
	public Spanner spanner(SpannerOptions spannerOptions) {
		return spannerOptions.getService();
	}

	@Bean
	public DatabaseClient spannerDatabaseClient(Spanner spanner, DatabaseId databaseId) {
		return spanner.getDatabaseClient(databaseId);
	}

	@Bean
	public SpannerMappingContext spannerMappingContext() {
		return new SpannerMappingContext();
	}

	@Bean
	public SpannerTemplate spannerTemplate(DatabaseClient databaseClient,
			SpannerMappingContext mappingContext, SpannerConverter spannerConverter,
			SpannerMutationFactory spannerMutationFactory) {
		return new SpannerTemplate(databaseClient, mappingContext, spannerConverter,
				spannerMutationFactory);
	}

	@Bean
	public SpannerConverter spannerConverter(SpannerMappingContext mappingContext) {
		return new MappingSpannerConverter(mappingContext);
	}

	@Bean
	public SpannerMutationFactory spannerMutationFactory(
			SpannerConverter spannerConverter,
			SpannerMappingContext spannerMappingContext) {
		return new SpannerMutationFactoryImpl(spannerConverter, spannerMappingContext);
	}

	@Bean
	public DatabaseAdminClient databaseAdminClient(Spanner spanner) {
		return spanner.getDatabaseAdminClient();
	}

	@Bean
	public SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate(
			DatabaseAdminClient databaseAdminClient, DatabaseId databaseId) {
		return new SpannerDatabaseAdminTemplate(databaseAdminClient, databaseId);
	}
}
