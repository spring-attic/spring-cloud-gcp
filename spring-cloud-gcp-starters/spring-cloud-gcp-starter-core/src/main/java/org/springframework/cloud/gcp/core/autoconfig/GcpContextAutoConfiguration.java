/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.cloud.gcp.core.autoconfig;

import java.util.List;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.gax.core.GoogleCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.DefaultGcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.core.GcpProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 *
 * Base starter for Google Cloud Projects. Provide defaults for {@link GoogleCredentials}.
 * Binds properties from {@link GcpProperties}
 *
 * @author Vinicius Carvalho
 * @author João André Martins
 */
@Configuration
@ConditionalOnClass(GoogleCredentials.class)
@EnableConfigurationProperties(GcpProperties.class)
public class GcpContextAutoConfiguration {

	private static final String PUBSUB_SCOPE = "https://www.googleapis.com/auth/pubsub";

	private static final String SQLADMIN_SCOPE =
			"https://www.googleapis.com/auth/sqlservice.admin";

	private static final List<String> SCOPES_LIST = ImmutableList.of(PUBSUB_SCOPE, SQLADMIN_SCOPE);

	@Autowired
	private GcpProperties gcpProperties;

	@Bean
	@ConditionalOnMissingBean
	public CredentialsProvider googleCredentials() throws Exception {
		if (!StringUtils.isEmpty(this.gcpProperties.getCredentialsLocation())) {
			return FixedCredentialsProvider
					.create(GoogleCredentials.fromStream(
							this.gcpProperties.getCredentialsLocation().getInputStream())
					.createScoped(SCOPES_LIST));
		}

		return GoogleCredentialsProvider.newBuilder()
				.setScopesToApply(SCOPES_LIST)
				.build();
	}

	/**
	 * @return a {@link GcpProjectIdProvider} that returns the project ID in the properties or, if
	 * none, the project ID from the GOOGLE_CLOUD_PROJECT envvar and Metadata Server
	 */
	@Bean
	@ConditionalOnMissingBean
	public GcpProjectIdProvider gcpProjectIdProvider() {
		return this.gcpProperties.getProjectId() != null
				? () -> this.gcpProperties.getProjectId()
				: new DefaultGcpProjectIdProvider();
	}
}
