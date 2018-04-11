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

import io.grpc.Attributes.Key;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.expression.spel.SpelEvaluationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
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

	@Test(expected = SpannerDataException.class)
	public void testDuplicatePrimaryKeyOrder() {
		new SpannerMappingContext()
				.getPersistentEntity(EntityWithDuplicatePrimaryKeyOrder.class);
	}

	@Test(expected = SpannerDataException.class)
	public void testInvalidPrimaryKeyOrder() {
		new SpannerMappingContext()
				.getPersistentEntity(EntityWithWronglyOrderedKeys.class).getIdProperty();
	}

	@Test(expected = SpannerDataException.class)
	public void testNoIdEntity() {
		new SpannerMappingContext().getPersistentEntity(EntityWithNoId.class)
				.getIdProperty();
	}

	@Test
	public void testGetIdProperty() {
		assertTrue(new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.getIdProperty() instanceof SpannerCompositeKeyProperty);
	}

	@Test
	public void testHasIdProperty() {
		assertTrue(new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.hasIdProperty());
	}


	@Test(expected = SpannerDataException.class)
	public void testSetIdProperty() {
		SpannerPersistentEntityImpl<TestEntity> entity =
				(SpannerPersistentEntityImpl<TestEntity>) new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class);

		SpannerPersistentProperty idProperty = entity.getIdProperty();

		TestEntity t = new TestEntity();
		entity.getPropertyAccessor(t).setProperty(idProperty, Key.of("blah"));
	}


	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@Column(name = "custom_col")
		String something;
	}

	private static class EntityNoCustomName {
		@PrimaryKey(keyOrder = 1)
		String id;

		String something;
	}

	@Table
	private static class EntityEmptyCustomName {
		@PrimaryKey(keyOrder = 1)
		String id;

		String something;
	}

	@Table(name = "#{'table_'.concat(tablePostfix)}")
	private static class EntityWithExpression {
		@PrimaryKey(keyOrder = 1)
		String id;

		String something;
	}

	private static class EntityWithDuplicatePrimaryKeyOrder {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 1)
		String id2;
	}

	private static class EntityWithWronglyOrderedKeys {
		@PrimaryKey(keyOrder = 1)
		String id;

		@PrimaryKey(keyOrder = 3)
		String id2;
	}

	private static class EntityWithNoId {
		String id;
	}
}
