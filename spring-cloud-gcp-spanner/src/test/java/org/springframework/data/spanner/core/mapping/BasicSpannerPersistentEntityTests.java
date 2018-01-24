/*
 *  Copyright 2017 original author or authors.
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

package org.springframework.data.spanner.core.mapping;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.data.annotation.Id;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Chengyuan Zhao
 */
@RunWith(SpringRunner.class)
public class BasicSpannerPersistentEntityTests {

	@Test
	public void testTableName() {
		BasicSpannerPersistentEntity<TestEntity> entity = new BasicSpannerPersistentEntity<>(
				ClassTypeInformation.from(TestEntity.class));

		Assert.assertEquals("custom_test_table", entity.tableName());
	}

	@Test
	public void testColumns() {
		BasicSpannerPersistentEntity<TestEntity> entity =
				(BasicSpannerPersistentEntity<TestEntity>) (new SpannerMappingContext()
				.getPersistentEntity(TestEntity.class));

		Iterator<String> colIter = entity.columns().iterator();
		Set<String> cols = new HashSet<>();
		cols.add("custom_col");
		cols.add("id");

		int colCount = 0;
		String col;

		while (colIter.hasNext()) {
			colCount++;
			col = colIter.next();
			Assert.assertTrue(cols.contains(col));
		}

		Assert.assertEquals(2, colCount);
	}

	@Table(name = "custom_test_table")
	private static class TestEntity {
		@Id
		String id;

		@Column(name = "custom_col")
		String something;
	}
}
