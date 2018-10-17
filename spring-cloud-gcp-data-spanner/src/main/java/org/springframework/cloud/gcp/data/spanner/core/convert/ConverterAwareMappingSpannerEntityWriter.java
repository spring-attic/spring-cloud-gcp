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

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.ValueBinder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

/**
 * The primary class for adding values from entity objects to {@link WriteBuilder} for
 * the purpose of creating mutations for Spanner.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 *
 * @since 1.1
 */
public class ConverterAwareMappingSpannerEntityWriter implements SpannerEntityWriter {
	private static Set<Class> SPANNER_KEY_COMPATIBLE_TYPES = ImmutableSet
			.<Class>builder()
			.add(Boolean.class)
			.add(Integer.class)
			.add(Long.class)
			.add(Float.class)
			.add(Double.class)
			.add(String.class)
			.add(ByteArray.class)
			.add(Timestamp.class)
			.add(com.google.cloud.Date.class)
			.build();

	public static final Map<Class<?>, BiFunction<ValueBinder, ?, ?>> singleItemType2ToMethodMap;

	static final Map<Class<?>, BiConsumer<ValueBinder<?>, Iterable>>
			iterablePropertyType2ToMethodMap = createIterableTypeMapping();

	@SuppressWarnings("unchecked")
	private static Map<Class<?>, BiConsumer<ValueBinder<?>, Iterable>> createIterableTypeMapping() {
		// Java 8 has compile errors when using the builder extension methods
		// @formatter:off
		ImmutableMap.Builder<Class<?>, BiConsumer<ValueBinder<?>, Iterable>> builder =
						new ImmutableMap.Builder<>();
		// @formatter:on

		builder.put(com.google.cloud.Date.class, ValueBinder::toDateArray);
		builder.put(Boolean.class, ValueBinder::toBoolArray);
		builder.put(Long.class, ValueBinder::toInt64Array);
		builder.put(String.class, ValueBinder::toStringArray);
		builder.put(Double.class, ValueBinder::toFloat64Array);
		builder.put(Timestamp.class, ValueBinder::toTimestampArray);
		builder.put(ByteArray.class, ValueBinder::toBytesArray);

		return builder.build();
	}

	static {
		ImmutableMap.Builder<Class<?>, BiFunction<ValueBinder, ?, ?>> builder = new ImmutableMap.Builder<>();

		builder.put(Date.class, (BiFunction<ValueBinder, Date, ?>) ValueBinder::to);
		builder.put(Boolean.class, (BiFunction<ValueBinder, Boolean, ?>) ValueBinder::to);
		builder.put(Long.class, (BiFunction<ValueBinder, Long, ?>) ValueBinder::to);
		builder.put(long.class, (BiFunction<ValueBinder, Long, ?>) ValueBinder::to);
		builder.put(Double.class, (BiFunction<ValueBinder, Double, ?>) ValueBinder::to);
		builder.put(double.class, (BiFunction<ValueBinder, Double, ?>) ValueBinder::to);
		builder.put(String.class, (BiFunction<ValueBinder, String, ?>) ValueBinder::to);
		builder.put(Timestamp.class,
				(BiFunction<ValueBinder, Timestamp, ?>) ValueBinder::to);
		builder.put(ByteArray.class,
				(BiFunction<ValueBinder, ByteArray, ?>) ValueBinder::to);
		builder.put(double[].class,
				(BiFunction<ValueBinder, double[], ?>) ValueBinder::toFloat64Array);
		builder.put(boolean[].class,
				(BiFunction<ValueBinder, boolean[], ?>) ValueBinder::toBoolArray);
		builder.put(long[].class,
				(BiFunction<ValueBinder, long[], ?>) ValueBinder::toInt64Array);
		builder.put(Struct.class, (BiFunction<ValueBinder, Struct, ?>) ValueBinder::to);

		singleItemType2ToMethodMap = builder.build();
	}

	private final SpannerMappingContext spannerMappingContext;

