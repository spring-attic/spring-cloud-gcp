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

package org.springframework.cloud.gcp.data.spanner.core.mapping;

import java.util.List;

import com.google.cloud.spanner.Key;
import com.google.spanner.v1.TypeCode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ApplicationContext;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.util.ClassTypeInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the Spanner persistent entity.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 */
public class SpannerPersistentEntityImplTests {

	/**
	 * tests the messages and types of exceptions.
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final SpannerMappingContext spannerMappingContext = new SpannerMappingContext();

	@Test
	public void testTableName() {
		SpannerPersistentEntityImpl<TestEntity> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(TestEntity.class));

		assertThat(entity.tableName()).isEqualTo("custom_test_table");
	}

	@Test
	public void testRawTableName() {
		SpannerPersistentEntityImpl<EntityNoCustomName> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityNoCustomName.class));

		assertThat(entity.tableName()).isEqualTo("entityNoCustomName");
	}

	@Test
	public void testEmptyCustomTableName() {
		SpannerPersistentEntityImpl<EntityEmptyCustomName> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityEmptyCustomName.class));

		assertThat(entity.tableName()).isEqualTo("entityEmptyCustomName");
	}

	@Test
	public void testColumns() {
		assertThat(new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.columns()).containsExactlyInAnyOrder("id", "custom_col");
	}

	@Test
	public void testExpressionResolutionWithoutApplicationContext() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage("Error getting table name for EntityWithExpression; " +
				"nested exception is org.springframework.expression.spel.SpelEvaluationException: " +
				"EL1007E: Property or field 'tablePostfix' cannot be found on null");
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
		assertThat(entity.tableName()).isEqualTo("table_something");
	}

	@Test
	public void testDuplicatePrimaryKeyOrder() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"Two properties were annotated with the same primary key order: " +
				"id2 and id in EntityWithDuplicatePrimaryKeyOrder.");

		new SpannerMappingContext()
				.getPersistentEntity(EntityWithDuplicatePrimaryKeyOrder.class);
	}

	@Test
	public void testInvalidPrimaryKeyOrder() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"The primary key columns were not given a consecutive order. " +
						"There is no property annotated with order 2 in EntityWithWronglyOrderedKeys.");

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
		assertThat(new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.getIdProperty()).isInstanceOf(SpannerCompositeKeyProperty.class);
	}

	@Test
	public void testHasIdProperty() {
		assertThat(new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.hasIdProperty()).isTrue();
	}

	@Test
	public void testSetIdProperty() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"Setting the primary key directly via the Key ID property is not supported. " +
						"Please set the underlying column properties.");

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
		p.doWithProperties(
				(SimplePropertyHandler) (property) -> assertThat(accessor.getProperty(property)).isNotEqualTo("b"));
	}

	@Test
	public void testInvalidTableName() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"Error getting table name for EntityBadName; nested exception is " +
						"org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException: Only " +
						"letters, numbers, and underscores are allowed in table names: ;DROP TABLE your_table;");

		SpannerPersistentEntityImpl<EntityBadName> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityBadName.class));
		entity.tableName();
	}

	@Test
	public void testSpELInvalidName() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"Error getting table name for EntityWithExpression; nested exception is " +
						"org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException: " +
						"Only letters, numbers, and underscores are allowed in table names: " +
						"table_; DROP TABLE your_table;");

		SpannerPersistentEntityImpl<EntityWithExpression> entity = new SpannerPersistentEntityImpl<>(
				ClassTypeInformation.from(EntityWithExpression.class));

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		when(applicationContext.getBean("tablePostfix"))
				.thenReturn("; DROP TABLE your_table;");
		when(applicationContext.containsBean("tablePostfix")).thenReturn(true);

		entity.setApplicationContext(applicationContext);
		entity.tableName();
	}

	@Test
	public void testDuplicateEmbeddedColumnName() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"Two properties resolve to the same column name: " +
						"other in EmbeddedParentDuplicateColumn");

		this.spannerMappingContext
				.getPersistentEntity(EmbeddedParentDuplicateColumn.class);
	}

	@Test
	public void testEmbeddedParentKeys() {
		GrandParentEmbedded grandParentEmbedded = new GrandParentEmbedded();
		grandParentEmbedded.id = "1";

		ParentEmbedded parentEmbedded = new ParentEmbedded();
		parentEmbedded.grandParentEmbedded = grandParentEmbedded;
		parentEmbedded.id2 = 2;
		parentEmbedded.id3 = 3L;

		ChildEmbedded childEmbedded = new ChildEmbedded();
		childEmbedded.parentEmbedded = parentEmbedded;
		childEmbedded.id4 = "4";

		// intentionally null, which is a supported key component type.
		childEmbedded.id5 = null;

		Key key = (Key) this.spannerMappingContext
				.getPersistentEntity(ChildEmbedded.class)
				.getIdentifierAccessor(childEmbedded).getIdentifier();

		assertThat(key).isEqualTo(
				Key.newBuilder()
						.append("1")
						.append("2")
						.append("3")
						.append("4")
						.appendObject(null)
						.build());
	}

	@Test
	public void testEmbeddedCollection() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"Embedded properties cannot be collections:");

		this.thrown.expectMessage(
				"org.springframework.cloud.gcp.data.spanner.core.mapping." +
						"SpannerPersistentEntityImplTests$ChildCollectionEmbedded.parentEmbedded");

		this.spannerMappingContext.getPersistentEntity(ChildCollectionEmbedded.class);
	}

	@Test
	public void testExcludeEmbeddedColumnNames() {
		assertThat(this.spannerMappingContext.getPersistentEntity(ChildEmbedded.class)
				.columns()).containsExactlyInAnyOrder("id", "id2", "id3", "id4", "id5");
	}

	@Test
	public void doWithChildrenCollectionsTest() {
		PropertyHandler<SpannerPersistentProperty> mockHandler = mock(PropertyHandler.class);
		SpannerPersistentEntity spannerPersistentEntity =
				this.spannerMappingContext.getPersistentEntity(ParentInRelationship.class);
		doAnswer((invocation) -> {
			String colName = ((SpannerPersistentProperty) invocation.getArgument(0))
					.getName();
			assertThat(colName.equals("childrenA") || colName.equals("childrenB")).isTrue();
			return null;
		}).when(mockHandler).doWithPersistentProperty(any());
		spannerPersistentEntity.doWithInterleavedProperties(mockHandler);
		verify(mockHandler, times(2)).doWithPersistentProperty(any());
	}

	@Test
	public void testParentChildPkNamesMismatch() {
		this.thrown.expect(SpannerDataException.class);
		this.thrown.expectMessage(
				"The child primary key column (ChildBInRelationship.id) at position 1 does not match that " +
						"of its parent (ParentInRelationshipMismatchedKeyName.idNameDifferentThanChildren).");

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

	private static class EmbeddedKeyComponents {
		@PrimaryKey
		String id;

		@PrimaryKey(keyOrder = 2)
		String id2;
	}

	private static class ChildBInRelationship {
		@Embedded
		@PrimaryKey
		EmbeddedKeyComponents embeddedKeyComponents;
	}

	private static class ParentInRelationshipMismatchedKeyName {
		@PrimaryKey
		String idNameDifferentThanChildren;

		@Interleaved
		List<ChildBInRelationship> childrenA;
	}

	private static class GrandParentEmbedded {
		@PrimaryKey
		String id;
	}

	private static class ParentEmbedded {
		@PrimaryKey
		@Embedded
		GrandParentEmbedded grandParentEmbedded;

		// This property requires conversion to be stored as a STRING column.
		@PrimaryKey(keyOrder = 2)
		@Column(name = "id2", spannerType = TypeCode.STRING)
		int id2;

		// This property will be stored as a STRING column even though Long is a natively supported type.
		@PrimaryKey(keyOrder = 3)
		@Column(name = "id3", spannerType = TypeCode.STRING)
		Long id3;
	}

	private static class ChildEmbedded {
		@PrimaryKey
		@Embedded
		ParentEmbedded parentEmbedded;

		@PrimaryKey(keyOrder = 2)
		String id4;

		@PrimaryKey(keyOrder = 3)
		@Column(spannerType = TypeCode.STRING)
		Long id5;
	}

	private static class ChildCollectionEmbedded {
		@PrimaryKey
		@Embedded
		List<ParentEmbedded> parentEmbedded;

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
