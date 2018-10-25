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
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.gcp.data.datastore.core.convert.TestItemWithEmbeddedEntity.EmbeddedEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */

public class DefaultDatastoreEntityConverterTests {
	private static final LocalDatastoreHelper HELPER = LocalDatastoreHelper.create(1.0);

	private static final DatastoreEntityConverter ENTITY_CONVERTER =
			new DefaultDatastoreEntityConverter(new DatastoreMappingContext(),
					new TwoStepsConversions(new DatastoreCustomConversions(
							Collections.singletonList(new Converter<HashMap, String>() {
								@Nullable
								@Override
								public String convert(HashMap source) {
									return "Map was converted to String";
								}
							})), null));

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
				)), null));
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
		this.thrown.expectMessage("Unable to read property doubleSet;");
		this.thrown.expectMessage(
				"Failed to convert from type [java.util.ArrayList<?>] " +
						"to type [com.google.common.collect.ImmutableSet<?>]");

		TestDatastoreItemCollections item = new TestDatastoreItemCollections(
				Arrays.asList(1, 2),
				ImmutableSet.of(3.14, 2.71),
				new String[] { "abc", "def" }, new boolean[] {true, false}, null, null);

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);
		Entity entity = builder.build();

		ENTITY_CONVERTER.read(TestDatastoreItemCollections.class, entity);
	}

	@Test
	public void testCollectionFields() {
		byte[][] bytes = {{1, 2}, {3, 4}};
		List<byte[]> listByteArray = Arrays.asList(bytes);
		TestDatastoreItemCollections item =
				new TestDatastoreItemCollections(
						Arrays.asList(1, 2),
						ImmutableSet.of(3.14, 2.71),
						new String[]{"abc", "def"}, new boolean[] {true, false}, bytes, listByteArray);

		DatastoreEntityConverter entityConverter =
				new DefaultDatastoreEntityConverter(
						new DatastoreMappingContext(),
						new TwoStepsConversions(new DatastoreCustomConversions(
								Collections.singletonList(
										new Converter<List<?>, ImmutableSet<?>>() {
											@Override
											public ImmutableSet<?> convert(List<?> source) {
												return ImmutableSet.copyOf(source);
											}
										})), null));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		List<Value<?>> intList = entity.getList("intList");
		assertThat(intList.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate int list values").isEqualTo(Arrays.asList(1L, 2L));

		List<Value<?>> stringArray = entity.getList("stringArray");
		assertThat(stringArray.stream().map(Value::get).collect(Collectors.toList()))
				.as("validate string array values").isEqualTo(Arrays.asList("abc", "def"));

		List<Value<?>> doubleSet = entity.getList("doubleSet");
		assertThat(doubleSet.stream().map(Value::get).collect(Collectors.toSet()))
				.as("validate double set values")
				.isEqualTo(new HashSet<>(Arrays.asList(3.14, 2.71)));

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

		List<Value<?>> doubleSet = entity.getList("doubleSet");
		assertThat(doubleSet)
				.as("validate double set is null")
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
								getNewTypeToIntegerConverter())), null));

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
						)), null));

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
						)), null));

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

		Entity.Builder builder = getEntityBuilder();
		ENTITY_CONVERTER.write(item, builder);
		Entity entity = builder.build();

		assertThat(entity.getLong("indexedField")).as("validate indexed field value")
				.isEqualTo(1L);

		assertThat(entity.getLong("unindexedField")).as("validate unindexed field value")
				.isEqualTo(2L);

		assertThat(entity.getValue("indexedField").excludeFromIndexes())
				.as("validate excludeFromIndexes on indexed field").isFalse();
		assertThat(entity.getValue("unindexedField").excludeFromIndexes())
				.as("validate excludeFromIndexes on unindexed field").isTrue();
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

		TestItemWithEmbeddedEntity item = new TestItemWithEmbeddedEntity(123,
				new EmbeddedEntity("abc"), embeddedEntities, mapSimpleValues,
				mapListValues, embeddedEntityMapEmbeddedEntity,
				embeddedEntityMapListOfEmbeddedEntities);

		item.setNestedEmbeddedMaps(nestedEmbeddedMap);

		DatastoreEntityConverter entityConverter = new DefaultDatastoreEntityConverter(
				new DatastoreMappingContext(),
				new DatastoreServiceObjectToKeyFactory(this.datastore));

		Entity.Builder builder = getEntityBuilder();
		entityConverter.write(item, builder);
		Entity entity = builder.build();

		assertThat(entity.getList("listOfEmbeddedEntities").stream()
				.map(val -> ((BaseEntity<?>) val.get()).getString("stringField")).collect(Collectors.toList()))
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
		assertEquals(1, embeddedMapValuesEmbeddedEntityA.size());

		assertThat(((BaseEntity) embeddedMapValuesEmbeddedEntityB.get(0).get())
				.getString("stringField")).isEqualTo("item 1");
		assertEquals(1, embeddedMapValuesEmbeddedEntityB.size());

		TestItemWithEmbeddedEntity read = entityConverter
				.read(TestItemWithEmbeddedEntity.class, entity);

		assertEquals("valueA",
				read.getNestedEmbeddedMaps().get("outer1").get(1L).get("a"));
		assertEquals("valueB",
				read.getNestedEmbeddedMaps().get("outer1").get(1L).get("b"));

		assertThat(read).as("read objects equals the original one").isEqualTo(item);
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

}
