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

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.annotation.Id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Chengyuan Zhao
 */
public class DatastoreServiceObjectToKeyFactoryTests {

	private final Datastore datastore = mock(Datastore.class);

	private final DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();

	private final DatastoreServiceObjectToKeyFactory datastoreServiceObjectToKeyFactory =
			new DatastoreServiceObjectToKeyFactory(this.datastore);

	@Test
	public void getKeyFromIdKeyTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		Key key = new KeyFactory("project").setKind("kind").newKey("key");
		assertSame(key,
				this.datastoreServiceObjectToKeyFactory.getKeyFromId(key, "kind"));
	}

	@Test
	public void getKeyFromIdStringTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		assertEquals(new KeyFactory("p").setKind("custom_test_kind").newKey("key"),
				this.datastoreServiceObjectToKeyFactory.getKeyFromId("key",
						"custom_test_kind"));
	}

	@Test
	public void getKeyFromIdLongTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		assertEquals(new KeyFactory("p").setKind("custom_test_kind").newKey(3L),
				this.datastoreServiceObjectToKeyFactory.getKeyFromId(3L,
						"custom_test_kind"));
	}

	@Test(expected = DatastoreDataException.class)
	public void getKeyFromIdExceptionTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		this.datastoreServiceObjectToKeyFactory.getKeyFromId(true, "custom_test_kind");
	}

	@Test
	public void getKeyTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		TestEntityWithId testEntity = new TestEntityWithId();
		testEntity.id = "testkey";
		assertEquals(new KeyFactory("p").setKind("custom_test_kind").newKey("testkey"),
				this.datastoreServiceObjectToKeyFactory.getKeyFromObject(testEntity,
						this.datastoreMappingContext
								.getPersistentEntity(TestEntityWithId.class)));
	}

	@Test(expected = DatastoreDataException.class)
	public void getKeyNoIdTest() {
		this.datastoreServiceObjectToKeyFactory.getKeyFromObject(new TestEntityNoId(),
				this.datastoreMappingContext.getPersistentEntity(TestEntityNoId.class));
	}

	@Test
	public void nullIdTest() {
		assertNull(this.datastoreServiceObjectToKeyFactory
				.getKeyFromObject(new TestEntityWithId(), this.datastoreMappingContext
						.getPersistentEntity(TestEntityWithId.class)));
	}

	@Test
	public void allocateIdForObjectTest() {
		TestEntityWithId testEntityWithId = new TestEntityWithId();
		Key key = new KeyFactory("project").setKind("kind").newKey("key");
		when(this.datastore.allocateId((IncompleteKey) any())).thenReturn(key);
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("project"));
		Key allocatedKey = this.datastoreServiceObjectToKeyFactory
				.allocateKeyForObject(testEntityWithId, this.datastoreMappingContext
						.getPersistentEntity(testEntityWithId.getClass()));
		assertEquals(key, allocatedKey);
		assertEquals("key", testEntityWithId.id);
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntityWithId {
		@Id
		String id;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntityNoId {
	}
}