	private final SpannerWriteConverter writeConverter;

	ConverterAwareMappingSpannerEntityWriter(SpannerMappingContext spannerMappingContext,
			SpannerWriteConverter writeConverter) {
		this.spannerMappingContext = spannerMappingContext;
		this.writeConverter = writeConverter;
	}

	@Override
	public void write(Object source, MultipleValueBinder sink) {
		write(source, sink, null);
	}

	/**
	 * Writes an object's properties to the sink.
	 * @param source the object to write
	 * @param sink the sink to which to write
	 * @param includeColumns the columns to write. If null, then all columns are written.
	 */
	public void write(Object source, MultipleValueBinder sink,
			Set<String> includeColumns) {
		boolean writeAllColumns = includeColumns == null;
		SpannerPersistentEntity<?> persistentEntity = this.spannerMappingContext
				.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(source);
		persistentEntity.doWithColumnBackedProperties(
				spannerPersistentProperty -> {
					if (spannerPersistentProperty.isEmbedded()) {
						Object embeddedObject = accessor
								.getProperty(spannerPersistentProperty);
						if (embeddedObject != null) {
							write(embeddedObject, sink, includeColumns);
						}
					}
					else if (writeAllColumns || includeColumns
								.contains(spannerPersistentProperty.getColumnName())) {
						writeProperty(sink, accessor, spannerPersistentProperty);
					}
				});
	}

	@Override
	public Key writeToKey(Object key) {
		Assert.notNull(key, "Key of an entity to be written cannot be null!");

		Key k;
		boolean isIterable = Iterable.class.isAssignableFrom(key.getClass());
		boolean isArray = Object[].class.isAssignableFrom(key.getClass());
		if ((isIterable || isArray) && !ByteArray.class.isAssignableFrom(key.getClass())) {
			Key.Builder kb = Key.newBuilder();
			for (Object keyPart : (isArray ? (Arrays.asList((Object[]) key))
					: ((Iterable) key))) {
				kb.appendObject(convertKeyPart(keyPart));
			}
			k = kb.build();
			if (k.size() == 0) {
				throw new SpannerDataException(
						"A key must have at least one component, but 0 were given.");
			}
		}
		else {
			k = Key.class.isAssignableFrom(key.getClass()) ? (Key) key
					: Key.of(convertKeyPart(key));
		}
		return k;
	}

	private Object convertKeyPart(Object object) {

		if (isValidSpannerKeyType(ConversionUtils.boxIfNeeded(object.getClass()))) {
			return object;
		}
		/*
		 * Iterate through the supported Key component types in the same order as the write
		 * converter. For example, if a type can be converted to both String and Double, we want
		 * both the this key conversion and the write converter to choose the same.
		 */
		for (Class<?> validKeyType : singleItemType2ToMethodMap.keySet()) {
			if (!isValidSpannerKeyType(validKeyType)) {
				continue;
			}
			if (this.writeConverter.canConvert(object.getClass(), validKeyType)) {
				return this.writeConverter.convert(object, validKeyType);
			}
		}
		throw new SpannerDataException(
				"The given object type couldn't be built into a Cloud Spanner Key: "
						+ object.getClass());
	}

	private boolean isValidSpannerKeyType(Class type) {
		return SPANNER_KEY_COMPATIBLE_TYPES.contains(type);
	}

