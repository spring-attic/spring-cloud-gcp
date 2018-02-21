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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.Map;
import java.util.function.BiConsumer;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.ValueBinder;
import com.google.common.collect.ImmutableMap;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
class MappingSpannerWriteConverter implements EntityWriter<Object, WriteBuilder> {

	private static final Map<Class, BiConsumer<ValueBinder<WriteBuilder>, Iterable>> iterablePropertyType2ToMethodMap;

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

	private static final Map<Class, BiConsumer<ValueBinder<WriteBuilder>, ?>> singleItemType2ToMethodMap;

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

	private final SpannerMappingContext spannerMappingContext;

	MappingSpannerWriteConverter(
			SpannerMappingContext spannerMappingContext) {
		this.spannerMappingContext = spannerMappingContext;
	}

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

	/**
	 * <p>
	 * For each property this method "set"s the column name and finds the corresponding "to"
	 * method on the {@link ValueBinder} interface
	 * </p>
	 * <pre>
	 * {
	 * 	&#64;code
	 *
	 * 	long singerId = my_singer_id;
	 * 	Mutation.WriteBuilder writeBuilder = Mutation.newInsertBuilder("Singer")
	 * 			.set("SingerId")
	 * 			.to(singerId)
	 * 			.set("FirstName")
	 * 			.to("Billy")
	 * 			.set("LastName")
	 * 			.to("Joel");
	 * }
	 * </pre>
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
		 * Due to type erasure, binder methods for Iterable properties must be manually specified.
		 * ByteArray must be excluded since it implements Iterable, but is also explicitly
		 * supported by spanner.
		 */
		if (ConversionUtils.isIterableNonByteArrayType(propertyType)) {
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

		Class innerType = ConversionUtils.boxIfNeeded(spannerPersistentProperty.getColumnInnerType());
		if (innerType == null || !iterablePropertyType2ToMethodMap.containsKey(innerType)) {
			return false;
		}
		BiConsumer toMethod = iterablePropertyType2ToMethodMap.get(innerType);
		toMethod.accept(valueBinder, value);
		return true;
	}

	private boolean attemptSetSingleItemValue(Object value,
			ValueBinder<WriteBuilder> valueBinder,
			SpannerPersistentProperty spannerPersistentProperty) {
		Class innerType = ConversionUtils.boxIfNeeded(spannerPersistentProperty.getType());
		BiConsumer toMethod = singleItemType2ToMethodMap.get(innerType);
		if (toMethod == null) {
			return false;
		}
		toMethod.accept(valueBinder, value);
		return true;
	}
}
