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

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.autoconfigure.core.GcpContextAutoConfiguration;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.data.spanner.config.AbstractSpannerConfiguration;
import org.springframework.cloud.gcp.data.spanner.core.SpannerMutationFactory;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerConverter;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.context.annotation.Configuration;

/**
 * Provides Spring Data classes to use with Google Spanner.
 *
 * @author Chengyuan Zhao
 */
@Configuration
@AutoConfigureAfter(GcpContextAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.spanner.enabled", matchIfMissing = true)
@ConditionalOnClass({ SpannerMappingContext.class, SpannerOperations.class,
		SpannerMutationFactory.class, SpannerConverter.class })
@EnableConfigurationProperties(GcpSpannerProperties.class)
public class GcpSpannerAutoConfiguration extends AbstractSpannerConfiguration {

	private final String projectId;

	private final String instanceId;

	private final String databaseName;

	private final Credentials credentials;

	public GcpSpannerAutoConfiguration(GcpSpannerProperties gcpSpannerProperties,
			GcpProjectIdProvider projectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {
		this.credentials = (gcpSpannerProperties.getCredentials().getLocation() != null
				? new DefaultCredentialsProvider(gcpSpannerProperties)
				: credentialsProvider).getCredentials();
		this.projectId = gcpSpannerProperties.getProjectId() != null
				? gcpSpannerProperties.getProjectId()
				: projectIdProvider.getProjectId();
		this.instanceId = gcpSpannerProperties.getInstanceId();
		this.databaseName = gcpSpannerProperties.getDatabase();
	}


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
		return this.projectId;
	}

	@Override
	protected Credentials getCredentials() {
		return this.credentials;
	}

}
