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

import com.google.cloud.spanner.Key;
import org.junit.Before;
import org.junit.Test;

import org.springframework.data.util.ClassTypeInformation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * @author Chengyuan Zhao
 */
public class SpannerKeyPropertyTests {

	private SpannerCompositeKeyProperty spannerKeyProperty;

	private SpannerPersistentEntity spannerPersistentEntity;

	@Before
	public void setup() {
		this.spannerPersistentEntity = mock(SpannerPersistentEntity.class);
		this.spannerKeyProperty = new SpannerCompositeKeyProperty(
				this.spannerPersistentEntity, new SpannerPersistentProperty[] {});
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullSpannerPersistentEntityTest() {
		new SpannerCompositeKeyProperty(null, new SpannerPersistentProperty[] {});
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullPropertiesTest() {
		new SpannerCompositeKeyProperty(this.spannerPersistentEntity, null);
	}

	@Test
	public void getColumnNameTest() {
		assertNull(this.spannerKeyProperty.getColumnName());
	}

	@Test
	public void getColumnInnerTypeTest() {
		assertNull(this.spannerKeyProperty.getColumnInnerType());
	}

	@Test
	public void getPrimaryKeyOrderTest() {
		assertNull(this.spannerKeyProperty.getPrimaryKeyOrder());
	}

	@Test
	public void getOwnerTest() {
		assertSame(this.spannerPersistentEntity, this.spannerKeyProperty.getOwner());
	}

	@Test
	public void getNameTest() {
		assertNull(this.spannerKeyProperty.getName());
	}

	@Test
	public void getTypeTest() {
		assertEquals(Key.class, this.spannerKeyProperty.getType());
	}

	@Test
	public void getTypeInformationTest() {
		assertEquals(ClassTypeInformation.from(Key.class),
				this.spannerKeyProperty.getTypeInformation());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getPersistentEntityTypeTest() {
		assertFalse(
				this.spannerKeyProperty.getPersistentEntityTypes().iterator().hasNext());
	}

	@Test
	public void getGetterTest() {
		assertNull(this.spannerKeyProperty.getGetter());
	}

	@Test
	public void getSetterTest() {
		assertNull(this.spannerKeyProperty.getSetter());
	}

	@Test
	public void getFieldTest() {
		assertNull(this.spannerKeyProperty.getField());
	}

	@Test
	public void getSpelExpressionTest() {
		assertNull(this.spannerKeyProperty.getSpelExpression());
	}

	@Test
	public void getAssociationTest() {
		assertNull(this.spannerKeyProperty.getAssociation());
	}

	@Test
	public void isEntityTest() {
		assertFalse(this.spannerKeyProperty.isEntity());
	}

	@Test
	public void isIdPropertyTest() {
		assertTrue(this.spannerKeyProperty.isIdProperty());
	}

	@Test
	public void isVersionPropertyTest() {
		assertFalse(this.spannerKeyProperty.isVersionProperty());
	}

	@Test
	public void isCollectionLikeTest() {
		assertFalse(this.spannerKeyProperty.isCollectionLike());
	}

	@Test
	public void isMapTest() {
		assertFalse(this.spannerKeyProperty.isMap());
	}

	@Test
	public void isArrayTest() {
		assertFalse(this.spannerKeyProperty.isArray());
	}

	@Test
	public void isTransientTest() {
		assertFalse(this.spannerKeyProperty.isTransient());
	}

	@Test
	public void isWritableTest() {
		assertFalse(this.spannerKeyProperty.isWritable());
	}

	@Test
	public void isAssociationTest() {
		assertFalse(this.spannerKeyProperty.isAssociation());
	}

	@Test
	public void getComponentTypeTest() {
		assertNull(this.spannerKeyProperty.getComponentType());
	}

	@Test
	public void getRawTypeTest() {
		assertEquals(Key.class, this.spannerKeyProperty.getRawType());
	}

	@Test
	public void getMapValueTypeTest() {
		assertNull(this.spannerKeyProperty.getMapValueType());
	}

	@Test
	public void getActualTypeTest() {
		assertEquals(Key.class, this.spannerKeyProperty.getActualType());
	}

	@Test
	public void findAnnotationTest() {
		assertNull(this.spannerKeyProperty.findAnnotation(null));
	}

	@Test
	public void findPropertyOrOwnerAnnotationTest() {
		assertNull(this.spannerKeyProperty.findPropertyOrOwnerAnnotation(null));
	}

	@Test
	public void isAnnotationPresentTest() {
		assertFalse(this.spannerKeyProperty.isAnnotationPresent(null));
	}

	@Test
	public void usePropertyAccessTest() {
		assertFalse(this.spannerKeyProperty.usePropertyAccess());
	}
}
