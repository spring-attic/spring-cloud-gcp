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

import java.util.List;
import java.util.Map;
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

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreMappingContext;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentEntity;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.EntityInstantiator;
import org.springframework.data.convert.EntityInstantiators;
import org.springframework.data.convert.JodaTimeConverters;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.data.convert.ThreeTenBackPortConverters;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.mapping.model.PersistentEntityParameterValueProvider;
import org.springframework.lang.Nullable;

/**
 * A class for object to entity and entity to object conversions
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class ConverterAwareMappingDatastoreEntityConverter implements DatastoreEntityConverter {
	private DatastoreMappingContext mappingContext;

	private final EntityInstantiators instantiators = new EntityInstantiators();

	private final GenericConversionService conversionService = new DefaultConversionService();

	private final DatastoreSimpleTypes conversions = new DatastoreSimpleTypes(this.conversionService);

	private static final Map<Class<?>, Function<?, Value<?>>> VAL_FACTORIES;

	static {
		VAL_FACTORIES = ImmutableMap.<Class<?>, Function<?, Value<?>>>builder()
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

	private static List<Converter> DEFAULT_CONVERTERS;

	static {
		DEFAULT_CONVERTERS = ImmutableList.<Converter>builder()
				.addAll(JodaTimeConverters.getConvertersToRegister())
				.addAll(Jsr310Converters.getConvertersToRegister())
				.addAll(ThreeTenBackPortConverters.getConvertersToRegister())
				.build();
	}

	public ConverterAwareMappingDatastoreEntityConverter(DatastoreMappingContext mappingContext) {
		this(mappingContext, null);
	}

	public ConverterAwareMappingDatastoreEntityConverter(DatastoreMappingContext mappingContext,
			List<Converter> customConverters) {
		this.mappingContext = mappingContext;
		if (customConverters != null) {
			customConverters.forEach(this.conversionService::addConverter);
		}
		DEFAULT_CONVERTERS.forEach(this.conversionService::addConverter);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R read(Class<R> aClass, Entity entity) {
		DatastorePersistentEntity<R> persistentEntity =
				(DatastorePersistentEntity<R>) this.mappingContext.getPersistentEntity(aClass);

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
		DatastorePersistentEntity<?> persistentEntity = this.mappingContext.getPersistentEntity(source.getClass());
		PersistentPropertyAccessor accessor = persistentEntity.getPropertyAccessor(source);
		persistentEntity.doWithProperties(
				(DatastorePersistentProperty datastorePersistentProperty) ->
						writeProperty(sink, accessor, datastorePersistentProperty));
	}

	@SuppressWarnings("unchecked")
	private void writeProperty(Entity.Builder sink, PersistentPropertyAccessor accessor,
			DatastorePersistentProperty persistentProperty) {
		Object propertyVal = convertToSimpleType(accessor.getProperty(persistentProperty), persistentProperty);
		if (propertyVal == null) {
			sink.set(persistentProperty.getFieldName(), new NullValue());
			return;
		}
		Function valueFactory = VAL_FACTORIES.get(propertyVal.getClass());

		if (valueFactory == null) {
			throw new DatastoreDataException("Simple type value factory is not found for property" +
					" with name " + persistentProperty.getFieldName()
					+ ". The property's simple type is " + propertyVal.getClass());
		}

		Value val = (Value) valueFactory.apply(propertyVal);
		sink.set(persistentProperty.getFieldName(), val);
	}

	private Object convertToSimpleType(@Nullable Object obj, DatastorePersistentProperty prop) {
		if (obj == null || DatastoreSimpleTypes.isSimple(obj.getClass())) {
			return obj;
		}

		Class<?> basicTargetType = this.conversions.getWriteTarget(obj.getClass());

		if (basicTargetType != null) {
				return this.conversionService.convert(obj, basicTargetType);
		}

		throw new DatastoreDataException("The value in column with name " + prop.getFieldName()
				+ " is of unsupported data type. " + "The property's type is " + obj.getClass());
	}
}
