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

package org.springframework.cloud.gcp.data.spanner.config;

import com.google.auth.Credentials;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;

import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactory;
import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactoryImpl;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTemplate;
import org.springframework.cloud.gcp.data.spanner.core.convert.MappingSpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.context.annotation.Bean;

/**
 * @author Balint Pato
 */
public abstract class AbstractSpannerConfiguration {

	protected abstract String getDatabaseName();

	protected abstract String getInstanceId();

	protected abstract String getProjectId();

	protected abstract Credentials getCredentials();

	@Bean
	public SpannerOptions spannerOptions() {
		return SpannerOptions.newBuilder().setProjectId(getProjectId())
				.setCredentials(getCredentials()).build();
	}

	@Bean
	public DatabaseId databaseId() {
		return DatabaseId.of(getProjectId(), getInstanceId(), getDatabaseName());
	}

	@Bean
	public Spanner spanner() {
		return spanner(spannerOptions());
	}

	@Bean
	public Spanner spanner(SpannerOptions spannerOptions) {
		return spannerOptions.getService();
	}

	@Bean
	public DatabaseClient spannerDatabaseClient() {
		return spannerDatabaseClient(spanner(), databaseId());
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
	public SpannerOperations spannerOperations() {
		SpannerMappingContext spannerMappingContext = spannerMappingContext();
		SpannerConverter spannerConverter = spannerConverter(spannerMappingContext);
		Spanner spanner = spanner(spannerOptions());
		DatabaseClient databaseClient = spannerDatabaseClient(spanner, databaseId());
		SpannerMutationFactory spannerMutationFactory = spannerMutationFactory(spannerConverter,
				spannerMappingContext);
		return new SpannerTemplate(databaseClient, spannerMappingContext, spannerConverter,
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
}
