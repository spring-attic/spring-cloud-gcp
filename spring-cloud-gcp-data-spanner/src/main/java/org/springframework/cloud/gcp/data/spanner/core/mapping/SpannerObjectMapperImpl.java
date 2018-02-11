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
import java.util.List;
import java.util.function.Predicate;

import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Type;
import com.google.cloud.spanner.ValueBinder;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.util.Assert;

/**
 * @author Chengyuan Zhao
 */
public class SpannerObjectMapperImpl implements SpannerObjectMapper {

	private final SpannerMappingContext spannerMappingContext;

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
			throw new UnsupportedOperationException(
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

			for (Method method : source.getClass().getMethods()) {
				// the retrieval methods are named like getDate or getTimestamp
				if (!method.getName().startsWith("get")) {
					continue;
				}

				Class[] params = method.getParameterTypes();
				if (params.length != 1 || !name.getClass().isAssignableFrom(params[0])) {
					continue;
				}

				Class retType = method.getReturnType();
				if (propType.isAssignableFrom(retType)) {
					try {
						accessor.setProperty(property, method.invoke(source, name));
						valueSet = true;
						break;
					}
					catch (ReflectiveOperationException e) {
						throw new UnsupportedOperationException(
								"Could not set value for property in entity. Value type is "
										+ retType + " , entity's property type is "
										+ propType,
								e);
					}
				}
			}

			if (!valueSet) {
				throw new UnsupportedOperationException(
						"The value in column with name " + name
								+ " could not be converted to the corresponding property in the entity."
								+ " The property's type is " + propType);
			}
		}
		return object;
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
					Class<?> propertyType = spannerPersistentProperty.getType();
					ValueBinder<WriteBuilder> valueBinder = sink
							.set(spannerPersistentProperty.getColumnName());

					Class testPropertyType = propertyType.isPrimitive()
							? getBoxedFromPrimitive(propertyType)
							: propertyType;

					// Attempt an exact match first
					boolean valueSet = attemptSetValue(value, valueBinder,
							(paramType) -> paramType.equals(testPropertyType));

					if (!valueSet) {
						valueSet = attemptSetValue(value, valueBinder,
								(paramType) -> paramType
										.isAssignableFrom(testPropertyType));
					}

					if (!valueSet) {
						throw new UnsupportedOperationException(String.format(
								"Unsupported mapping for type: %s", value.getClass()));
					}
				});
	}

	private boolean attemptSetValue(Object value, ValueBinder<WriteBuilder> valueBinder,
			Predicate<Class> matchFunction) {
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
				try {
					method.invoke(valueBinder, value);
					return true;
				}
				catch (ReflectiveOperationException e) {
					throw new UnsupportedOperationException(
							"Could not set value for write mutation.", e);
				}
			}
		}
		return false;
	}

	private Class getBoxedFromPrimitive(Class primitive) {
		return Array.get(Array.newInstance(primitive, 1), 0).getClass();
	}
}
