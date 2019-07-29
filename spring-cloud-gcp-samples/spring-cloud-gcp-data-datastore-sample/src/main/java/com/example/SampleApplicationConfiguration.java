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

package com.example;

import java.util.Arrays;

import org.springframework.cloud.gcp.autoconfigure.datastore.DatastoreProvider;
import org.springframework.cloud.gcp.data.datastore.core.DatastoreTransactionManager;
import org.springframework.cloud.gcp.data.datastore.core.convert.DatastoreCustomConversions;
import org.springframework.cloud.gcp.data.datastore.repository.config.EnableDatastoreAuditing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring Boot configuration for the sample application enabling features like Transactional management and auditing.
 *
 * @author Chengyuan Zhao
 */
@Configuration
@EnableTransactionManagement
@EnableDatastoreAuditing
public class SampleApplicationConfiguration {

	@Bean
	DatastoreTransactionManager datastoreTransactionManager(DatastoreProvider datastore) {
		return new DatastoreTransactionManager(datastore);
	}

	@Bean
	public TransactionalRepositoryService transactionalRepositoryService() {
		return new TransactionalRepositoryService();
	}

	@Bean
	public DatastoreCustomConversions datastoreCustomConversions() {
		return new DatastoreCustomConversions(Arrays.asList(
				// Converters to read and write custom Singer.Album type
				ConvertersExample.ALBUM_STRING_CONVERTER,
				ConvertersExample.STRING_ALBUM_CONVERTER));
	}
}
