/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.example.PostgreSqlApplicationTests.PostgreSqlApplicationConfiguration;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.gcp.autoconfigure.sql.CloudSqlJdbcInfoProvider;
import org.springframework.cloud.gcp.autoconfigure.sql.DatabaseType;
import org.springframework.cloud.gcp.autoconfigure.sql.DefaultCloudSqlJdbcInfoProvider;
import org.springframework.cloud.gcp.autoconfigure.sql.GcpCloudSqlProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Runs the {@link SqlApplicationTestCase} on a Postgres database backend.
 *
 * @author Daniel Zou
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		webEnvironment = WebEnvironment.RANDOM_PORT,
		classes = { PostgreSqlApplicationConfiguration.class, SqlApplication.class },
		properties = {
				"spring.cloud.gcp.sql.database-name=code_samples_test_db",
				"spring.cloud.gcp.sql.instance-connection-name=spring-cloud-gcp-ci:us-central1:testpostgres",
				"spring.datasource.username=postgres",
				"spring.datasource.password=test",
				"spring.datasource.continue-on-error=true",
				"spring.datasource.initialization-mode=always"
		})
public class PostgreSqlApplicationTests extends SqlApplicationTestCase {

	@Configuration
	static class PostgreSqlApplicationConfiguration {
		@Bean
		public CloudSqlJdbcInfoProvider postgresJdbcInfoProvider(
				GcpCloudSqlProperties gcpCloudSqlProperties) {
			CloudSqlJdbcInfoProvider postgresJdbcInfoProvider = new DefaultCloudSqlJdbcInfoProvider(
					gcpCloudSqlProperties, DatabaseType.POSTGRESQL);
			return postgresJdbcInfoProvider;
		}
	}

}
