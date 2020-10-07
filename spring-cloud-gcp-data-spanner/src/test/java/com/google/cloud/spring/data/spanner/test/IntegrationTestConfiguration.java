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

package com.google.cloud.spring.data.spanner.test;

import java.io.IOException;

import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spring.core.Credentials;
import com.google.cloud.spring.core.DefaultCredentialsProvider;
import com.google.cloud.spring.core.DefaultGcpProjectIdProvider;
import com.google.cloud.spring.data.spanner.core.SpannerMutationFactory;
import com.google.cloud.spring.data.spanner.core.SpannerMutationFactoryImpl;
import com.google.cloud.spring.data.spanner.core.SpannerTemplate;
import com.google.cloud.spring.data.spanner.core.SpannerTransactionManager;
import com.google.cloud.spring.data.spanner.core.admin.SpannerDatabaseAdminTemplate;
import com.google.cloud.spring.data.spanner.core.admin.SpannerSchemaUtils;
import com.google.cloud.spring.data.spanner.core.convert.ConverterAwareMappingSpannerEntityProcessor;
import com.google.cloud.spring.data.spanner.core.convert.SpannerEntityProcessor;
import com.google.cloud.spring.data.spanner.core.it.SpannerTemplateIntegrationTests.TemplateTransactionalService;
import com.google.cloud.spring.data.spanner.core.mapping.SpannerMappingContext;
import com.google.cloud.spring.data.spanner.repository.config.EnableSpannerRepositories;
import com.google.cloud.spring.data.spanner.repository.it.SpannerRepositoryIntegrationTests.TradeRepositoryTransactionalService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration for integration tets for Spanner.
 *
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

	private static final String TABLE_SUFFIX = String.valueOf(System.currentTimeMillis());

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
		catch (IOException ex) {
			throw new RuntimeException(ex);
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
				.setSessionPoolOption(SessionPoolOptions.newBuilder().setMaxSessions(10).build())
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
		return new SpannerTemplate(() -> databaseClient, mappingContext, spannerEntityProcessor,
				spannerMutationFactory, spannerSchemaUtils);
	}

	@Bean
	public SpannerEntityProcessor spannerConverter(SpannerMappingContext mappingContext) {
		return new ConverterAwareMappingSpannerEntityProcessor(mappingContext);
	}

	@Bean
	public SpannerTransactionManager spannerTransactionManager(
			DatabaseClient databaseClient) {
		return new SpannerTransactionManager(() -> databaseClient);
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
		return new SpannerDatabaseAdminTemplate(databaseAdminClient, () -> databaseClient, () -> databaseId);
	}

	@Bean
	String tableNameSuffix() {
		return this.TABLE_SUFFIX;
	}
}
