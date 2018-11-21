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

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class EntityPropertyValueProviderTests {

	private static final LocalDatastoreHelper HELPER = LocalDatastoreHelper.create(1.0);

	private Datastore datastore;

	private TwoStepsConversions twoStepsConversion = new TwoStepsConversions(new DatastoreCustomConversions(), null);


	private DatastorePersistentEntity<TestDatastoreItem> persistentEntity =
			(DatastorePersistentEntity<TestDatastoreItem>)
					new DatastoreMappingContext().getPersistentEntity(TestDatastoreItem.class);

	@Before
	public void setUp() {
		this.datastore = HELPER.getOptions().toBuilder().setNamespace("ghijklmnop").build().getService();
	}

	@Test
	public void getPropertyValue() {
		byte[] bytes = { 1, 2, 3 };
		Entity entity = Entity.newBuilder(this.datastore.newKeyFactory().setKind("aKind").newKey("1"))
				.set("stringField", "string value")
				.set("boolField", true)
				.set("doubleField", 3.1415D)
				.set("longField", 123L)
				.set("latLngField", LatLng.of(10, 20))
				.set("timestampField", Timestamp.ofTimeSecondsAndNanos(30, 40))
				.set("blobField", Blob.copyFrom(bytes))
				.build();

		EntityPropertyValueProvider provider = new EntityPropertyValueProvider(entity, this.twoStepsConversion);

		assertThat((String) provider.getPropertyValue(this.persistentEntity.getPersistentProperty("stringField")))
				.as("validate string field").isEqualTo("string value");
		assertThat((Boolean) provider.getPropertyValue(this.persistentEntity.getPersistentProperty("boolField")))
				.as("validate boolean field").isTrue();
		assertThat((Double) provider.getPropertyValue(this.persistentEntity.getPersistentProperty("doubleField")))
				.as("validate double field").isEqualTo(3.1415D);
		assertThat((Long) provider.getPropertyValue(this.persistentEntity.getPersistentProperty("longField")))
				.as("validate long field").isEqualTo(123L);
		assertThat((LatLng) provider.getPropertyValue(this.persistentEntity.getPersistentProperty("latLngField")))
				.as("validate latLng field").isEqualTo(LatLng.of(10, 20));
		assertThat((Timestamp) provider.getPropertyValue(this.persistentEntity.getPersistentProperty("timestampField")))
				.as("validate timestamp field")
				.isEqualTo(Timestamp.ofTimeSecondsAndNanos(30, 40));
		assertThat((Blob) provider.getPropertyValue(this.persistentEntity.getPersistentProperty("blobField")))
				.as("validate blob field").isEqualTo(Blob.copyFrom(bytes));
	}

	@Test(expected = DatastoreDataException.class)
	public void testException() {
		Entity entity = Entity.newBuilder(this.datastore.newKeyFactory().setKind("aKind").newKey("1"))
				.set("boolField", 123L)
				.build();

		EntityPropertyValueProvider provider = new EntityPropertyValueProvider(entity, this.twoStepsConversion);

		provider.getPropertyValue(this.persistentEntity.getPersistentProperty("boolField"));
	}
}
