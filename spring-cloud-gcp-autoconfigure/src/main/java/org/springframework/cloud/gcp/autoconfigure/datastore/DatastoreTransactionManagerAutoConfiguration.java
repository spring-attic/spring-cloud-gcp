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

package org.springframework.cloud.gcp.autoconfigure.datastore;

import com.google.cloud.datastore.Datastore;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Auto-configuration for {@link DatastoreTransactionManager}.
 *
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
@Configuration
@ConditionalOnClass(DatastoreTransactionManager.class)
@AutoConfigureBefore(TransactionAutoConfiguration.class)
public class DatastoreTransactionManagerAutoConfiguration {

	@Configuration
	static class DatastoreTransactionManagerConfiguration {

		private final Datastore datastore;

		private final TransactionManagerCustomizers transactionManagerCustomizers;

		DatastoreTransactionManagerConfiguration(Datastore datastore,
				ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
			this.datastore = datastore;
			this.transactionManagerCustomizers = transactionManagerCustomizers
					.getIfAvailable();
		}

		@Bean
		@ConditionalOnMissingBean(PlatformTransactionManager.class)
		public DatastoreTransactionManager datastoreTransactionManager() {
			DatastoreTransactionManager transactionManager = new DatastoreTransactionManager(
					this.datastore);
			if (this.transactionManagerCustomizers != null) {
				this.transactionManagerCustomizers.customize(transactionManager);
			}
			return transactionManager;
		}
	}
}
