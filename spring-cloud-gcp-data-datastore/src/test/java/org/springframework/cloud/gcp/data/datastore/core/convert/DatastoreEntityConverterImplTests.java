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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */

public class DatastoreEntityConverterImplTests {
	private static final LocalDatastoreHelper HELPER = LocalDatastoreHelper.create(1.0);

	private Datastore datastore;

	@Before
	public void setUp() throws Exception {
		datastore = HELPER.getOptions().toBuilder().setNamespace("ghijklmnop").build().getService();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void readTest() {
		byte[] bytes = { 1, 2, 3 };
		Entity entity = Entity.newBuilder(datastore.newKeyFactory().setKind("aKind").newKey("1"))
				.set("stringField", "string value")
				.set("boolField", true)
				.set("doubleField", 3.1415D)
				.set("longField", 123L)
				.set("latLngField", LatLng.of(10, 20))
				.set("timestampField", Timestamp.ofTimeSecondsAndNanos(30, 40))
				.set("blobField", Blob.copyFrom(bytes))
				.build();
		DatastoreEntityConverter entityConverter = new DatastoreEntityConverterImpl(new DatastoreMappingContext());
		TestDatastoreItem item = entityConverter.read(TestDatastoreItem.class, entity);

		assertThat(item.getStringField()).as("validate string field").isEqualTo("string value");
		assertThat(item.getBoolField()).as("validate boolean field").isTrue();
		assertThat(item.getDoubleField()).as("validate double field").isEqualTo(3.1415D);
		assertThat(item.getLongField()).as("validate long field").isEqualTo(123L);
		assertThat(item.getLatLngField()).as("validate latLng field").isEqualTo(LatLng.of(10, 20));
		assertThat(item.getTimestampField()).as("validate timestamp field")
				.isEqualTo(Timestamp.ofTimeSecondsAndNanos(30, 40));
		assertThat(item.getBlobField()).as("validate blob field").isEqualTo(Blob.copyFrom(bytes));
	}

	@Test
	public void writeTest() {
		byte[] bytes = { 1, 2, 3 };
		TestDatastoreItem item = new TestDatastoreItem();
		item.setStringField("string value");
		item.setBoolField(true);
		item.setDoubleField(3.1415D);
		item.setLongField(123L);
		item.setLatLngField(LatLng.of(10, 20));
		item.setTimestampField(Timestamp.ofTimeSecondsAndNanos(30, 40));
		item.setBlobField(Blob.copyFrom(bytes));

		DatastoreEntityConverter entityConverter = new DatastoreEntityConverterImpl(new DatastoreMappingContext());
		Entity.Builder builder = Entity.newBuilder(datastore.newKeyFactory().setKind("aKind").newKey("1"));
		entityConverter.write(item, builder);

		Entity entity = builder.build();

		assertThat(entity.getString("stringField")).as("validate string field").isEqualTo("string value");
		assertThat(entity.getBoolean("boolField")).as("validate boolean field").isTrue();
		assertThat(entity.getDouble("doubleField")).as("validate double field").isEqualTo(3.1415D);
		assertThat(entity.getLong("longField")).as("validate long field").isEqualTo(123L);
		assertThat(entity.getLatLng("latLngField")).as("validate latLng field").isEqualTo(LatLng.of(10, 20));
		assertThat(entity.getTimestamp("timestampField")).as("validate timestamp field")
				.isEqualTo(Timestamp.ofTimeSecondsAndNanos(30, 40));
		assertThat(entity.getBlob("blobField")).as("validate blob field").isEqualTo(Blob.copyFrom(bytes));
	}
}

class TestDatastoreItem {
	private String stringField;

	private Boolean boolField;

	private Double doubleField;

	private Long longField;

	private LatLng latLngField;

	private Timestamp timestampField;

	private Blob blobField;

	public String getStringField() {
		return stringField;
	}

	public void setStringField(String stringField) {
		this.stringField = stringField;
	}

	public Boolean getBoolField() {
		return boolField;
	}

	public void setBoolField(Boolean boolField) {
		this.boolField = boolField;
	}

	public Double getDoubleField() {
		return doubleField;
	}

	public void setDoubleField(Double doubleField) {
		this.doubleField = doubleField;
	}

	public Long getLongField() {
		return longField;
	}

	public void setLongField(Long longField) {
		this.longField = longField;
	}

	public LatLng getLatLngField() {
		return latLngField;
	}

	public void setLatLngField(LatLng latLngField) {
		this.latLngField = latLngField;
	}

	public Timestamp getTimestampField() {
		return timestampField;
	}

	public void setTimestampField(Timestamp timestampField) {
		this.timestampField = timestampField;
	}

	public Blob getBlobField() {
		return blobField;
	}

	public void setBlobField(Blob blobField) {
		this.blobField = blobField;
	}
}
