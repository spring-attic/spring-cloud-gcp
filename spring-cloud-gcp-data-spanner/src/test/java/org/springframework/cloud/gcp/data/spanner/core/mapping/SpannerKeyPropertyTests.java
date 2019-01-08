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

import java.lang.annotation.Annotation;

import com.google.cloud.spanner.Key;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.data.util.ClassTypeInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for the Spanner custom key property.
 *
 * @author Chengyuan Zhao
 */
public class SpannerKeyPropertyTests {

	/**
	 * checks the messages and types of exceptions.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	private SpannerCompositeKeyProperty spannerKeyProperty;

	private SpannerPersistentEntity spannerPersistentEntity;

	@Before
	public void setup() {
		this.spannerPersistentEntity = mock(SpannerPersistentEntity.class);
		this.spannerKeyProperty = new SpannerCompositeKeyProperty(
				this.spannerPersistentEntity, new SpannerPersistentProperty[] {});
	}

	@Test
	public void nullSpannerPersistentEntityTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A valid Cloud Spanner persistent entity is required.");
		new SpannerCompositeKeyProperty(null, new SpannerPersistentProperty[] {});
	}

	@Test
	public void nullPropertiesTest() {
		this.expectedEx.expect(IllegalArgumentException.class);
		this.expectedEx.expectMessage("A valid array of primary key properties is required.");
		new SpannerCompositeKeyProperty(this.spannerPersistentEntity, null);
	}

	@Test
	public void getColumnNameTest() {
		assertThat(this.spannerKeyProperty.getColumnName()).isNull();
	}

	@Test
	public void getColumnInnerTypeTest() {
		assertThat(this.spannerKeyProperty.getColumnInnerType()).isNull();
	}

	@Test
	public void getPrimaryKeyOrderTest() {
		assertThat(this.spannerKeyProperty.getPrimaryKeyOrder()).isNull();
	}

	@Test
	public void getOwnerTest() {
		assertThat(this.spannerKeyProperty.getOwner()).isSameAs(this.spannerPersistentEntity);
	}

	@Test
	public void getNameTest() {
		assertThat(this.spannerKeyProperty.getName()).isNull();
	}

	@Test
	public void getTypeTest() {
		assertThat(this.spannerKeyProperty.getType()).isEqualTo(Key.class);
	}

	@Test
	public void getTypeInformationTest() {
		assertThat(this.spannerKeyProperty.getTypeInformation())
				.isEqualTo(ClassTypeInformation.from(Key.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void getPersistentEntityTypeTest() {
		assertThat(
				this.spannerKeyProperty.getPersistentEntityTypes().iterator().hasNext()).isFalse();
	}

	@Test
	public void getGetterTest() {
		assertThat(this.spannerKeyProperty.getGetter()).isNull();
	}

	@Test
	public void getSetterTest() {
		assertThat(this.spannerKeyProperty.getSetter()).isNull();
	}

	@Test
	public void getFieldTest() {
		assertThat(this.spannerKeyProperty.getField()).isNull();
	}

	@Test
	public void getSpelExpressionTest() {
		assertThat(this.spannerKeyProperty.getSpelExpression()).isNull();
	}

	@Test
	public void getAssociationTest() {
		assertThat(this.spannerKeyProperty.getAssociation()).isNull();
	}

	@Test
	public void isEntityTest() {
		assertThat(this.spannerKeyProperty.isEntity()).isFalse();
	}

	@Test
	public void isIdPropertyTest() {
		assertThat(this.spannerKeyProperty.isIdProperty()).isTrue();
	}

	@Test
	public void isVersionPropertyTest() {
		assertThat(this.spannerKeyProperty.isVersionProperty()).isFalse();
	}

	@Test
	public void isCollectionLikeTest() {
		assertThat(this.spannerKeyProperty.isCollectionLike()).isFalse();
	}

	@Test
	public void isMapTest() {
		assertThat(this.spannerKeyProperty.isMap()).isFalse();
	}

	@Test
	public void isArrayTest() {
		assertThat(this.spannerKeyProperty.isArray()).isFalse();
	}

	@Test
	public void isTransientTest() {
		assertThat(this.spannerKeyProperty.isTransient()).isFalse();
	}

	@Test
	public void isWritableTest() {
		assertThat(this.spannerKeyProperty.isWritable()).isFalse();
	}

	@Test
	public void isAssociationTest() {
		assertThat(this.spannerKeyProperty.isAssociation()).isFalse();
	}

	@Test
	public void getComponentTypeTest() {
		assertThat(this.spannerKeyProperty.getComponentType()).isNull();
	}

	@Test
	public void getRawTypeTest() {
		assertThat(this.spannerKeyProperty.getRawType()).isEqualTo(Key.class);
	}

	@Test
	public void getMapValueTypeTest() {
		assertThat(this.spannerKeyProperty.getMapValueType()).isNull();
	}

	@Test
	public void getActualTypeTest() {
		assertThat(this.spannerKeyProperty.getActualType()).isEqualTo(Key.class);
	}

	@Test
	public void findAnnotationTest() {
		Annotation annotation = this.spannerKeyProperty.findAnnotation(null);
		assertThat(annotation).isNull();
	}

	@Test
	public void findPropertyOrOwnerAnnotationTest() {
		Annotation annotation = this.spannerKeyProperty.findPropertyOrOwnerAnnotation(null);
		assertThat(annotation).isNull();
	}

	@Test
	public void isAnnotationPresentTest() {
		assertThat(this.spannerKeyProperty.isAnnotationPresent(null)).isFalse();
	}

	@Test
	public void usePropertyAccessTest() {
		assertThat(this.spannerKeyProperty.usePropertyAccess()).isFalse();
	}
}
