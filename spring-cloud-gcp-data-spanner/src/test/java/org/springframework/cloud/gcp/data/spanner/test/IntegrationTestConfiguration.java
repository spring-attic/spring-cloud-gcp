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
import org.springframework.cloud.gcp.data.spanner.core.SpannerTransactionManager;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.ConverterAwareMappingSpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.it.SpannerTemplateIntegrationTests.TemplateTransactionalService;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.repository.config.EnableSpannerRepositories;
import org.springframework.cloud.gcp.data.spanner.repository.it.SpannerRepositoryIntegrationTests.TradeRepositoryTransactionalService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */

@Configuration
@EnableTransactionManagement
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
			return new DefaultCredentialsProvider(Credentials::new).getCredentials();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Bean
	public TemplateTransactionalService templateTransactionalService() {
		return new TemplateTransactionalService();
	}

	@Bean
	public TradeRepositoryTransactionalService tradeRepositoryTransactionalService() {
		return new TradeRepositoryTransactionalService();
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
			SpannerMappingContext mappingContext, SpannerEntityProcessor spannerEntityProcessor,
			SpannerMutationFactory spannerMutationFactory,
			SpannerSchemaUtils spannerSchemaUtils) {
		return new SpannerTemplate(databaseClient, mappingContext, spannerEntityProcessor,
				spannerMutationFactory, spannerSchemaUtils);
	}

	@Bean
	public SpannerEntityProcessor spannerConverter(SpannerMappingContext mappingContext) {
		return new ConverterAwareMappingSpannerEntityProcessor(mappingContext);
	}

	@Bean
	public SpannerTransactionManager spannerTransactionManager(
			DatabaseClient databaseClient) {
		return new SpannerTransactionManager(databaseClient);
	}

	@Bean
	public SpannerMutationFactory spannerMutationFactory(
			SpannerEntityProcessor spannerEntityProcessor,
			SpannerMappingContext spannerMappingContext,
			SpannerSchemaUtils spannerSchemaUtils) {
		return new SpannerMutationFactoryImpl(spannerEntityProcessor,
				spannerMappingContext, spannerSchemaUtils);
	}

	@Bean
	public DatabaseAdminClient databaseAdminClient(Spanner spanner) {
		return spanner.getDatabaseAdminClient();
	}

	@Bean
	public SpannerSchemaUtils spannerSchemaUtils(
			SpannerMappingContext spannerMappingContext,
			SpannerEntityProcessor spannerEntityProcessor) {
		return new SpannerSchemaUtils(spannerMappingContext, spannerEntityProcessor, true);
	}

	@Bean
	public SpannerDatabaseAdminTemplate spannerDatabaseAdminTemplate(
			DatabaseAdminClient databaseAdminClient, DatabaseClient databaseClient, DatabaseId databaseId) {
		return new SpannerDatabaseAdminTemplate(databaseAdminClient, databaseClient, databaseId);
	}
}
