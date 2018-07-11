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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.List;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.mapping.Ancestors;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.annotation.Id;
import org.springframework.data.util.Pair;

import static org.junit.Assert.assertEquals;

/**
 * @author Chengyuan Zhao
 */
public class DatastoreDataUtilsTests {

	private final DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();

	@Test
	public void getKeyTest() {
		TestEntity testEntity = new TestEntity();
		testEntity.ancestors = ImmutableList.of(Pair.of("grandparent", "g"),
				Pair.of("parent", "p"));
		KeyFactory keyFactory = new KeyFactory("project");
		DatastoreDataUtils.getKey(testEntity, keyFactory,
				this.datastoreMappingContext.getPersistentEntity(TestEntity.class), x -> {
					List<PathElement> ancestors = x.getAncestors();
					assertEquals(2, ancestors.size());
					assertEquals(PathElement.of("grandparent", "g"), ancestors.get(0));
					assertEquals(PathElement.of("parent", "p"), ancestors.get(1));
					return Key.newBuilder("test", "test")
							.addAncestor(PathElement.of("x", "y")).build().getParent();
				});
		assertEquals("y", testEntity.id);
	}

	@Test(expected = DatastoreDataException.class)
	public void getKeyNoIdPropTest() {
		DatastoreDataUtils.getKey(new TestEntityNoId(), new KeyFactory("test"),
				this.datastoreMappingContext.getPersistentEntity(TestEntityNoId.class),
				x -> null);
	}

	@Test(expected = DatastoreDataException.class)
	public void getKeyBadIdTypeTest() {
		TestEntityBadIdType testEntityBadIdType = new TestEntityBadIdType();
		testEntityBadIdType.id = true;
		DatastoreDataUtils.getKey(testEntityBadIdType, new KeyFactory("test"),
				this.datastoreMappingContext
						.getPersistentEntity(TestEntityBadIdType.class),
				x -> null);
	}

	@Test(expected = DatastoreDataException.class)
	public void getKeyBadAncestorTypeTest() {
		TestEntity testEntity = new TestEntity();
		testEntity.ancestors = ImmutableList.of(Pair.of("grandparent", "g"),
				Pair.of("parent", true));
		KeyFactory keyFactory = new KeyFactory("project");
		DatastoreDataUtils.getKey(testEntity, keyFactory,
				this.datastoreMappingContext.getPersistentEntity(TestEntity.class),
				x -> null);
	}

	private static class TestEntityNoId {

		@Ancestors
		List<Pair<String, Object>> ancestors;
	}

	private static class TestEntityBadIdType {

		@Id
		Boolean id;

		@Ancestors
		List<Pair<String, Object>> ancestors;
	}

	private static class TestEntity {

		@Id
		String id;

		@Ancestors
		List<Pair<String, Object>> ancestors;
	}
}
