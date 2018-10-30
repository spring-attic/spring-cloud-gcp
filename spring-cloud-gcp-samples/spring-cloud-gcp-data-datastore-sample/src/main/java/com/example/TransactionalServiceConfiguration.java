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

package com.example;

import com.google.cloud.datastore.Datastore;

import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot configuration for {@link TransactionalRepositoryService}.
 *
 * @author Chengyuan Zhao
 */
@Configuration
@EnableTransactionManagement
public class TransactionalServiceConfiguration {

	@Bean
	DatastoreTransactionManager datastoreTransactionManager(Datastore datastore) {
		return new DatastoreTransactionManager(datastore);
	}

	@Bean
	public TransactionalRepositoryService transactionalRepositoryService() {
		return new TransactionalRepositoryService();
	}
}
