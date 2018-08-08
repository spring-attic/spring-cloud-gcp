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
import com.google.cloud.spanner.SessionPoolOptions;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.SpannerOptions.Builder;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.UsageTrackingHeaderProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * If <code>spring.cloud.gcp.spanner.emulator-host</code> is set, spring will connect to a
 * Spanner emulator instead of a real Cloud Spanner instance.
 *
 * @author Andreas Berger
 * @author Olav Loite
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.cloud.gcp.spanner", name = "emulator-host")
@AutoConfigureBefore(GcpSpannerAutoConfiguration.class)
@EnableConfigurationProperties(GcpSpannerProperties.class)
public class GcpSpannerEmulatorConfiguration {

	private final GcpSpannerProperties gcpSpannerProperties;

	private final Credentials credentials;

	GcpSpannerEmulatorConfiguration(GcpSpannerProperties gcpSpannerProperties,
			CredentialsProvider credentialsProvider) throws IOException {
		this.gcpSpannerProperties = gcpSpannerProperties;
		this.credentials = (gcpSpannerProperties.getCredentials().hasKey()
				? new DefaultCredentialsProvider(gcpSpannerProperties)
				: credentialsProvider).getCredentials();
	}

	@Bean
	@ConditionalOnMissingBean
	public SpannerOptions spannerOptions(SessionPoolOptions sessionPoolOptions) {
		Builder builder = SpannerOptions.newBuilder()
				.setProjectId(this.gcpSpannerProperties.getProjectId())
				.setHeaderProvider(new UsageTrackingHeaderProvider(this.getClass()))
				.setCredentials(this.credentials);
		builder.setHost(this.gcpSpannerProperties.getEmulatorHost());
		if (this.gcpSpannerProperties.getNumRpcChannels() >= 0) {
			builder.setNumChannels(this.gcpSpannerProperties.getNumRpcChannels());
		}
		if (this.gcpSpannerProperties.getPrefetchChunks() >= 0) {
			builder.setPrefetchChunks(this.gcpSpannerProperties.getPrefetchChunks());
		}
		builder.setSessionPoolOption(sessionPoolOptions);
		return builder.build();
	}
}
