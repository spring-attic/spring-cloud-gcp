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

package org.springframework.cloud.gcp.data.spanner.core.convert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import com.google.cloud.ByteArray;
import com.google.cloud.Date;
import com.google.cloud.Timestamp;
import com.google.cloud.spanner.Key;
import com.google.cloud.spanner.Mutation.WriteBuilder;
import com.google.cloud.spanner.Struct;
import com.google.cloud.spanner.Value;
import com.google.cloud.spanner.ValueBinder;

import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerDataException;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerMappingContext;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentEntity;
import org.springframework.cloud.gcp.data.spanner.core.mapping.SpannerPersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.util.Assert;

/**
 * The primary class for adding values from entity objects to {@link WriteBuilder} for the
 * purpose of creating mutations for Spanner.
 *
 * @author Chengyuan Zhao
 * @author Balint Pato
 *
 * @since 1.1
 */
public class ConverterAwareMappingSpannerEntityWriter implements SpannerEntityWriter {

	private static final Set<Class> SPANNER_KEY_COMPATIBLE_TYPES = Collections.unmodifiableSet(
			new HashSet<Class>(
					Arrays.asList(
							Boolean.class, Integer.class, Long.class, Float.class, Double.class, String.class,
							ByteArray.class, Timestamp.class, com.google.cloud.Date.class)));

	/**
	 * A map of types to functions that binds them to `ValueBinder` objects.
	 */
	public static final Map<Class<?>, BiFunction<ValueBinder, ?, ?>> singleItemTypeValueBinderMethodMap;

	static final Map<Class<?>, BiConsumer<ValueBinder<?>, Iterable>>
			iterablePropertyType2ToMethodMap = createIterableTypeMapping();

	@SuppressWarnings("unchecked")
	private static Map<Class<?>, BiConsumer<ValueBinder<?>, Iterable>> createIterableTypeMapping() {
		Map<Class<?>, BiConsumer<ValueBinder<?>, Iterable>> map = new LinkedHashMap<>();
		map.put(com.google.cloud.Date.class, ValueBinder::toDateArray);
		map.put(Boolean.class, ValueBinder::toBoolArray);
		map.put(Long.class, ValueBinder::toInt64Array);
		map.put(String.class, ValueBinder::toStringArray);
		map.put(Double.class, ValueBinder::toFloat64Array);
		map.put(Timestamp.class, ValueBinder::toTimestampArray);
		map.put(ByteArray.class, ValueBinder::toBytesArray);

		return Collections.unmodifiableMap(map);
	}

	static {
		Map<Class<?>, BiFunction<ValueBinder, ?, ?>> map = new LinkedHashMap<>();

		map.put(Date.class, (BiFunction<ValueBinder, Date, ?>) ValueBinder::to);
		map.put(Boolean.class, (BiFunction<ValueBinder, Boolean, ?>) ValueBinder::to);
		map.put(Long.class, (BiFunction<ValueBinder, Long, ?>) ValueBinder::to);
		map.put(long.class, (BiFunction<ValueBinder, Long, ?>) ValueBinder::to);
		map.put(Double.class, (BiFunction<ValueBinder, Double, ?>) ValueBinder::to);
		map.put(double.class, (BiFunction<ValueBinder, Double, ?>) ValueBinder::to);
		map.put(String.class, (BiFunction<ValueBinder, String, ?>) ValueBinder::to);
		map.put(Timestamp.class,
				(BiFunction<ValueBinder, Timestamp, ?>) ValueBinder::to);
		map.put(ByteArray.class,
				(BiFunction<ValueBinder, ByteArray, ?>) ValueBinder::to);
		map.put(double[].class,
				(BiFunction<ValueBinder, double[], ?>) ValueBinder::toFloat64Array);
		map.put(boolean[].class,
				(BiFunction<ValueBinder, boolean[], ?>) ValueBinder::toBoolArray);
		map.put(long[].class,
				(BiFunction<ValueBinder, long[], ?>) ValueBinder::toInt64Array);
		map.put(Struct.class, (BiFunction<ValueBinder, Struct, ?>) ValueBinder::to);

		singleItemTypeValueBinderMethodMap = Collections.unmodifiableMap(map);
	}

