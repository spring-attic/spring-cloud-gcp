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

package org.springframework.cloud.gcp.data.datastore.core.mapping;

import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.PropertyHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for the Datastore persistent property.
 *
 * @author Chengyuan Zhao
 */
public class DatastorePersistentPropertyImplTests {

	private final DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();

	/**
	 * used to check exception messages and types.
	 */
	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void propertiesTest() {
		this.datastoreMappingContext.getPersistentEntity(TestEntity.class)
				.doWithProperties(
						(PropertyHandler<DatastorePersistentProperty>) (property) -> {
							if (property.isIdProperty()) {
								assertThat(property.getFieldName()).isEqualTo("id");
							}
							else if (property.getFieldName().equals("custom_field")) {
								assertThat(property.getType()).isEqualTo(String.class);
							}
							else if (property.getFieldName().equals("other")) {
								assertThat(property.getType()).isEqualTo(String.class);
							}
							else if (property.getFieldName().equals("doubleList")) {
								assertThat(property.getComponentType()).isEqualTo(Double.class);
								assertThat(property.isCollectionLike()).isTrue();
							}
							else if (property.getFieldName().equals("embeddedEntity")) {
								assertThat(property.getEmbeddedType()).isEqualTo(EmbeddedType.EMBEDDED_ENTITY);
							}
							else if (property.getFieldName().equals("embeddedMap")) {
								assertThat(property.getEmbeddedType()).isEqualTo(EmbeddedType.EMBEDDED_MAP);
								assertThat(property.getTypeInformation().getTypeArguments().get(1).getType())
										.isEqualTo(String.class);
							}
							else if (property.getFieldName().equals("linkedEntity")) {
								assertThat(property.isDescendants()).isTrue();
							}
							else if (property.getFieldName().equals("linkedEntityRef")) {
								assertThat(property.isReference()).isTrue();
							}
							else {
								fail("All properties of the test entity are expected to match a checked"
										+ " case above, but this did not: " + property);
							}
						});
	}

	@Test
	public void testAssociations() {
		this.datastoreMappingContext.getPersistentEntity(TestEntity.class)
				.doWithProperties((PropertyHandler<DatastorePersistentProperty>) (prop) -> {
					assertThat(prop).isSameAs(
							((DatastorePersistentPropertyImpl) prop).createAssociation().getInverse());
					assertThat(((DatastorePersistentPropertyImpl) prop).createAssociation().getObverse()).isNull();
				});
	}

	@Test
	public void referenceDescendantAnnotatedTest() {
		this.expectedException.expect(DatastoreDataException.class);
		this.expectedException.expectMessage("Property cannot be annotated both @Descendants and " +
				"@Reference: subEntity");
		this.datastoreMappingContext
				.getPersistentEntity(DescendantReferenceAnnotatedEntity.class);
	}

	@Test
	public void fieldDescendantAnnotatedTest() {
		this.expectedException.expect(DatastoreDataException.class);
		this.expectedException.expectMessage("Property cannot be annotated as @Field if it is " +
				"annotated @Descendants or @Reference: name");
		this.datastoreMappingContext
				.getPersistentEntity(DescendantFieldAnnotatedEntity.class);
	}

	@Test
	public void fieldReferenceAnnotatedTest() {
		this.expectedException.expect(DatastoreDataException.class);
		this.expectedException.expectMessage("Property cannot be annotated as @Field if it is " +
				"annotated @Descendants or @Reference: name");
		this.datastoreMappingContext
				.getPersistentEntity(FieldReferenceAnnotatedEntity.class);
	}

	@Entity(name = "custom_test_kind")
	private static class TestEntity {
		@Id
		String id;

		@Field(name = "custom_field")
		String something;

		@Field(name = "")
		String other;

		List<Double> doubleList;

		@Transient
		@Field(name = "not_mapped")
		String notMappedString;

		TestSubEntity embeddedEntity;

		Map<String, String> embeddedMap;

		@Descendants
		List<TestSubEntity> linkedEntity;

		@Reference
		TestSubEntity linkedEntityRef;
	}

	@Entity
	private static class TestSubEntity {

	}

	private static class UntypedListEntity {
		List untypedList;
	}

	private static class DescendantReferenceAnnotatedEntity {
		@Descendants
		@Reference
		TestSubEntity[] subEntity;
	}

	private static class FieldReferenceAnnotatedEntity {
		@Field(name = "name")
		@Reference
		TestSubEntity[] subEntity;
	}

	private static class DescendantFieldAnnotatedEntity {
		@Descendants
		@Field(name = "name")
		TestSubEntity[] subEntity;
	}
}
