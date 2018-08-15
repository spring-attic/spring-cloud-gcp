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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.LatLngValue;
import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * A class to manage Datastore-specific simple type conversions.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
class DatastoreSimpleTypes {
	static final Map<Class<?>, Function<?, Value<?>>> DATASTORE_TYPE_WRAPPERS;

	static final Set<Class<?>> DATASTORE_NATIVE_TYPES;

	static final Set<Class<?>> ID_TYPES;

	static {
		//keys are used for type resolution, in order of insertion
		DATASTORE_TYPE_WRAPPERS = ImmutableMap.<Class<?>, Function<?, Value<?>>>builder()
				.put(Boolean.class, (Function<Boolean, Value<?>>) BooleanValue::of)
				.put(Long.class, (Function<Long, Value<?>>) LongValue::of)
				.put(Double.class, (Function<Double, Value<?>>) DoubleValue::of)
				.put(LatLng.class, (Function<LatLng, Value<?>>) LatLngValue::of)
				.put(Timestamp.class, (Function<Timestamp, Value<?>>) TimestampValue::of)
				.put(String.class, (Function<String, Value<?>>) StringValue::of)
				.put(Blob.class, (Function<Blob, Value<?>>) BlobValue::of)
				.put(Entity.class, (Function<Entity, Value<?>>) EntityValue::of)
				.put(Key.class, (Function<Key, Value<?>>) KeyValue::of)
				.build();


		//entries are used for type resolution, in order of insertion
		DATASTORE_NATIVE_TYPES = ImmutableSet.<Class<?>>builder()
				.addAll(DATASTORE_TYPE_WRAPPERS.keySet())
				.build();

		ID_TYPES = ImmutableSet.<Class<?>>builder()
				.add(String.class)
				.add(Long.class)
				.build();
	}

	static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(DATASTORE_NATIVE_TYPES, true);

	final private Map<Class, Optional<Class<?>>> writeConverters = new HashMap<>();

	final private ConversionService conversionService;

	DatastoreSimpleTypes(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	static boolean isSimple(Class aClass) {
		return aClass == null || DATASTORE_NATIVE_TYPES.contains(aClass);
	}

	@SuppressWarnings("unchecked")
	static public Value getDatastoreWrappedValue(Object propertyVal, DatastorePersistentProperty persistentProperty) {
		if (propertyVal == null) {
			return new NullValue();
		}
		Function wrapper = DatastoreSimpleTypes.DATASTORE_TYPE_WRAPPERS.get(propertyVal.getClass());
		if (wrapper != null) {
			return (Value) wrapper.apply(propertyVal);
		}
		throw new DatastoreDataException("Unable to convert a property" +
				" with name " + persistentProperty.getFieldName()
				+ " to Datastore supported type. The property's type is " + propertyVal.getClass());
	}


	public Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
		if (isSimple(sourceType)) {
			return Optional.empty();
		}
		return this.writeConverters.computeIfAbsent(sourceType, this::getSimpleTypeWithBidirectionalConversion);
	}

	private Optional<Class<?>> getSimpleTypeWithBidirectionalConversion(Class inputType) {
		return DATASTORE_NATIVE_TYPES.stream()
				.filter(simpleType ->
						this.conversionService.canConvert(inputType, simpleType)
								&& this.conversionService.canConvert(simpleType, inputType))
				.findAny();
	}
}
