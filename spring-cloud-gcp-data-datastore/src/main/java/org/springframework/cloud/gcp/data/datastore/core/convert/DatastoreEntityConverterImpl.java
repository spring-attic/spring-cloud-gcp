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

import java.util.Map;
import java.util.Optional;
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

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.lang.Nullable;

/**
 * Class for object to entity and entity to object conversions
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class DatastoreEntityConverterImpl implements DatastoreEntityConverter {
	private DatastoreMappingContext mappingContext;

	private final EntityInstantiators instantiators = new EntityInstantiators();

	private final DatastoreCustomConversions conversions = new DatastoreCustomConversions();

	private final GenericConversionService conversionService = new DefaultConversionService();

	private static Map<Class<?>, Function<?, Value<?>>> valFactories;

	static {
		ImmutableMap.Builder<Class<?>, Function<?, Value<?>>> builder = new ImmutableMap.Builder<>();

		builder.put(Blob.class, (Function<Blob, Value<?>>) BlobValue::of);
		builder.put(Boolean.class, (Function<Boolean, Value<?>>) BooleanValue::of);
		builder.put(Double.class, (Function<Double, Value<?>>) DoubleValue::of);
		builder.put(Entity.class, (Function<Entity, Value<?>>) EntityValue::of);
		builder.put(Key.class, (Function<Key, Value<?>>) KeyValue::of);
		builder.put(LatLng.class, (Function<LatLng, Value<?>>) LatLngValue::of);
		builder.put(Long.class, (Function<Long, Value<?>>) LongValue::of);
		builder.put(String.class, (Function<String, Value<?>>) StringValue::of);
		builder.put(Timestamp.class, (Function<Timestamp, Value<?>>) TimestampValue::of);

		valFactories = builder.build();
	}

	DatastoreEntityConverterImpl(DatastoreMappingContext mappingContext) {
		this.mappingContext = mappingContext;
		this.conversions.registerConvertersIn(this.conversionService);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R read(Class<R> aClass, Entity entity) {
		DatastorePersistentEntity<R> persistentEntity = (DatastorePersistentEntity<R>) this.mappingContext
				.getPersistentEntity(aClass);

		EntityPropertyValueProvider propertyValueProvider =
				new EntityPropertyValueProvider(entity, this.conversionService);

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
		DatastorePersistentEntity<?> persistentEntity = this.mappingContext
				.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity
				.getPropertyAccessor(source);
		persistentEntity.doWithProperties(
				(DatastorePersistentProperty datastorePersistentProperty) -> writeProperty(sink, accessor,
						datastorePersistentProperty));
	}

	@SuppressWarnings("unchecked")
	private void writeProperty(Entity.Builder sink, PersistentPropertyAccessor accessor,
			DatastorePersistentProperty persistentProperty) {
		Object propertyVal = convertToSimpleType(accessor.getProperty(persistentProperty), persistentProperty);
		if (propertyVal == null) {
			sink.set(persistentProperty.getFieldName(), new NullValue());
			return;
		}
		Function valueFactory = valFactories.get(propertyVal.getClass());

		if (valueFactory == null) {
			throw new DatastoreDataException("Simple type value factory is not found for property" +
					" with name " + persistentProperty.getFieldName()
					+ " which is of simple type " + propertyVal.getClass());
		}

		Value val = (Value) valueFactory.apply(propertyVal);
		sink.set(persistentProperty.getFieldName(), val);
	}

	private Object convertToSimpleType(@Nullable Object obj, DatastorePersistentProperty prop) {
		if (obj == null || DatastoreSimpleTypes.DATASTORE_SIMPLE_TYPES.contains(obj.getClass())) {
			return obj;
		}

		Optional<Class<?>> basicTargetType = this.conversions.getCustomWriteTarget(obj.getClass());

		if (basicTargetType.isPresent()) {
			Class<?> aClass = basicTargetType.get();
			if (DatastoreSimpleTypes.DATASTORE_SIMPLE_TYPES.contains(aClass)) {
				return this.conversionService.convert(obj, aClass);
			}
		}

		throw new DatastoreDataException("The value in column with name " + prop.getFieldName()
				+ " is of unsupported data type. " + "The property's type is " + obj.getClass());
	}

}
