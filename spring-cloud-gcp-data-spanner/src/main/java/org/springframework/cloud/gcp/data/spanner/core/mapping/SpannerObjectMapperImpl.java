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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.AbstractStructReader;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.ValueBinder;
import com.google.common.collect.ImmutableMap;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 */
public class SpannerObjectMapperImpl implements SpannerObjectMapper {

	private final SpannerMappingContext spannerMappingContext;

	private static final Map<Class, BiConsumer<ValueBinder<WriteBuilder>, Iterable>>
			iterablePropertyType2ToMethodMap;

	static {
		// Java 8 has compile errors when using the builder extension methods
		ImmutableMap.Builder<Class, BiConsumer<ValueBinder<WriteBuilder>, Iterable>> builder =
				new ImmutableMap.Builder<>();

		builder.put(Date.class, ValueBinder::toDateArray);
		builder.put(Boolean.class, ValueBinder::toBoolArray);
		builder.put(Long.class, ValueBinder::toInt64Array);
		builder.put(String.class, ValueBinder::toStringArray);
		builder.put(Double.class, ValueBinder::toFloat64Array);
		builder.put(Timestamp.class, ValueBinder::toTimestampArray);
		builder.put(ByteArray.class, ValueBinder::toBytesArray);

		iterablePropertyType2ToMethodMap = builder.build();
	}

	private static final Map<Class, BiFunction<Struct, String, List>>
			readIterableMapping =
			new ImmutableMap.Builder<Class, BiFunction<Struct, String, List>>()
					.put(Boolean.class, AbstractStructReader::getBooleanList)
					.put(Long.class, AbstractStructReader::getLongList)
					.put(String.class, AbstractStructReader::getStringList)
					.put(Double.class, AbstractStructReader::getDoubleList)
					.put(Timestamp.class, AbstractStructReader::getTimestampList)
					.put(Date.class, AbstractStructReader::getDateList)
					.put(ByteArray.class, AbstractStructReader::getBytesList)
					.build();

	private static final Map<Class, BiFunction<Struct, String, ?>> singleItemReadMethodMapping =
			new ImmutableMap.Builder<Class, BiFunction<Struct, String, ?>>()
			.put(Boolean.class, AbstractStructReader::getBoolean)
			.put(Long.class, AbstractStructReader::getLong)
			.put(String.class, AbstractStructReader::getString)
			.put(Double.class, AbstractStructReader::getDouble)
			.put(Timestamp.class, AbstractStructReader::getTimestamp)
			.put(Date.class, AbstractStructReader::getDate)
			.put(ByteArray.class, AbstractStructReader::getBytes)
			.put(double[].class, AbstractStructReader::getDoubleArray)
			.put(long[].class, AbstractStructReader::getLongArray)
			.put(boolean[].class, AbstractStructReader::getBooleanArray).build();

	private static final Map<Class, BiConsumer<ValueBinder<WriteBuilder>, ?>>
			singleItemType2ToMethodMap;

	static {
		ImmutableMap.Builder<Class, BiConsumer<ValueBinder<WriteBuilder>, ?>> builder =
				new ImmutableMap.Builder<>();

		builder.put(Date.class,
				((BiConsumer<ValueBinder<WriteBuilder>, Date>) ValueBinder::to));
		builder.put(Boolean.class,
				((BiConsumer<ValueBinder<WriteBuilder>, Boolean>) ValueBinder::to));
		builder.put(Long.class,
				((BiConsumer<ValueBinder<WriteBuilder>, Long>) ValueBinder::to));
		builder.put(String.class,
				((BiConsumer<ValueBinder<WriteBuilder>, String>) ValueBinder::to));
		builder.put(Double.class,
				((BiConsumer<ValueBinder<WriteBuilder>, Double>) ValueBinder::to));
		builder.put(Timestamp.class,
				((BiConsumer<ValueBinder<WriteBuilder>, Timestamp>) ValueBinder::to));
		builder.put(ByteArray.class,
				((BiConsumer<ValueBinder<WriteBuilder>, ByteArray>) ValueBinder::to));
		builder.put(double[].class,
				((BiConsumer<ValueBinder<WriteBuilder>, double[]>) ValueBinder::toFloat64Array));
		builder.put(boolean[].class,
				((BiConsumer<ValueBinder<WriteBuilder>, boolean[]>) ValueBinder::toBoolArray));
		builder.put(long[].class,
				((BiConsumer<ValueBinder<WriteBuilder>, long[]>) ValueBinder::toInt64Array));

		singleItemType2ToMethodMap = builder.build();
	}

	public SpannerObjectMapperImpl(SpannerMappingContext spannerMappingContext) {
		Assert.notNull(spannerMappingContext,
				"A valid mapping context for Spanner is required.");
		this.spannerMappingContext = spannerMappingContext;
	}

	@Override
	public <T> List<T> mapToList(ResultSet resultSet, Class<T> entityClass) {
		ArrayList<T> result = new ArrayList<>();
		while (resultSet.next()) {
			result.add(read(entityClass, resultSet.getCurrentRowAsStruct()));
		}
		return result;
	}

	private static Class boxIfNeeded(Class propertyType) {
		if (propertyType == null) {
			return null;
		}
		return propertyType.isPrimitive()
				? Array.get(Array.newInstance(propertyType, 1), 0).getClass()
				: propertyType;
	}

	private <R> R instantiate(Class<R> type) {
		R object;
		try {
			Constructor<R> constructor = type.getDeclaredConstructor();
			constructor.setAccessible(true);
			object = constructor.newInstance();
		}
		catch (ReflectiveOperationException e) {
			throw new SpannerDataException(
					"Unable to create a new instance of entity using default constructor.",
					e);
		}
		return object;
	}

