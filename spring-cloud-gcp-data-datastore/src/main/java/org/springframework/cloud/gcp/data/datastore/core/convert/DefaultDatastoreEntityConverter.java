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
import java.util.List;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.lang.Nullable;

/**
 * A class for object to entity and entity to object conversions
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class DefaultDatastoreEntityConverter implements DatastoreEntityConverter {
	private DatastoreMappingContext mappingContext;

	private final EntityInstantiators instantiators = new EntityInstantiators();

	private final GenericConversionService conversionService = new DefaultConversionService();

	private final DatastoreSimpleTypes toNativeConversions = new DatastoreSimpleTypes(this.conversionService);

	private final CustomConversions customConversions;

	private static final Map<Class<?>, Function<?, Value<?>>> DATASTORE_TYPE_WRAPPERS;

	static {
		DATASTORE_TYPE_WRAPPERS = ImmutableMap.<Class<?>, Function<?, Value<?>>>builder()
				.put(Blob.class, (Function<Blob, Value<?>>) BlobValue::of)
				.put(Boolean.class, (Function<Boolean, Value<?>>) BooleanValue::of)
				.put(Double.class, (Function<Double, Value<?>>) DoubleValue::of)
				.put(Entity.class, (Function<Entity, Value<?>>) EntityValue::of)
				.put(Key.class, (Function<Key, Value<?>>) KeyValue::of)
				.put(LatLng.class, (Function<LatLng, Value<?>>) LatLngValue::of)
				.put(Long.class, (Function<Long, Value<?>>) LongValue::of)
				.put(String.class, (Function<String, Value<?>>) StringValue::of)
				.put(Timestamp.class, (Function<Timestamp, Value<?>>) TimestampValue::of)
				.build();
	}

	public DefaultDatastoreEntityConverter(DatastoreMappingContext mappingContext) {
		this(mappingContext, new DatastoreCustomConversions());
	}

	public DefaultDatastoreEntityConverter(DatastoreMappingContext mappingContext,
			CustomConversions customConversions) {
		this.mappingContext = mappingContext;
		this.customConversions = customConversions;
		this.customConversions.registerConvertersIn(this.conversionService);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R read(Class<R> aClass, Entity entity) {
		DatastorePersistentEntity<R> persistentEntity =
				(DatastorePersistentEntity<R>) this.mappingContext.getPersistentEntity(aClass);

		EntityPropertyValueProvider propertyValueProvider =
				new EntityPropertyValueProvider(entity, this.conversionService, this.customConversions);

		ParameterValueProvider<DatastorePersistentProperty> parameterValueProvider =
				new PersistentEntityParameterValueProvider<>(persistentEntity, propertyValueProvider, null);

		EntityInstantiator instantiator = this.instantiators.getInstantiatorFor(persistentEntity);
		R instance = instantiator.createInstance(persistentEntity, parameterValueProvider);
		PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(instance);

		persistentEntity.doWithProperties(
				(PropertyHandler<DatastorePersistentProperty>) datastorePersistentProperty -> {
					Object value = propertyValueProvider.getPropertyValue(datastorePersistentProperty);
					accessor.setProperty(datastorePersistentProperty, value);
				});

		return instance;
	}

	@Override
	public void write(Object source, Entity.Builder sink) {
		DatastorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(source);
		persistentEntity.doWithProperties(
				(DatastorePersistentProperty datastorePersistentProperty) ->
						writeProperty(sink, accessor, datastorePersistentProperty));
	}

	private void writeProperty(Entity.Builder sink, PersistentPropertyAccessor accessor,
			DatastorePersistentProperty persistentProperty) {
		Object propertyVal = convertToSimpleType(accessor.getProperty(persistentProperty));
		Class<?> sourceType = propertyVal == null ? null : propertyVal.getClass();
		Optional<Class<?>> basicTargetType = this.toNativeConversions.getCustomWriteTarget(sourceType);
		if (basicTargetType.isPresent()) {
			propertyVal = this.conversionService.convert(propertyVal, basicTargetType.get());
		}
		Value val = getDatastoreWrappedValue(propertyVal, persistentProperty);
		sink.set(persistentProperty.getFieldName(), val);
	}

	@SuppressWarnings("unchecked")
	private Value getDatastoreWrappedValue(Object propertyVal, DatastorePersistentProperty persistentProperty) {
		if (propertyVal == null) {
			return new NullValue();
		}
		Function wrapper = DATASTORE_TYPE_WRAPPERS.get(propertyVal.getClass());
		if (wrapper != null) {
			return (Value) wrapper.apply(propertyVal);
		}
		throw new DatastoreDataException("Simple type value wrapper is not found for property" +
				" with name " + persistentProperty.getFieldName()
				+ ". The property's simple type is " + propertyVal.getClass());
	}

	private Object convertToSimpleType(@Nullable Object obj) {
		if (obj == null) {
			return null;
		}

		Optional<Class<?>> basicTargetType = this.customConversions.getCustomWriteTarget(obj.getClass());
		if (basicTargetType.isPresent()) {
				return this.conversionService.convert(obj, basicTargetType.get());
		}

		return obj;
	}

	/**
	 * A class to manage Datastore-specific simple type conversions.
	 *
	 * @author Dmitry Solomakha
	 */
	static class DatastoreSimpleTypes {

		static final Set<Class<?>> DATASTORE_NATIVE_TYPES;

		static final Set<Class<?>> ID_TYPES;

		static final List<Class<?>> DATASTORE_NATIVE_TYPES_RESOLUTION;

		static {
			ID_TYPES = ImmutableSet.<Class<?>>builder()
					.add(String.class)
					.add(Long.class)
					.build();

			DATASTORE_NATIVE_TYPES_RESOLUTION = ImmutableList.<Class<?>>builder()
					.add(Boolean.class)
					.add(Long.class)
					.add(Double.class)
					.add(LatLng.class)
					.add(Timestamp.class)
					.add(String.class)
					.add(Blob.class)
					.build();

			DATASTORE_NATIVE_TYPES = ImmutableSet.<Class<?>>builder()
					.addAll(DATASTORE_NATIVE_TYPES_RESOLUTION)
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

		Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
			if (isSimple(sourceType)) {
				return Optional.empty();
			}
			return this.writeConverters.computeIfAbsent(sourceType, this::getSimpleTypeWithBidirectionalConversion);
		}

		private Optional<Class<?>> getSimpleTypeWithBidirectionalConversion(Class inputType) {
			return DatastoreSimpleTypes.DATASTORE_NATIVE_TYPES_RESOLUTION.stream()
					.filter(simpleType ->
							this.conversionService.canConvert(inputType, simpleType)
							&& this.conversionService.canConvert(simpleType, inputType))
					.findAny();
		}
	}
}
