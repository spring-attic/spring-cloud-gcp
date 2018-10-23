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

import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.PropertyHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Chengyuan Zhao
 */
public class DatastorePersistentPropertyImplTests {

	private final DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();

	@Test
	public void propertiesTest() {
		this.datastoreMappingContext.getPersistentEntity(TestEntity.class)
				.doWithProperties(
						(PropertyHandler<DatastorePersistentProperty>) property -> {
							if (property.isIdProperty()) {
								assertEquals("id", property.getFieldName());
							}
							else if (property.getFieldName().equals("custom_field")) {
								assertEquals(String.class, property.getType());
							}
							else if (property.getFieldName().equals("other")) {
								assertEquals(String.class, property.getType());
							}
							else if (property.getFieldName().equals("doubleList")) {
								assertEquals(Double.class,
										property.getComponentType());
								assertTrue(property.isCollectionLike());
							}
							else if (property.getFieldName().equals("embeddedEntity")) {
								assertTrue(property
										.getEmbeddedType() == EmbeddedType.EMBEDDED_ENTITY);
							}
							else if (property.getFieldName().equals("embeddedMap")) {
								assertTrue(property
										.getEmbeddedType() == EmbeddedType.EMBEDDED_MAP);
								assertEquals(String.class,
										property.getTypeInformation().getTypeArguments()
												.get(1).getType());
							}
							else if (property.getFieldName().equals("linkedEntity")) {
								assertTrue(property.isDescendants());
							}
							else if (property.getFieldName().equals("linkedEntityRef")) {
								assertTrue(property.isReference());
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
				.doWithProperties((PropertyHandler<DatastorePersistentProperty>) prop -> {
					assertSame(prop, ((DatastorePersistentPropertyImpl) prop)
							.createAssociation().getInverse());
					assertNull(((DatastorePersistentPropertyImpl) prop)
							.createAssociation().getObverse());
				});
	}

	@Test(expected = DatastoreDataException.class)
	public void referenceDescendantAnnotatedTest() {
		this.datastoreMappingContext
				.getPersistentEntity(DescendantReferenceAnnotatedEntity.class);
	}

	@Test(expected = DatastoreDataException.class)
	public void fieldDescendantAnnotatedTest() {
		this.datastoreMappingContext
				.getPersistentEntity(DescendantFieldAnnotatedEntity.class);
	}

	@Test(expected = DatastoreDataException.class)
	public void fieldReferenceAnnotatedTest() {
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
