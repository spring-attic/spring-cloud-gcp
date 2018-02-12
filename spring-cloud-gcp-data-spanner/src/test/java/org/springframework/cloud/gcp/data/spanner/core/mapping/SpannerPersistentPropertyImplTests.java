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

import org.springframework.data.annotation.Id;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
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
		SpannerPersistentEntityImpl<TestEntity> entity =
				(SpannerPersistentEntityImpl<TestEntity>) (new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class));

		assertThat(entity.columns(), containsInAnyOrder("id", "custom_col", "other", "doubleList"));
	}

	@Test(expected = MappingException.class)
	public void testNullColumnName() {
		SpannerMappingContext context = new SpannerMappingContext();
		FieldNamingStrategy namingStrat = mock(FieldNamingStrategy.class);
		when(namingStrat.getFieldName(any())).thenReturn(null);
		context.setFieldNamingStrategy(namingStrat);
		SpannerPersistentEntityImpl<TestEntity> entity =
				(SpannerPersistentEntityImpl<TestEntity>) (context
				.getPersistentEntity(TestEntity.class));

		entity.columns().forEach(col -> {
			SpannerPersistentPropertyImpl prop = (SpannerPersistentPropertyImpl) entity
					.getPersistentPropertyByColumnName(col);

			// Getting the column name will throw an exception because of the mock naming
			// strategy.
			prop.getColumnName();
		});
	}

	@Test
	public void testAssociations() {
		SpannerPersistentEntityImpl<TestEntity> entity =
				(SpannerPersistentEntityImpl<TestEntity>) (new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class));

		entity.columns().forEach(col -> {
			SpannerPersistentPropertyImpl prop = (SpannerPersistentPropertyImpl) entity
					.getPersistentPropertyByColumnName(col);
			assertSame(prop, prop.createAssociation().getInverse());
			assertNull(prop.createAssociation().getObverse());
		});
	}

	@Test
	public void testColumnInnerTypeNull() {
		SpannerPersistentEntityImpl<TestEntity> entity =
				(SpannerPersistentEntityImpl<TestEntity>) (new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class));

		assertNull(entity.getPersistentPropertyByColumnName("custom_col")
				.getColumnInnerType());
	}

	@Test
	public void testColumnInnerType() {
		SpannerPersistentEntityImpl<TestEntity> entity =
				(SpannerPersistentEntityImpl<TestEntity>) (new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class));

		assertEquals(Double.class, entity.getPersistentPropertyByColumnName("doubleList")
				.getColumnInnerType());
	}

	@SpannerTable(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@SpannerColumn(name = "custom_col")
		String something;

		@SpannerColumn(name = "")
		String other;

		@SpannerColumnInnerType(innerType = Double.class)
		List<Double> doubleList;
	}
}
