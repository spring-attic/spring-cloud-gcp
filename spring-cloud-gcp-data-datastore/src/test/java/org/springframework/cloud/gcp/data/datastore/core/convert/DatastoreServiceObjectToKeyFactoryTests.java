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

package org.springframework.cloud.gcp.data.datastore.core.convert;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the object to key factory.
 *
 * @author Chengyuan Zhao
 */
public class DatastoreServiceObjectToKeyFactoryTests {

	/**
	 * Used to check exception types and messages.
	 */
	@Rule
	public ExpectedException expectedEx = ExpectedException.none();


	private final Datastore datastore = mock(Datastore.class);

	private final DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();

	private final DatastoreServiceObjectToKeyFactory datastoreServiceObjectToKeyFactory =
			new DatastoreServiceObjectToKeyFactory(this.datastore);

	@Test
	public void getKeyFromIdKeyTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		Key key = new KeyFactory("project").setKind("kind").newKey("key");
		assertThat(this.datastoreServiceObjectToKeyFactory.getKeyFromId(key, "kind")).isSameAs(key);
	}

	@Test
	public void getKeyFromIdStringTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		assertThat(this.datastoreServiceObjectToKeyFactory.getKeyFromId("key", "custom_test_kind"))
				.isEqualTo(new KeyFactory("p").setKind("custom_test_kind").newKey("key"));
	}

	@Test
	public void getKeyFromIdLongTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		assertThat(new KeyFactory("p").setKind("custom_test_kind").newKey(3L))
				.isEqualTo(this.datastoreServiceObjectToKeyFactory.getKeyFromId(3L, "custom_test_kind"));
	}

	@Test
	public void getKeyFromIdExceptionTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Keys can only be created using String or long values.");
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		this.datastoreServiceObjectToKeyFactory.getKeyFromId(true, "custom_test_kind");
	}

	@Test
	public void getKeyTest() {
		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("p").setKind("k"));
		TestEntityWithId testEntity = new TestEntityWithId();
		testEntity.id = "testkey";

		Key actual = this.datastoreServiceObjectToKeyFactory.getKeyFromObject(
				testEntity, this.datastoreMappingContext.getPersistentEntity(TestEntityWithId.class));
		Key expectedKey = new KeyFactory("p").setKind("custom_test_kind").newKey("testkey");

		assertThat(actual).isEqualTo(expectedKey);
	}

	@Test
	public void getKeyNoIdTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("An ID property was required but does not exist for the type: " +
				"class org.springframework.cloud.gcp.data.datastore.core.convert." +
				"DatastoreServiceObjectToKeyFactoryTests$TestEntityNoId");
		this.datastoreServiceObjectToKeyFactory.getKeyFromObject(new TestEntityNoId(),
				this.datastoreMappingContext.getPersistentEntity(TestEntityNoId.class));
	}

	@Test
	public void nullIdTest() {
		assertThat(this.datastoreServiceObjectToKeyFactory
				.getKeyFromObject(new TestEntityWithId(), this.datastoreMappingContext
						.getPersistentEntity(TestEntityWithId.class))).isNull();
	}

	@Test
	public void allocateIdForObjectTest() {
		TestEntityWithKeyId testEntityWithKeyId = new TestEntityWithKeyId();

		doAnswer((invocation) -> {
			IncompleteKey incompleteKey = (IncompleteKey) invocation.getArguments()[0];
			long id = 123L;
			if (incompleteKey.getAncestors().size() > 0) {
				id = 456L;
			}
			return Key.newBuilder(incompleteKey, id).build();
		}).when(this.datastore).allocateId((IncompleteKey) any());

		when(this.datastore.newKeyFactory()).thenReturn(new KeyFactory("project"));
		Key allocatedKey = this.datastoreServiceObjectToKeyFactory
				.allocateKeyForObject(testEntityWithKeyId, this.datastoreMappingContext
						.getPersistentEntity(testEntityWithKeyId.getClass()));
		Key key = new KeyFactory("project").setKind("custom_test_kind").newKey(123L);
		assertThat(allocatedKey).isEqualTo(key);
		assertThat(testEntityWithKeyId.id).isEqualTo(key);

		Key allocatedKeyWithAncestor = this.datastoreServiceObjectToKeyFactory
				.allocateKeyForObject(testEntityWithKeyId, this.datastoreMappingContext
						.getPersistentEntity(testEntityWithKeyId.getClass()), allocatedKey);
		Key keyWithAncestor = new KeyFactory("project").setKind("custom_test_kind")
				.addAncestor(PathElement.of(key.getKind(), key.getId()))
				.newKey(456L);
		assertThat(allocatedKeyWithAncestor).isEqualTo(keyWithAncestor);
		assertThat(testEntityWithKeyId.id).isEqualTo(keyWithAncestor);
	}

	@Test
	public void allocateIdForObjectNonKeyIdTest() {
		this.expectedEx.expect(DatastoreDataException.class);
		this.expectedEx.expectMessage("Only Key types are allowed for descendants id");

		TestEntityWithId testEntityWithId = new TestEntityWithId();
		KeyFactory keyFactory = new KeyFactory("project").setKind("kind");
		when(this.datastore.newKeyFactory()).thenReturn(keyFactory);
		this.datastoreServiceObjectToKeyFactory
				.allocateKeyForObject(testEntityWithId, this.datastoreMappingContext
						.getPersistentEntity(testEntityWithId.getClass()),
						keyFactory.newKey("ancestor"));
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntityWithId {
		@Id
		String id;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntityNoId {
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity(name = "custom_test_kind")
	private static class TestEntityWithKeyId {
		@Id
		Key id;
	}
}
