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

package org.springframework.cloud.gcp.autoconfigure.Spanner;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.autoconfigure.spanner.GcpSpannerAutoConfiguration;
import org.springframework.cloud.gcp.core.GcpProjectIdProvider;
import org.springframework.cloud.gcp.data.spanner.core.SpannerOperations;
import org.springframework.cloud.gcp.data.spanner.repository.config.EnableSpannerRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { GcpSpannerAutoConfiguration.class }, properties = {
				"spring.cloud.gcp.spanner.projectId=testProject",
				"spring.cloud.gcp.spanner.instanceId=testInstance",
				"spring.cloud.gcp.spanner.database=testDatabase",
				"spring.cloud.gcp.config.enabled=false"})
@EnableSpannerRepositories
public class GcpSpannerAutoConfigurationTests {

	@Autowired
	SpannerOperations spannerOperations;

	@Autowired
	TestRepository testRepository;

	@Test
	public void testSpannerOperationsCreated() {
		assertNotNull(this.spannerOperations);
	}

	@Test
	public void testTestRepositoryCreated() {
		assertNotNull(this.testRepository);
	}

	private static class SpannerApplication {

		@Bean
		public static CredentialsProvider googleCredentials() {
			return () -> mock(Credentials.class);
		}

		@Bean
		public static GcpProjectIdProvider googleProjectIdProvier() {
			return () -> "project123";
		}
	}
}
