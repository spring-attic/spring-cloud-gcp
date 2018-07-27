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

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.expression.spel.SpelEvaluationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class DatastorePersistentEntityImplTests {

	@Test
	public void testTableName() {
		DatastorePersistentEntityImpl<TestEntity> entity = new DatastorePersistentEntityImpl<>(
				ClassTypeInformation.from(TestEntity.class));

		assertThat(entity.kindName(), is("custom_test_kind"));
	}

	@Test
	public void testRawTableName() {
		DatastorePersistentEntityImpl<EntityNoCustomName> entity = new DatastorePersistentEntityImpl<>(
				ClassTypeInformation.from(EntityNoCustomName.class));

		assertThat(entity.kindName(), is("entityNoCustomName"));
	}

	@Test
	public void testEmptyCustomTableName() {
		DatastorePersistentEntityImpl<EntityEmptyCustomName> entity = new DatastorePersistentEntityImpl<>(
				ClassTypeInformation.from(EntityEmptyCustomName.class));

		assertThat(entity.kindName(), is("entityEmptyCustomName"));
	}

	@Test(expected = SpelEvaluationException.class)
	public void testExpressionResolutionWithoutApplicationContext() {
		DatastorePersistentEntityImpl<EntityWithExpression> entity = new DatastorePersistentEntityImpl<>(
				ClassTypeInformation.from(EntityWithExpression.class));

		entity.kindName();
	}

	@Test
	public void testExpressionResolutionFromApplicationContext() {
		DatastorePersistentEntityImpl<EntityWithExpression> entity = new DatastorePersistentEntityImpl<>(
				ClassTypeInformation.from(EntityWithExpression.class));

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean("kindPostfix")).thenReturn("something");
		when(applicationContext.containsBean("kindPostfix")).thenReturn(true);

		entity.setApplicationContext(applicationContext);
		assertThat(entity.kindName(), is("kind_something"));
	}

	@Test
	public void testHasIdProperty() {
		assertTrue(new DatastoreMappingContext().getPersistentEntity(TestEntity.class)
				.hasIdProperty());
	}

	@Test
	public void testHasNoIdProperty() {
		assertFalse(new DatastoreMappingContext()
				.getPersistentEntity(EntityWithNoId.class).hasIdProperty());
	}

	@Test(expected = DatastoreDataException.class)
	public void testGetIdPropertyOrFail() {
		new DatastoreMappingContext().getPersistentEntity(EntityWithNoId.class)
				.getIdPropertyOrFail();
	}

	@Test
	public void testIgnoredProperty() {
		TestEntity t = new TestEntity();
		t.id = "a";
		t.something = "a";
		t.notMapped = "b";
		DatastorePersistentEntity p = new DatastoreMappingContext()
				.getPersistentEntity(TestEntity.class);
		PersistentPropertyAccessor accessor = p.getPropertyAccessor(t);
		p.doWithProperties((SimplePropertyHandler) property -> assertNotEquals("b",
				accessor.getProperty(property)));
	}

	@Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;

		@Field(name = "custom_col")
		String something;

		@Transient
		String notMapped;
	}

	private static class EntityNoCustomName {
		@Id
		String id;

		String something;
	}

	@Entity
	private static class EntityEmptyCustomName {
		@Id
		String id;

		String something;
	}

	@Entity(name = "#{'kind_'.concat(kindPostfix)}")
	private static class EntityWithExpression {
		@Id
		String id;

		String something;
	}

	private static class EntityWithNoId {
		String id;
	}
}
