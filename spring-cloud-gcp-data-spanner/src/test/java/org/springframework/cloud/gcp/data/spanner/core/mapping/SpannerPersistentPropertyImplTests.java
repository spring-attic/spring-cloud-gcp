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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerPersistentPropertyImplTests {

	@Test
	public void testGetColumn() {
		assertThat(
				new SpannerMappingContext().getPersistentEntity(TestEntity.class)
						.columns(),
				containsInAnyOrder("id", "custom_col", "other", "doubleList"));
	}

	@Test(expected = MappingException.class)
	public void testNullColumnName() {
		SpannerMappingContext context = new SpannerMappingContext();
		FieldNamingStrategy namingStrat = mock(FieldNamingStrategy.class);
		when(namingStrat.getFieldName(any())).thenReturn(null);
		context.setFieldNamingStrategy(namingStrat);
		SpannerPersistentEntity entity = context.getPersistentEntity(TestEntity.class);

		for (Object col : entity.columns()) {
			SpannerPersistentPropertyImpl prop = (SpannerPersistentPropertyImpl) entity
					.getPersistentProperty((String) col);

			// Getting the column name will throw an exception because of the mock naming
			// strategy.
			prop.getColumnName();
		}
	}

	@Test
	public void testAssociations() {
		new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.doWithProperties((PropertyHandler<SpannerPersistentProperty>) prop -> {
			assertSame(prop, ((SpannerPersistentPropertyImpl) prop).createAssociation()
					.getInverse());
			assertNull(((SpannerPersistentPropertyImpl) prop).createAssociation()
					.getObverse());
		});
	}

	@Test
	public void testColumnInnerType() {
		assertEquals(Double.class,
				new SpannerMappingContext().getPersistentEntity(TestEntity.class)
						.getPersistentProperty("doubleList")
				.getColumnInnerType());
	}

	@Test
	public void testNoPojoIdProperties() {
		new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.doWithProperties(
						(PropertyHandler<SpannerPersistentProperty>) prop -> assertFalse(
								prop.isIdProperty()));
	}

	@Test
	public void testIgnoredProperty() {
		new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.doWithProperties(
						(PropertyHandler<SpannerPersistentProperty>) prop -> assertNotEquals(
								"not_mapped", prop.getColumnName()));
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@PrimaryKey(keyOrder = 1)
		String id;

		@Column(name = "custom_col")
		String something;

		@Column(name = "")
		String other;

		List<Double> doubleList;

		@NotMapped
		@Column(name = "not_mapped")
		String notMappedString;
	}
}
