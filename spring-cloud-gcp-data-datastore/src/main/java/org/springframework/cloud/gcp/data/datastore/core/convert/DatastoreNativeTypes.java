/*
 * Copyright 2017-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.gcp.data.datastore.core.convert;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.GqlQuery.Builder;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.LatLngValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.core.util.MapBuilder;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * A class to manage Datastore-specific simple type conversions.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
 *
 * @since 1.1
 */
public abstract class DatastoreNativeTypes {

	/**
	 * The set of natively-supported Datastore types.
	 */
	public static final Set<Class<?>> DATASTORE_NATIVE_TYPES;

	/**
	 * The set of native ID types that Datastore supports.
	 */
	public static final Set<Class<?>> ID_TYPES;

	private static final Map<Class<?>, Function<?, Value<?>>> DATASTORE_TYPE_WRAPPERS;

	private static final Map<Class<?>, Function<Builder, BiFunction<String, Object, Builder>>>
			GQL_PARAM_BINDING_FUNC_MAP;

	static {
		//keys are used for type resolution, in order of insertion
		Map<Class<?>, Function<?, Value<?>>> wrappers = new LinkedHashMap<>();
		wrappers.put(Blob.class, (Function<Blob, Value<?>>) BlobValue::of);
		wrappers.put(Boolean.class, (Function<Boolean, Value<?>>) BooleanValue::of);
		wrappers.put(Long.class, (Function<Long, Value<?>>) LongValue::of);
		wrappers.put(Double.class, (Function<Double, Value<?>>) DoubleValue::of);
		wrappers.put(LatLng.class, (Function<LatLng, Value<?>>) LatLngValue::of);
		wrappers.put(Timestamp.class, (Function<Timestamp, Value<?>>) TimestampValue::of);
		wrappers.put(String.class, (Function<String, Value<?>>) StringValue::of);
		wrappers.put(Enum.class, (Function<Enum, Value<?>>) (x) -> StringValue.of(x.name()));
		wrappers.put(Entity.class, (Function<Entity, Value<?>>) EntityValue::of);
		wrappers.put(Key.class, (Function<Key, Value<?>>) KeyValue::of);

		DATASTORE_TYPE_WRAPPERS = Collections.unmodifiableMap(wrappers);

		//entries are used for type resolution, in order of insertion
		DATASTORE_NATIVE_TYPES = Collections.unmodifiableSet(wrappers.keySet());

		ID_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(String.class, Long.class)));

		GQL_PARAM_BINDING_FUNC_MAP = new MapBuilder<Class<?>, Function<Builder, BiFunction<String, Object, Builder>>>()
				.put(Cursor.class, (builder) -> (s, o) -> builder.setBinding(s, (Cursor) o))
				.put(String.class, (builder) -> (s, o) -> builder.setBinding(s, (String) o))
				.put(Enum.class,
					(builder) -> (s, o) -> builder.setBinding(s, ((Enum) o).name()))
				.put(String[].class,
					(builder) -> (s, o) -> builder.setBinding(s, (String[]) o))
				.put(Long.class, (builder) -> (s, o) -> builder.setBinding(s, (Long) o))
				.put(Long[].class, (builder) -> (s, o) -> builder.setBinding(s, Stream.of((Long[]) o)
					.mapToLong(Long::longValue).toArray()))
				.put(long[].class, (builder) -> (s, o) -> builder.setBinding(s, (long[]) o))
				.put(Double.class, (builder) -> (s, o) -> builder.setBinding(s, (Double) o))
				.put(Double[].class, (builder) -> (s, o) -> builder.setBinding(s, Stream.of((Double[]) o)
					.mapToDouble(Double::doubleValue).toArray()))
				.put(double[].class,
					(builder) -> (s, o) -> builder.setBinding(s, (double[]) o))
				.put(Boolean.class,
					(builder) -> (s, o) -> builder.setBinding(s, (Boolean) o))
				.put(Boolean[].class, (builder) -> (s, o) -> builder.setBinding(s,
						wrapperToBooleanArray((Boolean[]) o)))
				.put(boolean[].class,
					(builder) -> (s, o) -> builder.setBinding(s, (boolean[]) o))
				.put(Timestamp.class,
					(builder) -> (s, o) -> builder.setBinding(s, (Timestamp) o))
				.put(Timestamp[].class,
					(builder) -> (s, o) -> builder.setBinding(s, (Timestamp[]) o))
				.put(Key.class, (builder) -> (s, o) -> builder.setBinding(s, (Key) o))
				.put(Key[].class, (builder) -> (s, o) -> builder.setBinding(s, (Key[]) o))
				.put(Blob.class, (builder) -> (s, o) -> builder.setBinding(s, (Blob) o))
				.put(Blob[].class, (builder) -> (s, o) -> builder.setBinding(s, (Blob[]) o))
				.build();
	}

	/**
	 * A simple type holder that only contains the Cloud Datastore native data types.
	 */
	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(DATASTORE_NATIVE_TYPES, true);

	/**
	 * Checks if a given type is a native type for Cloud Datastore.
	 * @param aClass the class type to check
	 * @return `true` if the type is a native type, which includes `null`. `false` otherwise.
	 */
	public static boolean isNativeType(Class aClass) {
		return aClass == null || DATASTORE_NATIVE_TYPES.contains(aClass);
	}

	/**
	 * Wraps Datastore native type to Datastore value type.
	 * @param propertyVal the property value to wrap
	 * @return the wrapped value
	*/
	@SuppressWarnings("unchecked")
	public static Value wrapValue(Object propertyVal) {
		if (propertyVal == null) {
			return new NullValue();
		}
		Class propertyClass = getTypeForWrappingInDatastoreValue(propertyVal);
		Function wrapper = DatastoreNativeTypes.DATASTORE_TYPE_WRAPPERS
				.get(propertyClass);
		if (wrapper != null) {
			return (Value) wrapper.apply(propertyVal);
		}
		throw new DatastoreDataException(
				"Unable to convert " + propertyClass
				+ " to Datastore supported type.");
	}

	/**
	 * Bind a given tag and value to a GQL query builder.
	 * @param builder the builder holding a GQL query that is being built.
	 * @param tagName the name of the tag to bind.
	 * @param val the value to bind to the tag.
	 */
	public static void bindValueToGqlBuilder(Builder builder, String tagName,
			Object val) {
		Class valClass = getTypeForWrappingInDatastoreValue(val);
		if (!GQL_PARAM_BINDING_FUNC_MAP.containsKey(valClass)) {
			throw new DatastoreDataException(
					"Param value for GQL annotated query is not a supported Cloud "
							+ "Datastore GQL param type: " + valClass);
		}
		// this value must be set due to compiler rule
		Object unusued = GQL_PARAM_BINDING_FUNC_MAP.get(valClass).apply(builder)
				.apply(tagName, val);
	}

	private static Class getTypeForWrappingInDatastoreValue(Object val) {
		return val.getClass().isEnum() ? Enum.class : val.getClass();
	}

	private static boolean[] wrapperToBooleanArray(Boolean[] input) {
		boolean[] output = new boolean[input.length];
		for (int i = 0; i < input.length; i++) {
			output[i] = input[i];
		}
		return output;
	}

}
