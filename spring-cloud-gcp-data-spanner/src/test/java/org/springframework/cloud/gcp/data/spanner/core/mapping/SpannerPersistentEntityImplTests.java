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

import java.util.List;

import com.google.cloud.spanner.Key;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.expression.spel.SpelEvaluationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerPersistentEntityImplTests {

	private final SpannerMappingContext spannerMappingContext = new SpannerMappingContext();

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
		assertThat(new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.columns(), containsInAnyOrder("id", "custom_col"));
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

	@Test
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
		SpannerPersistentEntity entity = new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class);

		PersistentProperty idProperty = entity.getIdProperty();

		TestEntity t = new TestEntity();
		entity.getPropertyAccessor(t).setProperty(idProperty, Key.of("blah"));
	}

	@Test
	public void testIgnoredProperty() {
		TestEntity t = new TestEntity();
		t.id = "a";
		t.something = "a";
		t.notMapped = "b";
		SpannerPersistentEntity p = new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class);
		PersistentPropertyAccessor accessor = p.getPropertyAccessor(t);
		p.doWithProperties((SimplePropertyHandler) property -> assertNotEquals("b",
				accessor.getProperty(property)));
	}

	@Test(expected = SpannerDataException.class)
	public void testInvalidTableName() {
		SpannerPersistentEntityImpl<EntityBadName> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityBadName.class));
		entity.tableName();
	}

	@Test(expected = SpannerDataException.class)
	public void testSpELInvalidName() {
		SpannerPersistentEntityImpl<EntityWithExpression> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityWithExpression.class));

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean("tablePostfix"))
				.thenReturn("; DROP TABLE your_table;");
		when(applicationContext.containsBean("tablePostfix")).thenReturn(true);

		entity.setApplicationContext(applicationContext);
		entity.tableName();
	}

	@Test(expected = SpannerDataException.class)
	public void testDuplicateEmbeddedColumnName() {
		this.spannerMappingContext
				.getPersistentEntity(EmbeddedParentDuplicateColumn.class);
	}

	@Test
	public void testEmbeddedParentKeys() {
		GrandParentEmbedded grandParentEmbedded = new GrandParentEmbedded();
		grandParentEmbedded.id = "1";

		ParentEmbedded parentEmbedded = new ParentEmbedded();
		parentEmbedded.grandParentEmbedded = grandParentEmbedded;
		parentEmbedded.id2 = "2";
		parentEmbedded.id3 = "3";

		ChildEmbedded childEmbedded = new ChildEmbedded();
		childEmbedded.parentEmbedded = parentEmbedded;
		childEmbedded.id4 = "4";

		Key key = (Key) this.spannerMappingContext
				.getPersistentEntity(ChildEmbedded.class)
				.getIdentifierAccessor(childEmbedded).getIdentifier();
		assertEquals(
				Key.newBuilder().append("1").append("2").append("3").append("4").build(),
				key);
	}

	@Test
	public void testExcludeEmbeddedColumnNames() {
		assertThat(this.spannerMappingContext.getPersistentEntity(ChildEmbedded.class)
				.columns(), containsInAnyOrder("id", "id2", "id3", "id4"));
	}

	@Test
	public void doWithChildrenCollectionsTest() {
		PropertyHandler<SpannerPersistentProperty> mockHandler = mock(PropertyHandler.class);
		SpannerPersistentEntity spannerPersistentEntity =
				this.spannerMappingContext.getPersistentEntity(ParentInRelationship.class);
		doAnswer(invocation -> {
			String colName = ((SpannerPersistentProperty) invocation.getArgument(0))
					.getName();
			assertTrue(colName.equals("childrenA") || colName.equals("childrenB"));
			return null;
		}).when(mockHandler).doWithPersistentProperty(any());
		spannerPersistentEntity.doWithInterleavedProperties(mockHandler);
		verify(mockHandler, times(2)).doWithPersistentProperty(any());
	}

	@Test(expected = SpannerDataException.class)
	public void testParentChildPkNamesMismatch() {
		this.spannerMappingContext
				.getPersistentEntity(ParentInRelationshipMismatchedKeyName.class);
	}

	private static class ParentInRelationship {
		@PrimaryKey
		String id;

		@Interleaved
		List<ChildAInRelationship> childrenA;

		@Interleaved
		List<ChildBInRelationship> childrenB;
	}

	private static class ChildAInRelationship {
		@PrimaryKey
		String id;

		@PrimaryKey(keyOrder = 2)
		String id2;
	}

	private static class ChildBInRelationship {
		@PrimaryKey
		String id;

		@PrimaryKey(keyOrder = 2)
		String id2;
	}

	private static class ParentInRelationshipMismatchedKeyName {
		@PrimaryKey
		String idNameDifferentThanChildren;

		@Interleaved
		List<ChildAInRelationship> childrenA;
	}

	private static class GrandParentEmbedded {
		@PrimaryKey
		String id;
	}

	private static class ParentEmbedded {
		@PrimaryKey
		@Embedded
		GrandParentEmbedded grandParentEmbedded;

		@PrimaryKey(keyOrder = 2)
		String id2;

		@PrimaryKey(keyOrder = 3)
		String id3;
	}

	private static class ChildEmbedded {
		@PrimaryKey
		@Embedded
		ParentEmbedded parentEmbedded;

		@PrimaryKey(keyOrder = 2)
		String id4;
	}

	private static class EmbeddedParentDuplicateColumn {
		@PrimaryKey
		String id;

		String other;

		@Embedded
		EmbeddedChildDuplicateColumn embeddedChildDuplicateColumn;
	}

	private static class EmbeddedChildDuplicateColumn {
		@Column(name = "other")
		String stuff;
	}

	@Table(name = ";DROP TABLE your_table;")
	private static class EntityBadName {
		@PrimaryKey(keyOrder = 1)
		String id;

		String something;
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@Column(name = "custom_col")
		String something;

		@NotMapped
		String notMapped;
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
