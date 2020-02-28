/*
 * Copyright 2018-2019 the original author or authors.
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

package org.springframework.cloud.gcp.autoconfigure.pubsub.health;

import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.gcp.autoconfigure.pubsub.GcpPubSubAutoConfiguration;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link HealthContributorAutoConfiguration Auto-configuration} for
 * {@link PubSubHealthIndicator}.
 *
 * @author Vinicius Carvalho
 *
 * @since 1.2.2
 */
@Configuration
@ConditionalOnClass({HealthIndicator.class, PubSubTemplate.class})
@ConditionalOnEnabledHealthIndicator("pubsub")
@AutoConfigureBefore(HealthContributorAutoConfiguration.class)
@AutoConfigureAfter(GcpPubSubAutoConfiguration.class)
public class PubSubHealthIndicatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public PubSubHealthIndicator pubSubHealthIndicator(PubSubTemplate pubSubTemplate) {
		return new PubSubHealthIndicator(pubSubTemplate);
	}

}
