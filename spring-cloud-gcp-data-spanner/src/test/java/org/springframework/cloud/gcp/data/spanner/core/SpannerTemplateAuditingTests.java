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

package org.springframework.cloud.gcp.data.spanner.core;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.Mutation;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gcp.data.spanner.core.admin.SpannerSchemaUtils;
import org.springframework.cloud.gcp.data.spanner.core.convert.SpannerEntityProcessor;
import org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.Table;
import org.springframework.cloud.gcp.data.spanner.repository.config.EnableSpannerAuditing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.AuditorAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the auditing features of the template.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class SpannerTemplateAuditingTests {

	private static final List<Mutation> UPSERT_MUTATION = Arrays
			.asList(Mutation.newInsertOrUpdateBuilder("custom_test_table").build());

	private static final LocalDateTime LONG_AGO = LocalDate.parse("2000-01-01").atStartOfDay();

	@Autowired
	SpannerTemplate spannerTemplate;

	@Test
	public void testModifiedNullProperties() {
		TestEntity testEntity = new TestEntity();
		testEntity.id = "a";
		// intentionally leaving the other two audit properties untouched.

		this.spannerTemplate.upsert(testEntity);
	}

	@Test
	public void testModifiedPrevProperties() {
		TestEntity testEntity = new TestEntity();
		testEntity.id = "a";
		testEntity.lastTouched = LONG_AGO;
		testEntity.lastUser = "person";

		this.spannerTemplate.upsert(testEntity);
	}

	/**
	 * Spring config for the tests.
	 */
	@Configuration
	@EnableSpannerAuditing
	static class config {

		@Bean
		public SpannerMappingContext spannerMappingContext() {
			return new SpannerMappingContext();
		}

		@Bean
		public SpannerTemplate spannerTemplate(SpannerMappingContext spannerMappingContext) {
			SpannerEntityProcessor objectMapper = mock(SpannerEntityProcessor.class);
			SpannerMutationFactory mutationFactory = mock(SpannerMutationFactory.class);

			when(mutationFactory.upsert(Mockito.any(TestEntity.class), Mockito.any()))
					.thenAnswer(invocation -> {
						TestEntity testEntity = invocation.getArgument(0);
						assertThat(testEntity.lastTouched).isNotNull();
						assertThat(testEntity.lastTouched).isAfter(LONG_AGO);
						assertThat(testEntity.lastUser).isEqualTo("test_user");
						return UPSERT_MUTATION;
					});

			SpannerSchemaUtils schemaUtils = new SpannerSchemaUtils(spannerMappingContext, objectMapper, true);

			return new SpannerTemplate(() -> mock(DatabaseClient.class),
					spannerMappingContext, objectMapper, mutationFactory, schemaUtils);
		}

		@Bean
		public AuditorAware<String> auditorProvider() {
			return () -> Optional.of("test_user");
		}
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey
		String id;

		@LastModifiedBy
		String lastUser;

		@LastModifiedDate
		LocalDateTime lastTouched;
	}
}
