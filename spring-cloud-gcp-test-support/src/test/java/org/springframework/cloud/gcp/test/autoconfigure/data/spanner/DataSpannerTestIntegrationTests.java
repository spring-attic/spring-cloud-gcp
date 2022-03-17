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

package org.springframework.cloud.gcp.test.autoconfigure.data.spanner;

import com.google.api.gax.core.CredentialsProvider;
import com.google.auth.Credentials;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(SpringRunner.class)
@DataSpannerTest(properties = {"spring.cloud.gcp.spanner.instance-id=test-instance",
		"spring.cloud.gcp.spanner.database=test-database", "spring.cloud.gcp.spanner.emulator.enabled=true"})
public class DataSpannerTestIntegrationTests {

	@Autowired
	private ExampleRepository exampleRepository;

	@Test
	public void test() {
		ExampleEntity exampleEntity = new ExampleEntity(1L, "Java");
		this.exampleRepository.save(exampleEntity);

		Iterable<ExampleEntity> entities = this.exampleRepository.findAll();
		assertThat(entities).hasSize(1);
	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		public CredentialsProvider credentialsProvider() {
			return () -> mock(Credentials.class);
		}
	}
}
