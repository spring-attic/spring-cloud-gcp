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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the Spanner persistent property.
 *
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class SpannerPersistentPropertyImplTests {

	/**
	 * Checks the exceptions' messages and types.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();

	@Test
	public void testGetColumn() {
		assertThat(
				new SpannerMappingContext().getPersistentEntity(TestEntity.class)
						.columns())
								.containsExactlyInAnyOrder("id", "custom_col", "other", "doubleList");
	}

	@Test
	public void testNullColumnName() {
		this.expectedEx.expect(MappingException.class);
		// The expectMessage calls below operate as `contains` and seperate calls are used
		// because the printed order of some components can change randomly.
		this.expectedEx.expectMessage("Invalid (null or empty) field name returned for " +
				"property @org.springframework.cloud.gcp.data.spanner.core.mapping.PrimaryKey");
		this.expectedEx.expectMessage("keyOrder=1");
		this.expectedEx.expectMessage("value=1");
		this.expectedEx.expectMessage(
				"java.lang.String org.springframework.cloud.gcp.data.spanner.core.mapping." +
				"SpannerPersistentPropertyImplTests$TestEntity.id by class " +
				"org.springframework.data.mapping.model.FieldNamingStrategy$MockitoMock$");
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
				.doWithProperties((PropertyHandler<SpannerPersistentProperty>) (prop) -> {
					assertThat(((SpannerPersistentPropertyImpl) prop).createAssociation().getInverse()).isSameAs(prop);
					assertThat(((SpannerPersistentPropertyImpl) prop).createAssociation()
							.getObverse()).isNull();
		});
	}

	@Test
	public void testColumnInnerType() {
		assertThat(new SpannerMappingContext().getPersistentEntity(TestEntity.class).getPersistentProperty("doubleList")
				.getColumnInnerType()).isEqualTo(Double.class);
	}

	@Test
	public void testNoPojoIdProperties() {
		new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.doWithProperties(
						(PropertyHandler<SpannerPersistentProperty>) (prop) -> assertThat(prop.isIdProperty()).isFalse());
	}

	@Test
	public void testIgnoredProperty() {
		new SpannerMappingContext().getPersistentEntity(TestEntity.class)
				.doWithProperties(
						(PropertyHandler<SpannerPersistentProperty>) (prop) -> assertThat(prop.getColumnName())
								.isNotEqualTo("not_mapped"));
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
