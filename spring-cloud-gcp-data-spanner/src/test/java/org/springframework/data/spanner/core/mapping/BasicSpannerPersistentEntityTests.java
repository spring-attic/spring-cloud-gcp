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

package org.springframework.data.spanner.core.mapping;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.util.ClassTypeInformation;

/**
 * @author Chengyuan Zhao
 */
public class BasicSpannerPersistentEntityTests {

	@Test
	public void testTableName() {
		BasicSpannerPersistentEntity<TestEntity> entity = new BasicSpannerPersistentEntity<>(
				ClassTypeInformation.from(TestEntity.class));

		MatcherAssert.assertThat(entity.tableName(), Matchers.is("custom_test_table"));
	}

	@Test
	public void testRawTableName() {
		BasicSpannerPersistentEntity<EntityNoCustomName> entity = new BasicSpannerPersistentEntity<>(
				ClassTypeInformation.from(EntityNoCustomName.class));

		MatcherAssert.assertThat(entity.tableName(), Matchers.is("entityNoCustomName"));
	}

	@Test
	public void testEmptyCustomTableName() {
		BasicSpannerPersistentEntity<EntityEmptyCustomName> entity = new BasicSpannerPersistentEntity<>(
				ClassTypeInformation.from(EntityEmptyCustomName.class));

		MatcherAssert.assertThat(entity.tableName(),
				Matchers.is("entityEmptyCustomName"));
	}

	@Test
	public void testColumns() {
		BasicSpannerPersistentEntity<TestEntity> entity =
				(BasicSpannerPersistentEntity<TestEntity>) (new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class));

		MatcherAssert.assertThat(entity.columns(),
				Matchers.containsInAnyOrder("custom_col", "id"));
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@Column(name = "custom_col")
		String something;
	}

	private static class EntityNoCustomName {
		@Id
		String id;

		String something;
	}

	@Table(name = "")
	private static class EntityEmptyCustomName {
		@Id
		String id;

		String something;
	}
}
