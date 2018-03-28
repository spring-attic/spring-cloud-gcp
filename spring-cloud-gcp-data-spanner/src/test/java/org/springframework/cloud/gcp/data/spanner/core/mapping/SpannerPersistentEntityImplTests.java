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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.expression.spel.SpelEvaluationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerPersistentEntityImplTests {

	@Test
	public void testTableName() {
		SpannerPersistentEntityImpl<TestEntity> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(TestEntity.class));

		assertThat(entity.tableName(), is("custom_test_table"));
	}

	@Test
	public void testRawTableName() {
		SpannerPersistentEntityImpl<EntityNoCustomName> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityNoCustomName.class));

		assertThat(entity.tableName(), is("entityNoCustomName"));
	}

	@Test
	public void testEmptyCustomTableName() {
		SpannerPersistentEntityImpl<EntityEmptyCustomName> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityEmptyCustomName.class));

		assertThat(entity.tableName(), is("entityEmptyCustomName"));
	}

	@Test
	public void testColumns() {
		SpannerPersistentEntityImpl<TestEntity> entity =
				(SpannerPersistentEntityImpl<TestEntity>) (new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class));

		assertThat(entity.columns(), containsInAnyOrder("custom_col", "id"));
	}

	@Test(expected = SpelEvaluationException.class)
	public void testExpressionResolutionWithoutApplicationContext() {
		SpannerPersistentEntityImpl<EntityWithExpression> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityWithExpression.class));

		entity.tableName();
	}

	@Test
	public void testExpressionResolutionFromApplicationContext() {
		SpannerPersistentEntityImpl<EntityWithExpression> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityWithExpression.class));

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean("tablePostfix")).thenReturn("something");
		when(applicationContext.containsBean("tablePostfix")).thenReturn(true);

		entity.setApplicationContext(applicationContext);
		assertThat(entity.tableName(), is("table_something"));
	}

	@SpannerTable(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@SpannerColumn(name = "custom_col")
		String something;
	}

	private static class EntityNoCustomName {
		@Id
		String id;

		String something;
	}

	@SpannerTable(name = "")
	private static class EntityEmptyCustomName {
		@Id
		String id;

		String something;
	}

	@SpannerTable(name = "#{'table_'.concat(tablePostfix)}")
	private static class EntityWithExpression {
		@Id
		String id;

		String something;
	}
}
