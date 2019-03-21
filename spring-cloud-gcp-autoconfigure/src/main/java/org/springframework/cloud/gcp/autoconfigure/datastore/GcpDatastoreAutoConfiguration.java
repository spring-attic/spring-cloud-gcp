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

package org.springframework.cloud.gcp.autoconfigure.datastore;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import com.google.cloud.NoCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.UserAgentHeaderProvider;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreOperations;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTemplate;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreCustomConversions;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreServiceObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.convert.DefaultDatastoreEntityConverter;
import org.springframework.cloud.gcp.data.datastore.core.convert.ObjectToKeyFactory;
import org.springframework.cloud.gcp.data.datastore.core.convert.ReadWriteConversions;
import org.springframework.cloud.gcp.data.datastore.core.convert.TwoStepsConversions;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides Spring Data classes to use with Cloud Datastore.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.datastore.enabled", matchIfMissing = true)
@ConditionalOnClass({ DatastoreOperations.class, Datastore.class })
@EnableConfigurationProperties(GcpDatastoreProperties.class)
public class GcpDatastoreAutoConfiguration {

	private final String projectId;

	private final String namespace;

	private final Credentials credentials;

	private final String emulatorHost;

	GcpDatastoreAutoConfiguration(GcpDatastoreProperties gcpDatastoreProperties,
			GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {

		this.projectId = (gcpDatastoreProperties.getProjectId() != null)
				? gcpDatastoreProperties.getProjectId()
				: projectIdProvider.getProjectId();
		this.namespace = gcpDatastoreProperties.getNamespace();

		if (gcpDatastoreProperties.getEmulatorHost() == null) {
			this.credentials = (gcpDatastoreProperties.getCredentials().hasKey()
					? new DefaultCredentialsProvider(gcpDatastoreProperties)
					: credentialsProvider).getCredentials();
		}
		else {
			// Use empty credentials with Datastore Emulator.
			this.credentials = NoCredentials.getInstance();
		}

		this.emulatorHost = gcpDatastoreProperties.getEmulatorHost();
	}

	@Bean
	@ConditionalOnMissingBean
	public Datastore datastore() {
		DatastoreOptions.Builder builder = DatastoreOptions.newBuilder()
				.setProjectId(this.projectId)
				.setHeaderProvider(new UserAgentHeaderProvider(this.getClass()))
				.setCredentials(this.credentials);
		if (this.namespace != null) {
			builder.setNamespace(this.namespace);
		}

		if (emulatorHost != null) {
			builder.setHost(emulatorHost);
		}

		return builder.build().getService();
	}

	@Bean
	@ConditionalOnMissingBean
	public DatastoreCustomConversions datastoreCustomConversions() {
		return new DatastoreCustomConversions();
	}

	@Bean
	@ConditionalOnMissingBean
	public ReadWriteConversions datastoreReadWriteConversions(DatastoreCustomConversions customConversions,
			ObjectToKeyFactory objectToKeyFactory) {
		return new TwoStepsConversions(customConversions, objectToKeyFactory);
	}

	@Bean
	@ConditionalOnMissingBean
	public DatastoreMappingContext datastoreMappingContext() {
		return new DatastoreMappingContext();
	}

	@Bean
	@ConditionalOnMissingBean
	public ObjectToKeyFactory objectToKeyFactory(Datastore datastore) {
		return new DatastoreServiceObjectToKeyFactory(datastore);
	}

	@Bean
	@ConditionalOnMissingBean
	public DatastoreEntityConverter datastoreEntityConverter(DatastoreMappingContext datastoreMappingContext,
			ReadWriteConversions conversions) {
		return new DefaultDatastoreEntityConverter(datastoreMappingContext, conversions);
	}

	@Bean
	@ConditionalOnMissingBean
	public DatastoreTemplate datastoreTemplate(Datastore datastore, DatastoreMappingContext datastoreMappingContext,
			DatastoreEntityConverter datastoreEntityConverter, ObjectToKeyFactory objectToKeyFactory) {
		return new DatastoreTemplate(datastore, datastoreEntityConverter, datastoreMappingContext, objectToKeyFactory);
	}
}
