/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.gcp.core.util.MapBuilder;
import org.springframework.cloud.gcp.data.datastore.core.convert.TestDatastoreItemCollections.ComparableBeanContextSupport;
import org.springframework.cloud.gcp.data.datastore.core.convert.TestItemWithEmbeddedEntity.EmbeddedEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorField;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DiscriminatorValue;
import org.springframework.cloud.gcp.data.datastore.entities.CustomMap;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for the entity converter.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */

public class DefaultDatastoreEntityConverterTests {
	private static final LocalDatastoreHelper HELPER = LocalDatastoreHelper.create(1.0);

	private static final DatastoreMappingContext datastoreMappingContext = new DatastoreMappingContext();

	private static final DatastoreEntityConverter ENTITY_CONVERTER =
			new DefaultDatastoreEntityConverter(new DatastoreMappingContext(),
					new TwoStepsConversions(new DatastoreCustomConversions(
							Collections.singletonList(new Converter<HashMap, String>() {
								@Nullable
								@Override
								public String convert(HashMap source) {
									return "Map was converted to String";
								}
							})), null, datastoreMappingContext));

	/**
	 * Used to check exception messages and types.
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Datastore datastore;

	@Before
	public void setUp() {
		this.datastore = HELPER.getOptions().toBuilder().setNamespace("ghijklmnop").build().getService();
	}

	@Test
	public void readTest() {
		byte[] bytes = { 1, 2, 3 };
		Key otherKey = Key.newBuilder("testproject", "test_kind", "test_name").build();
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
				.set("enumField", "WHITE")
				.set("keyField", otherKey)
				.build();
		TestDatastoreItem item = ENTITY_CONVERTER.read(TestDatastoreItem.class, entity);

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
		assertThat(item.getEnumField()).as("validate enum field").isEqualTo(TestDatastoreItem.Color.WHITE);
		assertThat(item.getKeyField()).as("validate key field").isEqualTo(otherKey);
	}

	@Test
	public void discriminatingReadTest() {
		Entity entityA = getEntityBuilder()
				.set("discrimination_column", "A", "unused", "anotherParentValue")
				.set("boolField", true)
				.set("intField", 99)
				.set("enumField", "WHITE")
				.build();

		Entity entityB = getEntityBuilder()
				.set("discrimination_column", "B", "unused", "anotherParentValue")
				.set("boolField", true)
				.set("intField", 99)
				.set("enumField", "WHITE")
				.build();

		Entity entityX = getEntityBuilder()
				.set("discrimination_column", "X", "unused", "anotherParentValue")
				.set("boolField", true)
				.set("intField", 99)
				.set("enumField", "WHITE")
				.build();

		// All the reads use the superclass type but verify to be instances of the subclasses.
		assertThat(ENTITY_CONVERTER.read(DiscrimEntityX.class, entityX)).isInstanceOf(DiscrimEntityX.class);
		assertThat(ENTITY_CONVERTER.read(DiscrimEntityX.class, entityA)).isInstanceOf(DiscrimEntityA.class);
		assertThat(ENTITY_CONVERTER.read(DiscrimEntityX.class, entityB)).isInstanceOf(DiscrimEntityB.class);

		// Because A is NOT a superclass of X, we cannot read entityX as type X. It falls back to
		// A.
		assertThat(ENTITY_CONVERTER.read(DiscrimEntityA.class, entityX)).isInstanceOf(DiscrimEntityA.class);
	}

	@Test
	public void conflictingDiscriminationTest() {

		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage("More than one class in an inheritance hierarchy " +
				"has the same DiscriminatorValue: ");

		Entity entityY = getEntityBuilder()
				.set("discrimination_column", "Y", "unused", "anotherParentValue")
				.set("boolField", true)
				.set("intField", 99)
				.set("enumField", "WHITE")
				.build();

		ENTITY_CONVERTER.read(DiscrimEntityY.class, entityY);
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
				.set("enumField", "BLACK")
				.build();
		TestDatastoreItem item = ENTITY_CONVERTER.read(TestDatastoreItem.class, entity);

		assertThat(item.getStringField()).as("validate null field").isNull();
	}

	@Test
	public void testWrongTypeReadException() {
		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage(
				"Unable to read " +
						"org.springframework.cloud.gcp.data.datastore.core.convert.TestDatastoreItem entity");
		this.thrown.expectMessage("Unable to read property boolField");
		this.thrown.expectMessage("Unable to convert class java.lang.Long to class java.lang.Boolean");

		Entity entity = getEntityBuilder()
				.set("stringField", "string value")
				.set("boolField", 123L)
				.build();

		ENTITY_CONVERTER.read(TestDatastoreItem.class, entity);
	}

	@Test
	public void testObjectEntityException() {
		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage("Unable to convert Datastore Entity to class java.lang.Object");

		Entity entity = getEntityBuilder()
				.set("stringField", "string value")
				.set("boolField", 123L)
				.build();

		ENTITY_CONVERTER.read(Object.class, entity);
	}

	@Test
	public void testWrongTypeReadExceptionList() {
		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage(
				"Unable to read " +
						"org.springframework.cloud.gcp.data.datastore.core.convert.TestDatastoreItem entity");
		this.thrown.expectMessage("Unable to read property boolField");
		this.thrown.expectMessage(
				"Unable to convert class " +
				"com.google.common.collect.SingletonImmutableList to class java.lang.Boolean");

		Entity entity = getEntityBuilder()
				.set("stringField", "string value")
				.set("boolField", ListValue.of(true))
				.build();

		ENTITY_CONVERTER.read(TestDatastoreItem.class, entity);
	}

	@Test
	public void writeTest() {
		byte[] bytesForBlob = { 1, 2, 3 };
		byte[] bytes = { 1, 2, 3 };
		Key otherKey = Key.newBuilder("testproject", "test_kind", "test_name").build();
		TestDatastoreItem item = new TestDatastoreItem();
		item.setDurationField(Duration.ofDays(1));
		item.setStringField("string value");
		item.setBoolField(true);
		item.setDoubleField(3.1415D);
		item.setLongField(123L);
		item.setLatLngField(LatLng.of(10, 20));
		item.setTimestampField(Timestamp.ofTimeSecondsAndNanos(30, 40));
		item.setBlobField(Blob.copyFrom(bytesForBlob));
		item.setIntField(99);
		item.setEnumField(TestDatastoreItem.Color.BLACK);
		item.setByteArrayField(bytes);
		item.setKeyField(otherKey);

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);

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
				.isEqualTo(Blob.copyFrom(bytesForBlob));
		assertThat(entity.getLong("intField")).as("validate int field").isEqualTo(99L);
		assertThat(entity.getString("enumField")).as("validate enum field").isEqualTo("BLACK");
		assertThat(entity.getBlob("byteArrayField")).as("validate blob field")
				.isEqualTo(Blob.copyFrom(bytes));
		assertThat(entity.getKey("keyField")).as("validate key field")
				.isEqualTo(otherKey);
	}

	@Test
	public void writeTestSubtypes() {
		DiscrimEntityD entityD = new DiscrimEntityD();
		entityD.stringField = "item D";
		entityD.intField = 10;
		entityD.enumField = TestDatastoreItem.Color.BLACK;

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(entityD, builder);

		Entity entity = builder.build();


		assertThat(entity.getString("stringField")).as("validate string field").isEqualTo("item D");
		assertThat(entity.getLong("intField")).as("validate int field").isEqualTo(10L);
		assertThat(entity.getString("enumField")).as("validate enum field").isEqualTo("BLACK");
		assertThat(entity.getList("discrimination_column")).as("validate discrimination field")
				.containsExactly(StringValue.of("D"), StringValue.of("B"), StringValue.of("X"));
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

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);

		Entity entity = builder.build();

		assertThat(entity.getValue("stringField").equals(new NullValue()))
				.as("validate null field").isTrue();
	}

	@Test
	public void testUnsupportedTypeWriteException() {
		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage("Unable to write testItemUnsupportedFields.unsupportedField");
		this.thrown.expectMessage("Unable to convert class " +
				"org.springframework.cloud.gcp.data.datastore.core.convert." +
				"TestItemUnsupportedFields$NewType to Datastore supported type.");

		TestItemUnsupportedFields item = new TestItemUnsupportedFields();
		item.setStringField("string value");
		item.setUnsupportedField(new TestItemUnsupportedFields.NewType(true));

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);
	}

	@Test
	public void testUnsupportedTypeWrite() {
		TestItemUnsupportedFields item = new TestItemUnsupportedFields();
		item.setStringField("string value");
		item.setUnsupportedField(new TestItemUnsupportedFields.NewType(true));

		DatastoreEntityConverter entityConverter = new DefaultDatastoreEntityConverter(
				new DatastoreMappingContext(), new TwoStepsConversions(new DatastoreCustomConversions(
				Arrays.asList(
						getIntegerToNewTypeConverter(),
						getNewTypeToIntegerConverter()
						)), null, datastoreMappingContext));
		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		assertThat(entity.getLong("unsupportedField")).as("validate custom conversion")
				.isEqualTo(1L);
		assertThat(entity.getString("stringField")).as("validate string field")
				.isEqualTo("string value");

		TestItemUnsupportedFields readItem =
				entityConverter.read(TestItemUnsupportedFields.class, entity);

		assertThat(item.equals(readItem)).as("read object should be equal to original").isTrue();
	}

	@Test
	public void testCollectionFieldsUnsupportedCollection() {

		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage("Unable to read " +
				"org.springframework.cloud.gcp.data.datastore.core.convert.TestDatastoreItemCollections entity;");
		this.thrown.expectMessage("Unable to read property beanContext;");

		this.thrown.expectMessage(
				"Failed to convert from type [java.util.ArrayList<?>] " +
						"to type [org.springframework.cloud.gcp.data.datastore.core.convert.TestDatastoreItemCollections$ComparableBeanContextSupport]");

		ComparableBeanContextSupport comparableBeanContextSupport = new ComparableBeanContextSupport();
		comparableBeanContextSupport.add("this implementation of Collection");
		comparableBeanContextSupport.add("is unsupported out of the box!");

		TestDatastoreItemCollections item = new TestDatastoreItemCollections(
				Arrays.asList(1, 2),
				comparableBeanContextSupport,
				new String[] { "abc", "def" }, new boolean[] {true, false}, null, null);

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);
		Entity entity = builder.build();

		TestDatastoreItemCollections result = ENTITY_CONVERTER.read(TestDatastoreItemCollections.class, entity);
	}

	@Test
	public void testCollectionFields() {
		byte[][] bytes = {{1, 2}, {3, 4}};
		List<byte[]> listByteArray = Arrays.asList(bytes);

		ComparableBeanContextSupport ComparableBeanContextSupport = new ComparableBeanContextSupport();
		ComparableBeanContextSupport.add("this implementation of Collection");
		ComparableBeanContextSupport.add("is supported through a custom converter!");

		TestDatastoreItemCollections item =
				new TestDatastoreItemCollections(
						Arrays.asList(1, 2),
						ComparableBeanContextSupport,
						new String[]{"abc", "def"}, new boolean[] {true, false}, bytes, listByteArray);

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(
						new DatastoreMappingContext(),
						new TwoStepsConversions(new DatastoreCustomConversions(
								Arrays.asList(
										new Converter<List<String>, ComparableBeanContextSupport>() {
											@Override
											public ComparableBeanContextSupport convert(List<String> source) {
												ComparableBeanContextSupport bcs = new ComparableBeanContextSupport();
												source.forEach(bcs::add);
												return bcs;
											}
										},
										new Converter<ComparableBeanContextSupport, List<String>>() {
											@Override
											public List<String> convert(ComparableBeanContextSupport bcs) {
												List<String> list = new ArrayList<>();
												bcs.iterator().forEachRemaining((s) -> list.add((String) s));
												return list;
											}
										})),
								null, datastoreMappingContext));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		List<Value<?>> intList = entity.getList("intList");
		assertThat(intList.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate int list values").isEqualTo(Arrays.asList(1L, 2L));

		List<Value<?>> stringArray = entity.getList("stringArray");
		assertThat(stringArray.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate string array values").isEqualTo(Arrays.asList("abc", "def"));

		List<Value<?>> beanContext = entity.getList("beanContext");
		assertThat(beanContext.stream().map(Value::get).collect(Collectors.toSet()))
				.as("validate bean context values")
				.isEqualTo(new HashSet<>(Arrays.asList("this implementation of Collection", "is supported through a custom converter!")));

		List<Value<?>> bytesVals = entity.getList("bytes");
		assertThat(bytesVals.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate array of byte[] values")
				.isEqualTo(Arrays.asList(Blob.copyFrom(new byte[]{1, 2}), Blob.copyFrom(new byte[]{3, 4})));

		List<Value<?>> listByteArrayVals = entity.getList("listByteArray");
		assertThat(listByteArrayVals.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate list of byte[]")
				.isEqualTo(Arrays.asList(Blob.copyFrom(new byte[]{1, 2}), Blob.copyFrom(new byte[]{3, 4})));

		TestDatastoreItemCollections readItem =
				entityConverter.read(TestDatastoreItemCollections.class, entity);

		assertThat(item.equals(readItem)).as("read object should be equal to original").isTrue();
	}

	@Test
	public void testCollectionFieldsNulls() {
		TestDatastoreItemCollections item =
				new TestDatastoreItemCollections(
						Arrays.asList(1, 2),
						null,
						null, new boolean[] {true, false}, null, null);

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);
		Entity entity = builder.build();

		List<Value<?>> intList = entity.getList("intList");
		assertThat(intList.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate int list values").isEqualTo(Arrays.asList(1L, 2L));

		List<Value<?>> stringArray = entity.getList("stringArray");
		assertThat(stringArray)
				.as("validate string array is null").isNull();

		List<Value<?>> beanContext = entity.getList("beanContext");
		assertThat(beanContext)
				.as("validate bean context is null")
				.isNull();

		TestDatastoreItemCollections readItem = ENTITY_CONVERTER.read(TestDatastoreItemCollections.class, entity);
		assertThat(item.equals(readItem)).as("read object should be equal to original").isTrue();

	}

	@Test
	public void testCollectionFieldsUnsupported() {
		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage("Unable to write collectionOfUnsupportedTypes.unsupportedElts");
		this.thrown.expectMessage("Unable to convert " +
						"class org.springframework.cloud.gcp.data.datastore.core.convert." +
						"TestItemUnsupportedFields$NewType to Datastore supported type.");

		TestItemUnsupportedFields.CollectionOfUnsupportedTypes item = getCollectionOfUnsupportedTypesItem();

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);
	}

	@Test
	public void testCollectionFieldsUnsupportedWriteOnly() {
		TestItemUnsupportedFields.CollectionOfUnsupportedTypes item = getCollectionOfUnsupportedTypesItem();

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext(),
						new TwoStepsConversions(new DatastoreCustomConversions(Collections.singletonList(
								getNewTypeToIntegerConverter())), null, datastoreMappingContext));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		List<Value<?>> intList = entity.getList("unsupportedElts");
		assertThat(intList.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate int list values").isEqualTo(Arrays.asList(1L, 0L));
	}

	@Test
	public void testCollectionFieldsUnsupportedWriteReadException() {
		this.thrown.expect(DatastoreDataException.class);
		this.thrown.expectMessage(
				"Unable to read org.springframework.cloud.gcp.data.datastore.core.convert." +
						"TestItemUnsupportedFields$CollectionOfUnsupportedTypes entity");
		this.thrown.expectMessage("Unable to read property unsupportedElts");
		this.thrown.expectMessage("Unable process elements of a collection");
		this.thrown.expectMessage(
				"No converter found capable of converting from type [java.lang.Integer] " +
				"to type [org.springframework.cloud.gcp.data.datastore.core.convert." +
				"TestItemUnsupportedFields$NewType]");

		TestItemUnsupportedFields.CollectionOfUnsupportedTypes item = getCollectionOfUnsupportedTypesItem();

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext(),
						new TwoStepsConversions(new DatastoreCustomConversions(Collections.singletonList(
								getNewTypeToIntegerConverter()
						)), null, datastoreMappingContext));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		entityConverter.read(TestItemUnsupportedFields.CollectionOfUnsupportedTypes.class, entity);
	}

	@Test
	public void testCollectionFieldsUnsupportedWriteRead() {
		TestItemUnsupportedFields.CollectionOfUnsupportedTypes item = getCollectionOfUnsupportedTypesItem();

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(new DatastoreMappingContext(),
						new TwoStepsConversions(new DatastoreCustomConversions(Arrays.asList(
								getIntegerToNewTypeConverter(),
								getNewTypeToIntegerConverter()
						)), null, datastoreMappingContext));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		List<Value<?>> intList = entity.getList("unsupportedElts");
		assertThat(intList.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate long list values").isEqualTo(Arrays.asList(1L, 0L));

		TestItemUnsupportedFields.CollectionOfUnsupportedTypes read =
				entityConverter.read(TestItemUnsupportedFields.CollectionOfUnsupportedTypes.class, entity);

		assertThat(read.equals(item)).as("read object should be equal to original").isTrue();

	}

	private TestItemUnsupportedFields.CollectionOfUnsupportedTypes getCollectionOfUnsupportedTypesItem() {
		TestItemUnsupportedFields.CollectionOfUnsupportedTypes item =
				new TestItemUnsupportedFields.CollectionOfUnsupportedTypes();

		item.getUnsupportedElts().addAll(
				Arrays.asList(
						new TestItemUnsupportedFields.NewType(true),
						new TestItemUnsupportedFields.NewType(false)));
		return item;
	}

	@Test
	public void testUnindexedField() {
		UnindexedTestDatastoreItem item = new UnindexedTestDatastoreItem();
		item.setIndexedField(1L);
		item.setUnindexedField(2L);
		item.setUnindexedStringListField(Arrays.asList("a", "b"));
		item.setUnindexedMapField(new MapBuilder<String, String>().put("c", "C").put("d", "D").build());
		item.setEmbeddedItem(new UnindexedTestDatastoreItem(2, new UnindexedTestDatastoreItem(3, null)));
		item.setUnindexedItems(Collections.singletonList(new UnindexedTestDatastoreItem(4, new UnindexedTestDatastoreItem(5, null))));

		DatastoreEntityConverter entityConverter = new DefaultDatastoreEntityConverter(
						new DatastoreMappingContext(),
						new DatastoreServiceObjectToKeyFactory(() -> this.datastore));
		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		assertThat(entity.getLong("indexedField")).as("validate indexed field value")
				.isEqualTo(1L);

		assertThat(entity.getLong("unindexedField")).as("validate unindexed field value")
				.isEqualTo(2L);

		assertThat(entity.getValue("indexedField").excludeFromIndexes())
				.as("validate excludeFromIndexes on indexed field").isFalse();
		assertThat(entity.getValue("unindexedField").excludeFromIndexes())
				.as("validate excludeFromIndexes on unindexed field").isTrue();

		// the ListValue itself must NOT be unindexed or it will cause exception. the contents
		// individually must be. Same for map.
		assertThat(entity.getValue("unindexedStringListField").excludeFromIndexes()).isFalse();
		assertThat(((ListValue) entity.getValue("unindexedStringListField")).get().get(0).excludeFromIndexes())
				.isTrue();
		assertThat(((ListValue) entity.getValue("unindexedStringListField")).get().get(1).excludeFromIndexes())
				.isTrue();

		assertThat(entity.getValue("unindexedMapField").excludeFromIndexes()).isFalse();
		assertThat(((EntityValue) entity.getValue("unindexedMapField")).get().getValue("c").excludeFromIndexes())
				.isTrue();
		assertThat(((EntityValue) entity.getValue("unindexedMapField")).get().getValue("d").excludeFromIndexes())
				.isTrue();

		//Multi-level embedded entities - exclusion from indexes
		testMultiLevelEmbeddedEntityUnindexed(((EntityValue) entity.getValue("embeddedItem")).get());
		//Multi-level embedded entities in a list - exclusion from indexes
		testMultiLevelEmbeddedEntityUnindexed(
						((EntityValue) (
										(ListValue) entity.getValue("unindexedItems")).get().get(0)
						).get());
	}

	private void testMultiLevelEmbeddedEntityUnindexed(FullEntity entity) {
		assertThat(entity.getValue("indexedField").excludeFromIndexes()).isTrue();
		assertThat(((EntityValue) entity.getValue("embeddedItem"))
						.get().getValue("indexedField").excludeFromIndexes()).isTrue();
	}

	@Test
	public void testEmbeddedEntity() {
		EmbeddedEntity embeddedEntityA = new EmbeddedEntity("item 0");
		EmbeddedEntity embeddedEntityB = new EmbeddedEntity("item 1");

		List<EmbeddedEntity> embeddedEntities = Arrays.asList(embeddedEntityA,
				embeddedEntityB);

		Map<String, String> mapSimpleValues = new HashMap<>();
		mapSimpleValues.put("a", "valueA");
		mapSimpleValues.put("b", "valueB");

		Map<String, String[]> mapListValues = new HashMap<>();
		mapListValues.put("a", new String[] { "valueA" });
		mapListValues.put("b", new String[] { "valueB" });

		Map<String, EmbeddedEntity> embeddedEntityMapEmbeddedEntity = new HashMap<>();
		embeddedEntityMapEmbeddedEntity.put("a", embeddedEntityA);
		embeddedEntityMapEmbeddedEntity.put("b", embeddedEntityB);

		Map<String, List<EmbeddedEntity>> embeddedEntityMapListOfEmbeddedEntities = new HashMap<>();
		embeddedEntityMapListOfEmbeddedEntities.put("a", Arrays.asList(embeddedEntityA));
		embeddedEntityMapListOfEmbeddedEntities.put("b", Arrays.asList(embeddedEntityB));

		Map<String, Map<Long, Map<String, String>>> nestedEmbeddedMap = new HashMap<>();
		Map<Long, Map<String, String>> nestedInnerEmbeddedMap = new HashMap<>();
		nestedInnerEmbeddedMap.put(1L, mapSimpleValues);
		nestedEmbeddedMap.put("outer1", nestedInnerEmbeddedMap);

		Map<TestDatastoreItem.Color, String> enumKeysMap = new HashMap<>();
		enumKeysMap.put(TestDatastoreItem.Color.BLACK, "black");
		enumKeysMap.put(TestDatastoreItem.Color.WHITE, "white");

		CustomMap customMap = new CustomMap();
		customMap.put("key1", "val1");

		TestItemWithEmbeddedEntity item = new TestItemWithEmbeddedEntity(123,
				new EmbeddedEntity("abc"), embeddedEntities, mapSimpleValues,
				mapListValues, embeddedEntityMapEmbeddedEntity,
				embeddedEntityMapListOfEmbeddedEntities, enumKeysMap, customMap);

		item.setNestedEmbeddedMaps(nestedEmbeddedMap);

		DatastoreEntityConverter entityConverter = new DefaultDatastoreEntityConverter(
				new DatastoreMappingContext(),
				new DatastoreServiceObjectToKeyFactory(() -> this.datastore));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		assertThat(entity.getList("listOfEmbeddedEntities").stream()
				.map((val) -> ((BaseEntity<?>) val.get()).getString("stringField")).collect(Collectors.toList()))
						.as("validate embedded entity").isEqualTo(Arrays.asList("item 0", "item 1"));

		assertThat(entity.getEntity("embeddedEntityField").getString("stringField"))
				.as("validate embedded entity").isEqualTo("abc");

		assertThat(entity.getLong("intField"))
				.as("validate int field").isEqualTo(123L);

		assertThat(entity.getEntity("nestedEmbeddedMaps").getEntity("outer1")
				.getEntity("1").getString("a")).isEqualTo("valueA");
		assertThat(entity.getEntity("nestedEmbeddedMaps").getEntity("outer1")
				.getEntity("1").getString("b")).isEqualTo("valueB");

		assertThat(entity.getEntity("embeddedMapSimpleValues").getString("a"))
				.isEqualTo("valueA");
		assertThat(entity.getEntity("embeddedMapSimpleValues").getString("b"))
				.isEqualTo("valueB");

		assertThat(entity.getEntity("embeddedMapListOfValues").getList("a"))
				.contains(StringValue.of("valueA"));
		assertThat(entity.getEntity("embeddedMapListOfValues").getList("b"))
				.contains(StringValue.of("valueB"));

		assertThat(entity.getEntity("embeddedEntityMapEmbeddedEntity").getEntity("a")
				.getString("stringField")).isEqualTo("item 0");
		assertThat(entity.getEntity("embeddedEntityMapEmbeddedEntity").getEntity("b")
				.getString("stringField")).isEqualTo("item 1");

		List<Value> embeddedMapValuesEmbeddedEntityA = entity
				.getEntity("embeddedEntityMapListOfEmbeddedEntities").getList("a");
		List<Value> embeddedMapValuesEmbeddedEntityB = entity
				.getEntity("embeddedEntityMapListOfEmbeddedEntities").getList("b");

		assertThat(((BaseEntity) embeddedMapValuesEmbeddedEntityA.get(0).get())
				.getString("stringField")).isEqualTo("item 0");
		assertThat(embeddedMapValuesEmbeddedEntityA.size()).isEqualTo(1);

		assertThat(((BaseEntity) embeddedMapValuesEmbeddedEntityB.get(0).get())
				.getString("stringField")).isEqualTo("item 1");
		assertThat(embeddedMapValuesEmbeddedEntityB.size()).isEqualTo(1);

		TestItemWithEmbeddedEntity read = entityConverter
				.read(TestItemWithEmbeddedEntity.class, entity);

		assertThat(read.getNestedEmbeddedMaps().get("outer1").get(1L).get("a")).isEqualTo("valueA");
		assertThat(read.getNestedEmbeddedMaps().get("outer1").get(1L).get("b")).isEqualTo("valueB");

		assertThat(entity.getEntity("customMap").getString("key1"))
				.isEqualTo("val1");

		assertThat(read).as("read objects equals the original one").isEqualTo(item);
	}

	@Test
	public void PrivateCustomMapExceptionTest() {
		ServiceConfigurationPrivateCustomMap config =
				new ServiceConfigurationPrivateCustomMap("a", new PrivateCustomMap());
		DatastoreEntityConverter entityConverter = new DefaultDatastoreEntityConverter(
				new DatastoreMappingContext(),
				new DatastoreServiceObjectToKeyFactory(() -> this.datastore));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(config, builder);
		Entity entity = builder.build();

		assertThatThrownBy(() -> {
			entityConverter.read(ServiceConfigurationPrivateCustomMap.class, entity);
		}).isInstanceOf(DatastoreDataException.class).hasMessageContaining(
				"Unable to create an instance of a custom map type: "
						+ "class org.springframework.cloud.gcp.data.datastore.core.convert."
						+ "DefaultDatastoreEntityConverterTests$PrivateCustomMap "
						+ "(make sure the class is public and has a public no-args constructor)");
	}

	@Test
	public void testMismatchedStringIdLongProperty() {
		this.thrown.expect(ConversionFailedException.class);
		this.thrown.expectMessage(
				"The given key doesn't have a numeric ID but a conversion to Long was attempted");
		ENTITY_CONVERTER.read(LongIdEntity.class,
				Entity.newBuilder(this.datastore.newKeyFactory().setKind("aKind").newKey("a")).build());
	}

	@Test
	public void testMismatchedLongIdStringProperty() {
		this.thrown.expect(ConversionFailedException.class);
		this.thrown.expectMessage(
				"The given key doesn't have a String name value but a conversion to String was attempted");
		ENTITY_CONVERTER.read(StringIdEntity.class,
				Entity.newBuilder(this.datastore.newKeyFactory().setKind("aKind").newKey(1)).build());
	}

	private Entity.Builder getEntityBuilder() {
		return Entity.newBuilder(this.datastore.newKeyFactory().setKind("aKind").newKey("1"));
	}

	private static Converter<TestItemUnsupportedFields.NewType, Integer> getNewTypeToIntegerConverter() {
		return new Converter<TestItemUnsupportedFields.NewType, Integer>() {
			@Override
			public Integer convert(TestItemUnsupportedFields.NewType source) {
				return source.isVal() ? 1 : 0;
			}
		};
	}

	private static Converter<Integer, TestItemUnsupportedFields.NewType> getIntegerToNewTypeConverter() {
		return new Converter<Integer, TestItemUnsupportedFields.NewType>() {
			@Override
			public TestItemUnsupportedFields.NewType convert(Integer source) {
				return new TestItemUnsupportedFields.NewType(source == 1);
			}
		};
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	private static class StringIdEntity {
		@Id
		String id;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	private static class LongIdEntity {
		@Id
		long id;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	@DiscriminatorField(field = "discrimination_column")
	@DiscriminatorValue("X")
	private static class DiscrimEntityX {
		TestDatastoreItem.Color enumField;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	@DiscriminatorValue("A")
	private static class DiscrimEntityA extends DiscrimEntityX {
		boolean boolField;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	@DiscriminatorValue("B")
	private static class DiscrimEntityB extends DiscrimEntityX {
		int intField;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	@DiscriminatorValue("D")
	private static class DiscrimEntityD extends DiscrimEntityB {
		String stringField;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	@DiscriminatorField(field = "discrimination_column")
	@DiscriminatorValue("Y")
	private static class DiscrimEntityY {
		TestDatastoreItem.Color enumField;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	@DiscriminatorValue("Y")
	private static class DiscrimEntityC extends DiscrimEntityY {
		int intField;
	}

	@org.springframework.cloud.gcp.data.datastore.core.mapping.Entity
	public class ServiceConfigurationPrivateCustomMap {
		@Id
		private String serviceName;

		private PrivateCustomMap customMap;

		public ServiceConfigurationPrivateCustomMap(String serviceName, PrivateCustomMap customMap) {
			this.serviceName = serviceName;
			this.customMap = customMap;
		}
	}

	private class PrivateCustomMap extends HashMap<String, Object> {
	}

}
