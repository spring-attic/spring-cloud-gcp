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

	// used for caching the methods used for reading to entities. Does not contain the
	// methods for properties with inner types, such as Iterable.
	private final Map<Class, Method> propertyReadMethodMapping = new HashMap<>();

	// used for caching the methods used for writing to mutations. Does not contain the
	// methods for properties with inner types, such as Iterable.
	private final Map<Class, Method> propertyType2ToMethodMap = new HashMap<>();

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
						+ " that does not correspond to any property in the entity type "
                , columnName
                , type));
			}
			Class propType = property.getType();
			if (source.isNull(columnName)) {
				continue;
			}

			boolean valueSet = false;

			/*
			 * Due to type erasure, binder methods for Iterable properties must be
			 * manually specified. ByteArray must be excluded since it implements
			 * Iterable, but is also explicitly supported by spanner.
			 */
			if (isIterableNonByteArrayType(propType)) {
				valueSet = attemptReadIterableValue(property, source, columnName, accessor);
			}
			else {
				Method getMethod = this.propertyReadMethodMapping.get(propType);
				if (getMethod == null) {
					getMethod = findReadMethod(source, propType);
				}
				if (getMethod != null) {
					try {
						accessor.setProperty(property, getMethod.invoke(source, columnName));
						this.propertyReadMethodMapping.put(propType, getMethod);
						valueSet = true;
					}
					catch (ReflectiveOperationException e) {
						throw new SpannerDataException(
                String.format(
                    "Could not set value for property in entity. Value type is %s"
												+ " , entity's property type is %s",
                    getMethod.getReturnType(),
                    propType),
								e);
					}
				}
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

  /**
   * Writes each of the <pre>source</pre> properties to the sink.
   * @param source
   * @param sink
   */
	@Override
	public void write(Object source, WriteBuilder sink) {
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(source);
		persistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>)
            spannerPersistentProperty-> writeProperty(sink, accessor, spannerPersistentProperty));
	}

  /**
   * <p>For each property this method "set"s the column name and finds the corresponding "to" method
   * on the {@link ValueBinder} interface
   * </p>
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
   *
   * @param sink
   * @param accessor
   * @param property
   */
  private void writeProperty(WriteBuilder sink, PersistentPropertyAccessor accessor,
      SpannerPersistentProperty property) {
    Object propertyValue = accessor.getProperty(property);

    if (propertyValue == null) {
      return;
    }

    Class<?> propertyType = property.getType();
    ValueBinder<WriteBuilder> valueBinder = sink.set(property.getColumnName());

    boolean valueSet = false;

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
      Method toMethod = this.propertyType2ToMethodMap.get(propertyType);

      Class testPropertyType = boxIfNeeded(propertyType);

      // Attempt an exact match first
      if (toMethod == null) {
        toMethod = findToMethod((paramType) -> paramType.equals(testPropertyType));
      }
      if (toMethod == null) {
        toMethod = findToMethod((paramType) -> paramType.isAssignableFrom(testPropertyType));
      }
      if (toMethod != null) {
        try {
          toMethod.invoke(valueBinder, propertyValue);
          this.propertyType2ToMethodMap.put(propertyType, toMethod);
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
          "Unsupported mapping for type: %s", propertyValue.getClass()));
    }
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

	private Method findToMethod(Predicate<Class> matchFunction) {
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

	private Class boxIfNeeded(Class propertyType) {
		return propertyType.isPrimitive() ?
        Array.get(Array.newInstance(propertyType, 1), 0).getClass()
        : propertyType;
	}
}