	// @formatter:off

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
	// @formatter:on
	@SuppressWarnings("unchecked")
	private void writeProperty(MultipleValueBinder sink,
			PersistentPropertyAccessor accessor,
			SpannerPersistentProperty property) {
		Object propertyValue = accessor.getProperty(property);

		Class<?> propertyType = property.getType();
		ValueBinder<WriteBuilder> valueBinder = sink.set(property.getColumnName());

		boolean valueSet = false;

		/*
		 * Due to type erasure, binder methods for Iterable properties must be manually specified.
		 * ByteArray must be excluded since it implements Iterable, but is also explicitly
		 * supported by spanner.
		 */
		if (ConversionUtils.isIterableNonByteArrayType(propertyType)) {
			valueSet = attemptSetIterableValue((Iterable<Object>) propertyValue, valueBinder,
					property);
		}
		else {

			// use the user's annotated column type if possible
			if (property.getAnnotatedColumnItemType() != null) {
				valueSet = attemptSetSingleItemValue(propertyValue, propertyType,
						valueBinder,
						SpannerTypeMapper.getSimpleJavaClassFor(property.getAnnotatedColumnItemType()));
			}
			else {
				// directly try to set using the property's original Java type
				if (!valueSet) {
					valueSet = attemptSetSingleItemValue(propertyValue, propertyType,
							valueBinder, propertyType);
				}

				// Finally try and find any conversion that works
				if (!valueSet) {
					for (Class<?> targetType : singleItemType2ToMethodMap.keySet()) {
						valueSet = attemptSetSingleItemValue(propertyValue, propertyType,
								valueBinder, targetType);
						if (valueSet) {
							break;
						}
					}
				}
			}
		}

		if (!valueSet) {
			throw new SpannerDataException(String.format(
					"Unsupported mapping for type: %s", propertyValue.getClass()));
		}
	}

	private boolean attemptSetIterableValue(Iterable<Object> value,
			ValueBinder<WriteBuilder> valueBinder,
			SpannerPersistentProperty spannerPersistentProperty) {

		Class innerType = ConversionUtils.boxIfNeeded(spannerPersistentProperty.getColumnInnerType());
		if (innerType == null) {
			return false;
		}

		boolean valueSet = false;

		// use the annotated column type if possible.
		if (spannerPersistentProperty.getAnnotatedColumnItemType() != null) {
			valueSet = attemptSetIterablePropertyWithType(value, valueBinder, innerType,
					SpannerTypeMapper.getSimpleJavaClassFor(
							spannerPersistentProperty.getAnnotatedColumnItemType()));
		}
		else {

			// attempt check if there is directly a write method that can accept the
			// property
			if (!valueSet && iterablePropertyType2ToMethodMap.containsKey(innerType)) {
				iterablePropertyType2ToMethodMap.get(innerType).accept(valueBinder,
						value);
				valueSet = true;
			}

			// Finally find any compatible conversion
			if (!valueSet) {
				for (Class<?> targetType : iterablePropertyType2ToMethodMap.keySet()) {
					valueSet = attemptSetIterablePropertyWithType(value, valueBinder,
							innerType, targetType);
					if (valueSet) {
						break;
					}
				}

			}
		}
		return valueSet;
	}

	private boolean attemptSetIterablePropertyWithType(Iterable<Object> value,
			ValueBinder<WriteBuilder> valueBinder, Class innerType, Class<?> targetType) {
		if (this.writeConverter.canConvert(innerType, targetType)) {
			BiConsumer<ValueBinder<?>, Iterable> toMethod = iterablePropertyType2ToMethodMap
					.get(targetType);
			toMethod.accept(valueBinder,
					value == null ? null
							: ConversionUtils.convertIterable(value, targetType,
									this.writeConverter));
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private <T> boolean attemptSetSingleItemValue(Object value, Class<?> sourceType,
			ValueBinder<WriteBuilder> valueBinder, Class<T> targetType) {
		if (!this.writeConverter.canConvert(sourceType, targetType)) {
			return false;
		}
		Class innerType = ConversionUtils.boxIfNeeded(targetType);
		BiFunction<ValueBinder, T, ?> toMethod = (BiFunction<ValueBinder, T, ?>)
				singleItemType2ToMethodMap.get(innerType);
		if (toMethod == null) {
			return false;
		}
		// We're just checking for the bind to have succeeded, we don't need to chain the result.
		// Spanner allows binding of null values.
		Object ignored = toMethod.apply(valueBinder,
				value == null ? null : this.writeConverter.convert(value, targetType));
		return true;
	}
}
