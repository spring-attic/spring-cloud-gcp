/*
 * Copyright 2017-2020 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.metrics;

import java.io.IOException;

import com.google.api.gax.core.CredentialsProvider;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import io.micrometer.stackdriver.StackdriverConfig;

import org.springframework.boot.actuate.autoconfigure.metrics.export.stackdriver.StackdriverMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.stackdriver.StackdriverProperties;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.core.DefaultCredentialsProvider;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides auto-detection for `project-id` and `credentials`.
 *
 * @author Eddú Meléndez
 * @since 1.2.4
 */
@Configuration
@AutoConfigureBefore(StackdriverMetricsExportAutoConfiguration.class)
@ConditionalOnClass({StepMeterRegistry.class, StackdriverConfig.class})
@ConditionalOnBean(Clock.class)
@EnableConfigurationProperties({GcpMetricsProperties.class, StackdriverProperties.class})
@ConditionalOnProperty(value = "spring.cloud.gcp.metrics.enabled", matchIfMissing = true, havingValue = "true")
public class GcpStackdriverMetricsAutoConfiguration {

	private final StackdriverProperties stackdriverProperties;

	private final String projectId;

	private final CredentialsProvider credentialsProvider;

	public GcpStackdriverMetricsAutoConfiguration(GcpMetricsProperties gcpMetricsProperties,
			StackdriverProperties stackdriverProperties, GcpProjectIdProvider gcpProjectIdProvider,
			CredentialsProvider credentialsProvider) throws IOException {
		this.stackdriverProperties = stackdriverProperties;
		this.projectId = (gcpMetricsProperties.getProjectId() != null)
				? gcpMetricsProperties.getProjectId() : gcpProjectIdProvider.getProjectId();
		this.credentialsProvider = gcpMetricsProperties.getCredentials().hasKey()
				? new DefaultCredentialsProvider(gcpMetricsProperties) : credentialsProvider;
	}

	@Bean
	@ConditionalOnMissingBean
	public StackdriverConfig stackdriverConfig() {
		return new GcpStackdriverPropertiesConfigAdapter(this.stackdriverProperties, this.projectId, this.credentialsProvider);
	}

}
