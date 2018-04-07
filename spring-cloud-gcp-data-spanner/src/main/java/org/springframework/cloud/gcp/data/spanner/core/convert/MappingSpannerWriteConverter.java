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
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

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
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;

/**
 * @author Balint Pato
 * @author Chengyuan Zhao
 */
public class MappingSpannerWriteConverter extends AbstractSpannerCustomConverter
		implements EntityWriter<Object, WriteBuilder> {

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

	public static final Map<Class, BiFunction<ValueBinder, ?, ?>> singleItemType2ToMethodMap;

	static {
		ImmutableMap.Builder<Class, BiFunction<ValueBinder, ?, ?>> builder =
				new ImmutableMap.Builder<>();

		builder.put(Date.class,
				((BiFunction<ValueBinder, Date, ?>) ValueBinder::to));
		builder.put(Boolean.class,
				((BiFunction<ValueBinder, Boolean, ?>) ValueBinder::to));
		builder.put(Long.class,
				((BiFunction<ValueBinder, Long, ?>) ValueBinder::to));
		builder.put(String.class,
				((BiFunction<ValueBinder, String, ?>) ValueBinder::to));
		builder.put(Double.class,
				((BiFunction<ValueBinder, Double, ?>) ValueBinder::to));
		builder.put(Timestamp.class,
				((BiFunction<ValueBinder, Timestamp, ?>) ValueBinder::to));
		builder.put(ByteArray.class,
				((BiFunction<ValueBinder, ByteArray, ?>) ValueBinder::to));
		builder.put(double[].class,
				((BiFunction<ValueBinder, double[], ?>) ValueBinder::toFloat64Array));
		builder.put(boolean[].class,
				((BiFunction<ValueBinder, boolean[], ?>) ValueBinder::toBoolArray));
		builder.put(long[].class,
				((BiFunction<ValueBinder, long[], ?>) ValueBinder::toInt64Array));

		singleItemType2ToMethodMap = builder.build();
	}

	private final SpannerMappingContext spannerMappingContext;

	MappingSpannerWriteConverter(
			SpannerMappingContext spannerMappingContext,
			CustomConversions customConversions) {
		super(customConversions, null);
		this.spannerMappingContext = spannerMappingContext;
	}

	@Override
	public void write(Object source, WriteBuilder sink) {
		write(source, sink, null);
	}

	/**
	 * Writes an object's properties to the sink.
	 * @param source the object to write
	 * @param sink the sink to which to write
	 * @param includeColumns the properties/columns to write. If null, then all columns
	 * are written.
	 */
	public void write(Object source, WriteBuilder sink, Set<String> includeColumns) {
		boolean writeAllColumns = includeColumns == null;
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(source);
		persistentEntity.doWithProperties(
				(PropertyHandler<SpannerPersistentProperty>) spannerPersistentProperty -> {
					if (!writeAllColumns && !includeColumns
							.contains(spannerPersistentProperty.getColumnName())) {
						return;
					}
					writeProperty(sink, accessor, spannerPersistentProperty);
				});
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
		 * Due to type erasure, binder methods for Iterable properties must be manually
		 * specified. ByteArray must be excluded since it implements Iterable, but is also
		 * explicitly supported by spanner.
		 */
		if (ConversionUtils.isIterableNonByteArrayType(propertyType)) {
			valueSet = attemptSetIterableValue((Iterable) propertyValue, valueBinder,
					property);
		}
		else {
			valueSet = attemptSetSingleItemValue(propertyValue, valueBinder,
					propertyType);
			if (!valueSet) {
				for (Class targetType : this.singleItemType2ToMethodMap.keySet()) {
					valueSet = attemptSetSingleItemValue(propertyValue, valueBinder,
							targetType);
					if (valueSet) {
						break;
					}
				}
			}
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
		if (innerType == null) {
			return false;
		}

		// due to checkstyle limit of 3 return statments per function, this variable is
		// used.
		boolean valueSet = false;

		if (this.iterablePropertyType2ToMethodMap.containsKey(innerType)) {
			this.iterablePropertyType2ToMethodMap.get(innerType).accept(valueBinder,
					value);
			valueSet = true;
		}

		if (!valueSet) {
			for (Class targetType : this.iterablePropertyType2ToMethodMap.keySet()) {
				if (canConvert(innerType, targetType)) {
					BiConsumer toMethod = iterablePropertyType2ToMethodMap
							.get(targetType);
					toMethod.accept(valueBinder,
							ConversionUtils.convertIterable(value, targetType, this));
					valueSet = true;
					break;
				}
			}
		}
		return valueSet;
	}

	private boolean attemptSetSingleItemValue(Object value,
			ValueBinder<WriteBuilder> valueBinder, Class targetType) {
		if (!canConvert(value.getClass(), targetType)) {
			return false;
		}
		Class innerType = ConversionUtils.boxIfNeeded(targetType);
		BiFunction toMethod = singleItemType2ToMethodMap.get(innerType);
		if (toMethod == null) {
			return false;
		}
		// We're just checking for the bind to have succeeded, we don't need to chain the result.
		Object ignored = toMethod.apply(valueBinder, convert(value, targetType));
		return true;
	}
}