	private boolean isIterableNonByteArrayType(Class propType) {
		return Iterable.class.isAssignableFrom(propType)
				&& !ByteArray.class.isAssignableFrom(propType);
	}

	/**
	 * Writes each of the source
	 * properties to the sink.
	 */
	@Override
	public void write(Object source, WriteBuilder sink) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(source);
		persistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> writeProperty(
						sink, accessor, spannerPersistentProperty));
	}

	@Override
	public <R> R read(Class<R> type, Struct source) {
		R object = instantiate(type);
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(type);

		PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(object);

		for (Type.StructField structField : source.getType().getStructFields()) {
			String columnName = structField.getName();
			SpannerPersistentProperty property = persistentEntity
					.getPersistentPropertyByColumnName(columnName);
			if (property == null) {
				throw new SpannerDataException(
						String.format("Error trying to read from Spanner struct column named %s"
										+ " that does not correspond to any property in the entity type ",
								columnName,
								type));
			}
			Class propType = property.getType();
			if (source.isNull(columnName)) {
				continue;
			}

			boolean valueSet;

			/*
			 * Due to type erasure, binder methods for Iterable properties must be
			 * manually specified. ByteArray must be excluded since it implements
			 * Iterable, but is also explicitly supported by spanner.
			 */
			if (isIterableNonByteArrayType(propType)) {
				valueSet = attemptReadIterableValue(property, source, columnName, accessor);
			}
			else {
				valueSet = attemptReadSingleItemValue(property, source, columnName,
						accessor);
			}

			if (!valueSet) {
				throw new SpannerDataException(
						String.format("The value in column with name %s"
								+ " could not be converted to the corresponding property in the entity."
								+ " The property's type is %s.", columnName, propType));
			}
		}
		return object;
	}

	/**
	 * <p>For each property this method "set"s the column name and finds the corresponding "to" method
	 * on the {@link ValueBinder} interface </p>
	 * <pre> {@code
	 *
	 * long singerId = my_singer_id;
	 * Mutation.WriteBuilder writeBuilder = Mutation.newInsertBuilder("Singer")
	 *         .set("SingerId")
	 *         .to(singerId)
	 *         .set("FirstName")
	 *         .to("Billy")
	 *         .set("LastName")
	 *         .to("Joel");
	 * } </pre>
	 */
	private void writeProperty(WriteBuilder sink, PersistentPropertyAccessor accessor,
			SpannerPersistentProperty property) {
		Object propertyValue = accessor.getProperty(property);

		if (propertyValue == null) {
			return;
		}

		Class<?> propertyType = property.getType();
		ValueBinder<WriteBuilder> valueBinder = sink.set(property.getColumnName());

		boolean valueSet;

		/*
		 * Due to type erasure, binder methods for Iterable properties must be
		 * manually specified. ByteArray must be excluded since it implements
		 * Iterable, but is also explicitly supported by spanner.
		 */
		if (isIterableNonByteArrayType(propertyType)) {
			valueSet = attemptSetIterableValue((Iterable) propertyValue, valueBinder,
					property);
		}
		else {
			valueSet = attemptSetSingleItemValue(propertyValue, valueBinder, property);
		}

		if (!valueSet) {
			throw new SpannerDataException(String.format(
					"Unsupported mapping for type: %s", propertyValue.getClass()));
		}
	}

	private boolean attemptSetIterableValue(Iterable value,
			ValueBinder<WriteBuilder> valueBinder,
			SpannerPersistentProperty spannerPersistentProperty) {
		return handleIterableInnerTypeMapping(spannerPersistentProperty,
				iterablePropertyType2ToMethodMap,
				(binderFunction) -> binderFunction.accept(valueBinder, value));
	}

	private boolean attemptReadIterableValue(
			SpannerPersistentProperty spannerPersistentProperty, Struct struct,
			String colName, PersistentPropertyAccessor accessor) {
		return handleIterableInnerTypeMapping(spannerPersistentProperty,
				readIterableMapping,
				(readerFunction) -> accessor.setProperty(spannerPersistentProperty,
						readerFunction.apply(struct, colName)));
	}

	private <T> boolean handleIterableInnerTypeMapping(
			SpannerPersistentProperty spannerPersistentProperty,
			Map<Class, T> innerTypeMapping, Consumer<T> innerTypeMappingOperation) {
		Class innerType = boxIfNeeded(spannerPersistentProperty.getColumnInnerType());
		if (innerType == null) {
			return false;
		}
		for (Class type : innerTypeMapping.keySet()) {
			if (type.isAssignableFrom(innerType)) {
				innerTypeMappingOperation.accept(innerTypeMapping.get(type));
				return true;
			}
		}
		return false;
	}

	private boolean attemptReadSingleItemValue(
			SpannerPersistentProperty spannerPersistentProperty, Struct struct,
			String colName, PersistentPropertyAccessor accessor) {
		BiFunction readFunction = singleItemReadMethodMapping
				.get(boxIfNeeded(spannerPersistentProperty.getType()));
		if (readFunction == null) {
			return false;
		}
		accessor.setProperty(spannerPersistentProperty,
				readFunction.apply(struct, colName));
		return true;
	}

	private boolean attemptSetSingleItemValue(Object value,
			ValueBinder<WriteBuilder> valueBinder,
			SpannerPersistentProperty spannerPersistentProperty) {
		BiConsumer setterMethod = singleItemType2ToMethodMap
				.get(boxIfNeeded(spannerPersistentProperty.getType()));
		if (setterMethod == null) {
			return false;
		}
		setterMethod.accept(valueBinder, value);
		return true;
	}
}
