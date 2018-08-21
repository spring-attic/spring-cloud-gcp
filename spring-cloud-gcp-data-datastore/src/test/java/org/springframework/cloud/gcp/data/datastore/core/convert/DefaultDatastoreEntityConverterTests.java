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

import java.time.Duration;
import java.util.Arrays;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */

public class DefaultDatastoreEntityConverterTests {
	private static final LocalDatastoreHelper HELPER = LocalDatastoreHelper.create(1.0);

	private Datastore datastore;

	@Before
	public void setUp() {
		this.datastore = HELPER.getOptions().toBuilder().setNamespace("ghijklmnop").build().getService();
	}

	@Test
	public void readTest() {
		byte[] bytes = { 1, 2, 3 };
		Entity entity = getEntityBuilder()
				.set("durationField", "PT24H")
				.set("stringField", "string value")
				.set("boolField", true)
				.set("doubleField", 3.1415D)
				.set("longField", 123L)
				.set("latLngField", LatLng.of(10, 20))
				.set("timestampField", Timestamp.ofTimeSecondsAndNanos(30, 40))
				.set("blobField", Blob.copyFrom(bytes))
				.set("intField", 99)
				.build();
		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext());
		TestDatastoreItem item = entityConverter.read(TestDatastoreItem.class, entity);

		assertThat(item.getDurationField()).as("validate duration field").isEqualTo(Duration.ofDays(1));
		assertThat(item.getStringField()).as("validate string field").isEqualTo("string value");
		assertThat(item.getBoolField()).as("validate boolean field").isTrue();
		assertThat(item.getDoubleField()).as("validate double field").isEqualTo(3.1415D);
		assertThat(item.getLongField()).as("validate long field").isEqualTo(123L);
		assertThat(item.getLatLngField()).as("validate latLng field")
				.isEqualTo(LatLng.of(10, 20));
		assertThat(item.getTimestampField()).as("validate timestamp field")
				.isEqualTo(Timestamp.ofTimeSecondsAndNanos(30, 40));
		assertThat(item.getBlobField()).as("validate blob field").isEqualTo(Blob.copyFrom(bytes));
		assertThat(item.getIntField()).as("validate int field").isEqualTo(99);
	}

	@Test
	public void readNullTest() {
		byte[] bytes = { 1, 2, 3 };
		Entity entity = getEntityBuilder()
				.set("durationField", "PT24H")
				.set("stringField", new NullValue())
				.set("boolField", true)
				.set("doubleField", 3.1415D)
				.set("longField", 123L)
				.set("latLngField", LatLng.of(10, 20))
				.set("timestampField", Timestamp.ofTimeSecondsAndNanos(30, 40))
				.set("blobField", Blob.copyFrom(bytes))
				.set("intField", 99)
				.build();
		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext());
		TestDatastoreItem item = entityConverter.read(TestDatastoreItem.class, entity);

		assertThat(item.getStringField()).as("validate null field").isNull();
	}

	@Test(expected = DatastoreDataException.class)
	public void testWrongTypeReadException() {
		Entity entity = getEntityBuilder()
				.set("stringField", "string value")
				.set("boolField", 123L)
				.build();

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext());
		entityConverter.read(TestDatastoreItem.class, entity);
	}

	@Test
	public void writeTest() {
		byte[] bytes = { 1, 2, 3 };
		TestDatastoreItem item = new TestDatastoreItem();
		item.setDurationField(Duration.ofDays(1));
		item.setStringField("string value");
		item.setBoolField(true);
		item.setDoubleField(3.1415D);
		item.setLongField(123L);
		item.setLatLngField(LatLng.of(10, 20));
		item.setTimestampField(Timestamp.ofTimeSecondsAndNanos(30, 40));
		item.setBlobField(Blob.copyFrom(bytes));
		item.setIntField(99);

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext());
		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);

		Entity entity = builder.build();

		assertThat(entity.getString("durationField")).as("validate duration field")
				.isEqualTo("PT24H");
		assertThat(entity.getString("stringField")).as("validate string field")
				.isEqualTo("string value");
		assertThat(entity.getBoolean("boolField")).as("validate boolean field").isTrue();
		assertThat(entity.getDouble("doubleField")).as("validate double field").isEqualTo(3.1415D);
		assertThat(entity.getLong("longField")).as("validate long field").isEqualTo(123L);
		assertThat(entity.getLatLng("latLngField")).as("validate latLng field")
				.isEqualTo(LatLng.of(10, 20));
		assertThat(entity.getTimestamp("timestampField")).as("validate timestamp field")
				.isEqualTo(Timestamp.ofTimeSecondsAndNanos(30, 40));
		assertThat(entity.getBlob("blobField")).as("validate blob field")
				.isEqualTo(Blob.copyFrom(bytes));
		assertThat(entity.getLong("intField")).as("validate int field").isEqualTo(99L);
	}

	@Test
	public void writeNullTest() {
		byte[] bytes = { 1, 2, 3 };
		TestDatastoreItem item = new TestDatastoreItem();
		item.setStringField(null);
		item.setBoolField(true);
		item.setDoubleField(3.1415D);
		item.setLongField(123L);
		item.setLatLngField(LatLng.of(10, 20));
		item.setTimestampField(Timestamp.ofTimeSecondsAndNanos(30, 40));
		item.setBlobField(Blob.copyFrom(bytes));

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext());
		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);

		Entity entity = builder.build();

		assertThat(entity.getValue("stringField").equals(new NullValue()))
				.as("validate null field").isTrue();
	}

	@Test(expected = DatastoreDataException.class)
	public void testUnsupportedTypeWriteException() {
		TestDatastoreItemUnsupportedFields item = new TestDatastoreItemUnsupportedFields();
		item.setStringField("string value");
		item.setUnsupportedField(new TestDatastoreItemUnsupportedFields.UnsupportedType(true));

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext());
		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		System.out.println(builder.build());
	}

	@Test
	public void testUnsupportedTypeWrite() {
		TestDatastoreItemUnsupportedFields item = new TestDatastoreItemUnsupportedFields();
		item.setStringField("string value");
		item.setUnsupportedField(new TestDatastoreItemUnsupportedFields.UnsupportedType(true));

		DatastoreEntityConverter entityConverter = new DefaultDatastoreEntityConverter(
				new DatastoreMappingContext(), new TwoStepsConversions(new DatastoreCustomConversions(
				Arrays.asList(
						new Converter<Integer, TestDatastoreItemUnsupportedFields.UnsupportedType>() {
							@Override
							public TestDatastoreItemUnsupportedFields.UnsupportedType convert(Integer source) {
								return new TestDatastoreItemUnsupportedFields.UnsupportedType(source == 1);
							}
						},
						new Converter<TestDatastoreItemUnsupportedFields.UnsupportedType, Integer>() {
							@Override
							public Integer convert(TestDatastoreItemUnsupportedFields.UnsupportedType source) {
								return source.isVal() ? 1 : 0;
							}

						}
				))));
		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		assertThat(entity.getLong("unsupportedField")).as("validate custom conversion")
				.isEqualTo(1L);
		assertThat(entity.getString("stringField")).as("validate string field")
				.isEqualTo("string value");

		TestDatastoreItemUnsupportedFields readItem =
				entityConverter.read(TestDatastoreItemUnsupportedFields.class, entity);

		assertThat(item.equals(readItem)).as("read object should be equal to original").isTrue();
	}

	private Entity.Builder getEntityBuilder() {
		return Entity.newBuilder(this.datastore.newKeyFactory().setKind("aKind").newKey("1"));
	}
}
