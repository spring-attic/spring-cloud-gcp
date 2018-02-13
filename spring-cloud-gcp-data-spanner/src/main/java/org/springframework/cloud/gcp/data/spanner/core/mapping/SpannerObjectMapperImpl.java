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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
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

	// used for caching the methods used for reading to entities. Does not contain the
	// methods for
	// properties with inner types, such as Iterable.
	private final Map<Class, Method> propertyReadMethodMapping = new HashMap<>();

	// used for caching the methods used for writing to mutations. Does not contain the
	// methods for
	// properties with inner types, such as Iterable.
	private final Map<Class, Method> propertyWriteMethodMapping = new HashMap<>();

	private static final Map<Class, BiConsumer<ValueBinder<WriteBuilder>, Iterable>>
			writeBuilderIterableMapping =
			// The casting of each biconsumer below is needed for Java 8 compilation, but not Java 9.
			new ImmutableMap.Builder<Class, BiConsumer<ValueBinder<WriteBuilder>, Iterable>>()
			.put(Boolean.class,
					(BiConsumer<ValueBinder<WriteBuilder>, Iterable>) (binder,
							value) -> binder.toBoolArray(value))
			.put(Long.class,
					(BiConsumer<ValueBinder<WriteBuilder>, Iterable>) (binder,
							value) -> binder.toInt64Array(value))
			.put(String.class,
					(BiConsumer<ValueBinder<WriteBuilder>, Iterable>) (binder,
							value) -> binder.toStringArray(value))
			.put(Double.class,
					(BiConsumer<ValueBinder<WriteBuilder>, Iterable>) (binder,
							value) -> binder.toFloat64Array(value))
			.put(Timestamp.class,
					(BiConsumer<ValueBinder<WriteBuilder>, Iterable>) (binder,
							value) -> binder.toTimestampArray(value))
			.put(Date.class,
					(BiConsumer<ValueBinder<WriteBuilder>, Iterable>) (binder,
							value) -> binder.toDateArray(value))
			.put(ByteArray.class,
					(BiConsumer<ValueBinder<WriteBuilder>, Iterable>) (binder,
							value) -> binder.toBytesArray(value))
			.build();

	private static final Map<Class, BiFunction<Struct, String, List>>
			readIterableMapping =
			new ImmutableMap.Builder<Class, BiFunction<Struct, String, List>>()
			.put(Boolean.class, (struct, colName) -> struct.getBooleanList(colName))
			.put(Long.class, (struct, colName) -> struct.getLongList(colName))
			.put(String.class, (struct, colName) -> struct.getStringList(colName))
			.put(Double.class, (struct, colName) -> struct.getDoubleList(colName))
			.put(Timestamp.class, (struct, colName) -> struct.getTimestampList(colName))
			.put(Date.class, (struct, colName) -> struct.getDateList(colName))
			.put(ByteArray.class, (struct, colName) -> struct.getBytesList(colName))
			.build();

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

	@Override
	public <R> R read(Class<R> type, Struct source) {
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
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(type);
		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(object);

		for (Type.StructField field : source.getType().getStructFields()) {
			String name = field.getName();
			SpannerPersistentProperty property = persistentEntity
					.getPersistentPropertyByColumnName(name);
			if (property == null) {
				throw new IllegalStateException("Spanner struct contains a column named "
						+ name
						+ " that does not correspond to any property in the entity type "
						+ type);
			}
			Class propType = property.getType();
			if (source.isNull(name)) {
				continue;
			}

			boolean valueSet = false;

			/*
			 * Due to type erasure, binder methods for Iterable properties must be
			 * manually specified. ByteArray must be excluded since it implements
			 * Iterable, but is also explicitly supported by spanner.
			 */
			if (isIterableNonByteArrayType(propType)) {
				valueSet = attemptReadIterableValue(property, source, name, accessor);
			}
			else {
				Method getMethod = this.propertyReadMethodMapping.get(propType);
				if (getMethod == null) {
					getMethod = findReadMethod(source, propType);
				}
				if (getMethod != null) {
					try {
						accessor.setProperty(property, getMethod.invoke(source, name));
						this.propertyReadMethodMapping.put(propType, getMethod);
						valueSet = true;
					}
					catch (ReflectiveOperationException e) {
						throw new SpannerDataException(
								"Could not set value for property in entity. Value type is "
										+ getMethod.getReturnType()
										+ " , entity's property type is " + propType,
								e);
					}
				}
			}

			if (!valueSet) {
				throw new SpannerDataException(
						"The value in column with name " + name
								+ " could not be converted to the corresponding property in the entity."
								+ " The property's type is " + propType);
			}
		}
		return object;
	}

	private Method findReadMethod(Struct source, Class propType) {
		for (Method method : source.getClass().getMethods()) {
			// the retrieval methods are named like getDate or getTimestamp
			if (!method.getName().startsWith("get")) {
				continue;
			}

			Class[] params = method.getParameterTypes();
			if (params.length != 1 || !String.class.isAssignableFrom(params[0])) {
				continue;
			}

			Class retType = method.getReturnType();
			if (propType.isAssignableFrom(retType)) {
				return method;
			}
		}
		return null;
	}

	private boolean isIterableNonByteArrayType(Class propType) {
		return Iterable.class.isAssignableFrom(propType)
				&& !ByteArray.class.equals(propType);
	}

	@Override
	public void write(Object source, WriteBuilder sink) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(source);
		persistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					Object value = accessor.getProperty(spannerPersistentProperty);

					if (value == null) {
						return;
					}

					Class<?> propertyType = spannerPersistentProperty.getType();
					ValueBinder<WriteBuilder> valueBinder = sink
							.set(spannerPersistentProperty.getColumnName());

					boolean valueSet = false;

					/*
					 * Due to type erasure, binder methods for Iterable properties must be
					 * manually specified. ByteArray must be excluded since it implements
					 * Iterable, but is also explicitly supported by spanner.
					 */
					if (isIterableNonByteArrayType(propertyType)) {
						valueSet = attemptSetIterableValue((Iterable) value, valueBinder,
								spannerPersistentProperty);
					}
					else {
						Method writeMethod = this.propertyWriteMethodMapping
								.get(propertyType);
						Class testPropertyType = propertyType.isPrimitive()
								? getBoxedFromPrimitive(propertyType)
								: propertyType;

						// Attempt an exact match first
						if (writeMethod == null) {
							writeMethod = findWriteMethod(
									(paramType) -> paramType.equals(testPropertyType));
						}
						if (writeMethod == null) {
							writeMethod = findWriteMethod((paramType) -> paramType
											.isAssignableFrom(testPropertyType));
						}
						if (writeMethod != null) {
							try {
								writeMethod.invoke(valueBinder, value);
								this.propertyWriteMethodMapping.put(propertyType,
										writeMethod);
								valueSet = true;
							}
							catch (ReflectiveOperationException e) {
								throw new SpannerDataException(
										"Could not set value for write mutation.", e);
							}
						}
					}

					if (!valueSet) {
						throw new SpannerDataException(String.format(
								"Unsupported mapping for type: %s", value.getClass()));
					}
				});
	}

	private <T> boolean handleIterableInnerTypeMapping(
			SpannerPersistentProperty spannerPersistentProperty,
			Map<Class, T> innerTypeMapping, Consumer<T> innerTypeMappingOperation) {
		Class innerType = spannerPersistentProperty.getColumnInnerType();
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

	private boolean attemptSetIterableValue(Iterable value,
			ValueBinder<WriteBuilder> valueBinder,
			SpannerPersistentProperty spannerPersistentProperty) {
		return handleIterableInnerTypeMapping(spannerPersistentProperty,
				writeBuilderIterableMapping,
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

	private Method findWriteMethod(Predicate<Class> matchFunction) {
		for (Method method : ValueBinder.class.getMethods()) {
			// the binding methods are named like to() or toInt64Array()
			if (!method.getName().startsWith("to")) {
				continue;
			}

			Class[] params = method.getParameterTypes();
			if (params.length != 1) {
				continue;
			}

			if (matchFunction.test(params[0])) {
				return method;
			}
		}
		return null;
	}

	private Class getBoxedFromPrimitive(Class primitive) {
		return Array.get(Array.newInstance(primitive, 1), 0).getClass();
	}
}
