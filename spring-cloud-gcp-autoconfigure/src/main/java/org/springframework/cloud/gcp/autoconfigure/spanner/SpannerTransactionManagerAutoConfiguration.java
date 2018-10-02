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

import com.google.cloud.spanner.DatabaseClient;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gcp.data.spanner.core.SpannerTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Auto-configuration for {@link SpannerTransactionManager}.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@Configuration
@ConditionalOnClass(SpannerTransactionManager.class)
@AutoConfigureBefore(TransactionAutoConfiguration.class)
@ConditionalOnProperty(value = "spring.cloud.gcp.spanner.transactional-annotation.enabled",
		matchIfMissing = true)
@EnableConfigurationProperties(GcpSpannerProperties.class)
public class SpannerTransactionManagerAutoConfiguration {

	@Configuration
	@ConditionalOnSingleCandidate(DatabaseClient.class)
	static class DatabaseClientTransactionManagerConfiguration {

		private final DatabaseClient databaseClient;

		DatabaseClientTransactionManagerConfiguration(DatabaseClient databaseClient) {
			this.databaseClient = databaseClient;
		}

		@Bean
		@ConditionalOnMissingBean(PlatformTransactionManager.class)
		public SpannerTransactionManager spannerTransactionManager() {
			return new SpannerTransactionManager(this.databaseClient);
		}
	}
}