	private final SpannerMappingContext spannerMappingContext;

	private final SpannerWriteConverter writeConverter;

	ConverterAwareMappingSpannerEntityWriter(SpannerMappingContext spannerMappingContext,
			SpannerWriteConverter writeConverter) {
		this.spannerMappingContext = spannerMappingContext;
		this.writeConverter = writeConverter;
	}

	public static Class<?> findFirstCompatibleSpannerSingleItemNativeType(Predicate<Class> testFunc) {
		Optional<Class<?>> compatible = singleItemTypeValueBinderMethodMap.keySet().stream().filter(testFunc)
				.findFirst();
		return compatible.isPresent() ? compatible.get() : null;
	}

	public static Class<?> findFirstCompatibleSpannerMultupleItemNativeType(Predicate<Class> testFunc) {
		Optional<Class<?>> compatible = iterablePropertyType2ToMethodMap.keySet().stream().filter(testFunc).findFirst();
		return compatible.isPresent() ? compatible.get() : null;
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
				(spannerPersistentProperty) -> {
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
	public Key convertToKey(Object key) {
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

	@Override
	public SpannerWriteConverter getSpannerWriteConverter() {
		return this.writeConverter;
	}

	private Object convertKeyPart(Object object) {

		if (object == null || isValidSpannerKeyType(ConversionUtils.boxIfNeeded(object.getClass()))) {
			return object;
		}
		/*
		 * Iterate through the supported Key component types in the same order as the write
		 * converter. For example, if a type can be converted to both String and Double, we want
		 * both the this key conversion and the write converter to choose the same.
		 */
		Class<?> compatible = ConverterAwareMappingSpannerEntityWriter
				.findFirstCompatibleSpannerSingleItemNativeType((spannerType) -> isValidSpannerKeyType(spannerType)
						&& this.writeConverter.canConvert(object.getClass(), spannerType));

		if (compatible == null) {
			throw new SpannerDataException(
					"The given object type couldn't be built into a Cloud Spanner Key: "
							+ object.getClass());
		}
		return this.writeConverter.convert(object, compatible);
	}

	private boolean isValidSpannerKeyType(Class type) {
		return SPANNER_KEY_COMPATIBLE_TYPES.contains(type);
	}

	// @formatter:off

	/**
	 * <p>
	 * For each property this method "set"s the column name and finds the corresponding "to"
	 * method on the {@link ValueBinder} interface.
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
	 *
	 * @param accessor the accessor used to get the value to write
	 * @param property the property that will be written
	 * @param sink the object that will accept the value to be written
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

			// if the property is a commit timestamp, then its Spanner column type is always TIMESTAMP
			// and only the dummy value needs to be written to trigger auto-population of the commit
			// time
			if (property.isCommitTimestamp()) {
				valueSet = attemptSetSingleItemValue(Value.COMMIT_TIMESTAMP, Timestamp.class, valueBinder,
						Timestamp.class);
			}
			// use the user's annotated column type if possible
			else if (property.getAnnotatedColumnItemType() != null) {
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
					for (Class<?> targetType : singleItemTypeValueBinderMethodMap.keySet()) {
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
					(value != null) ? ConversionUtils.convertIterable(value, targetType, this.writeConverter) : null);
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
		BiFunction<ValueBinder, T, ?> toMethod = (BiFunction<ValueBinder, T, ?>) singleItemTypeValueBinderMethodMap
				.get(innerType);
		if (toMethod == null) {
			return false;
		}
		// We're just checking for the bind to have succeeded, we don't need to chain the result.
		// Spanner allows binding of null values.
		Object ignored = toMethod.apply(valueBinder,
				(value != null) ? this.writeConverter.convert(value, targetType) : null);
		return true;
	}
}
