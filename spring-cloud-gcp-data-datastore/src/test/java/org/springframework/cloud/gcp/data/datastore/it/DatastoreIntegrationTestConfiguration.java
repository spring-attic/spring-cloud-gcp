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

package org.springframework.cloud.gcp.data.datastore.it;

import java.io.IOException;

import com.google.auth.Credentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreServiceObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.convert.DefaultDatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.repository.config.EnableDatastoreRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Chengyuan Zhao
 */
@Configuration
@PropertySource("application-test.properties")
@EnableDatastoreRepositories
@EnableTransactionManagement
public class DatastoreIntegrationTestConfiguration {

	private final String projectId = new DefaultGcpProjectIdProvider().getProjectId();

	private final Credentials credentials = new DefaultCredentialsProvider(
			org.springframework.cloud.gcp.core.Credentials::new).getCredentials();

	@Value("${test.integration.datastore.namespacePrefix}")
	private String namespacePrefix;

	public DatastoreIntegrationTestConfiguration() throws IOException {
	}

	@Bean
	public TransactionalTemplateService transactionalTemplateService() {
		return new TransactionalTemplateService();
	}

	@Bean
	DatastoreTransactionManager datastoreTransactionManager(Datastore datastore) {
		return new DatastoreTransactionManager(datastore);
	}

	@Bean
	public Datastore datastore() {
		DatastoreOptions.Builder builder = DatastoreOptions.newBuilder()
				.setProjectId(this.projectId)
				.setHeaderProvider(new UsageTrackingHeaderProvider(this.getClass()))
				.setCredentials(this.credentials);
		if (this.namespacePrefix != null) {
			builder.setNamespace(this.namespacePrefix + System.currentTimeMillis());
		}
		return builder.build().getService();
	}

	@Bean
	public DatastoreMappingContext datastoreMappingContext() {
		return new DatastoreMappingContext();
	}

	@Bean
	public DatastoreEntityConverter datastoreEntityConverter(
			DatastoreMappingContext datastoreMappingContext, ObjectToKeyFactory objectToKeyFactory) {
		return new DefaultDatastoreEntityConverter(datastoreMappingContext, objectToKeyFactory);
	}

	@Bean
	public ObjectToKeyFactory objectToKeyFactory(Datastore datastore) {
		return new DatastoreServiceObjectToKeyFactory(datastore);
	}

	@Bean
	public DatastoreTemplate datastoreTemplate(Datastore datastore,
			DatastoreMappingContext datastoreMappingContext,
			DatastoreEntityConverter datastoreEntityConverter,
			ObjectToKeyFactory objectToKeyFactory) {
		return new DatastoreTemplate(datastore, datastoreEntityConverter,
				datastoreMappingContext, objectToKeyFactory);
	}
}
