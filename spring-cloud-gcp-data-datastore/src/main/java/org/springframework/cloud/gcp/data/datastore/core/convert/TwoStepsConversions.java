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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * In order to support {@link CustomConversions}, this class applies 2-step conversions.
 * The first step produces one of {@link org.springframework.data.mapping.model.SimpleTypeHolder}'s simple types.
 * The second step converts simple types to Datastore-native types.
 * The second step is skipped if the first one produces a Datastore-native type.
 *
 * @author Dmitry Solomakha
 *
 * @since 1.1
 */
public class TwoStepsConversions implements ReadWriteConversions {
	private static final Converter<Blob, byte[]> BLOB_TO_BYTE_ARRAY_CONVERTER = new Converter<Blob, byte[]>() {
		@Override
		public byte[] convert(Blob source) {
			return source.toByteArray();
		}
	};

	private static final Converter<byte[], Blob> BYTE_ARRAY_TO_BLOB_CONVERTER = new Converter<byte[], Blob>() {
		@Override
		public Blob convert(byte[] source) {
			return Blob.copyFrom(source);
		}
	};

	private final GenericConversionService conversionService;

	private final GenericConversionService internalConversionService;

	private final CustomConversions customConversions;

	private final ObjectToKeyFactory objectToKeyFactory;

	private DatastoreEntityConverter datastoreEntityConverter;

	private final Map<Class, Optional<Class<?>>> writeConverters = new HashMap<>();

	public TwoStepsConversions(CustomConversions customConversions, ObjectToKeyFactory objectToKeyFactory) {
		this.objectToKeyFactory = objectToKeyFactory;
		this.conversionService = new DefaultConversionService();
		this.internalConversionService = new DefaultConversionService();
		this.customConversions = customConversions;
		this.customConversions.registerConvertersIn(this.conversionService);

		this.internalConversionService.addConverter(BYTE_ARRAY_TO_BLOB_CONVERTER);
		this.internalConversionService.addConverter(BLOB_TO_BYTE_ARRAY_CONVERTER);

	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T convertOnRead(Value val, DatastorePersistentProperty persistentProperty) {
		BiFunction<Object, Class<?>, T> readConverter = persistentProperty.isEmbedded()
				? this::convertOnReadSingleEmbedded
				: this::convertOnReadSingle;

		Class<?> targetType = persistentProperty.getType();

		Class<?> componentType = persistentProperty.getComponentType();
		Object unwrappedVal = val.get();
		if (unwrappedVal instanceof Iterable && componentType != null) {
			try {
				List<?> elements = ((List<Value>) unwrappedVal)
						.stream()
						.map(v -> readConverter.apply(v.get(), componentType))
						.collect(Collectors.toList());
				return (T) convertCollection(elements, targetType);

			}
			catch (ConversionException | DatastoreDataException e) {
				throw new DatastoreDataException("Unable process elements of a collection", e);
			}
		}

		return readConverter.apply(unwrappedVal, targetType);
	}

	@SuppressWarnings("unchecked")
	private <T> T convertOnReadSingleEmbedded(Object value, Class<?> targetClass) {
		if (value instanceof BaseEntity || value == null) {
			return (T) this.datastoreEntityConverter.read(targetClass, (BaseEntity) value);
		}
		throw new DatastoreDataException("Embedded entity was expected, but " + value.getClass() + " found");
	}

	@SuppressWarnings("unchecked")
	public <T> T convertOnReadSingle(Object val, Class<?> targetType) {
		if (val == null) {
			return null;
		}
		Object result = null;
		TypeTargets typeTargets = computeTypeTargets(targetType);

		if (typeTargets.getFirstStepTarget() == null && typeTargets.getSecondStepTarget() == null
				&& ClassUtils.isAssignable(targetType, val.getClass())) {
			//neither first or second steps were applied, no conversion is necessary
			result = val;
		}
		else if (typeTargets.getFirstStepTarget() == null && typeTargets.getSecondStepTarget() != null) {
			//only second step was applied on write
			result = this.internalConversionService.convert(val, targetType);
		}
		else if (typeTargets.getFirstStepTarget() != null && typeTargets.getSecondStepTarget() == null) {
			//only first step was applied on write
			result = this.conversionService.convert(val, targetType);
		}
		else if (typeTargets.getFirstStepTarget() != null && typeTargets.getSecondStepTarget() != null) {
			//both steps were applied
			Object secondStepVal = this.internalConversionService.convert(val, typeTargets.getFirstStepTarget());
			result = this.conversionService.convert(secondStepVal, targetType);
		}
		if (result != null) {
			return (T) result;
		}
		else {
			throw new DatastoreDataException("Unable to convert " + val.getClass() + " to " + targetType);
		}

	}

	@Override
	public Value convertOnWrite(Object proppertyVal, DatastorePersistentProperty persistentProperty) {
		Object val = proppertyVal;

		Function<Object, Value> writeConverter = (persistentProperty.isEmbedded() && val != null)
				? (Object value) -> convertOnWriteSingleEmbedded(value, persistentProperty.getFieldName())
				: this::convertOnWriteSingle;

		//Check if property is a non-null array
		if (val != null && val.getClass().isArray() && val.getClass() != byte[].class) {
			//if a propperty is an array, convert it to list
			val = CollectionUtils.arrayToList(val);
		}

		if (val instanceof Iterable) {
			List<Value<?>> values = new ArrayList<>();
			for (Object propEltValue : (Iterable) val) {
				values.add(writeConverter.apply(propEltValue));
			}
			return ListValue.of(values);
		}
		return writeConverter.apply(val);
	}

	private EntityValue convertOnWriteSingleEmbedded(Object val, String kindName) {
		IncompleteKey key = this.objectToKeyFactory.getIncompleteKey(kindName);

		FullEntity.Builder<IncompleteKey> builder = FullEntity.newBuilder(key);
		this.datastoreEntityConverter.write(val, builder);
		return EntityValue.of(builder.build());
	}

	@SuppressWarnings("unchecked")
	public Value convertOnWriteSingle(Object propertyVal) {
		Object result = propertyVal;
		if (result != null) {
			TypeTargets typeTargets = computeTypeTargets(result.getClass());
			if (typeTargets.getFirstStepTarget() != null) {
				result = this.conversionService.convert(propertyVal, typeTargets.getFirstStepTarget());
			}

			if (typeTargets.getSecondStepTarget() != null) {
				result = this.internalConversionService.convert(result, typeTargets.getSecondStepTarget());
			}
		}
		return DatastoreNativeTypes.wrapValue(result);
	}

	private TypeTargets computeTypeTargets(Class<?> firstStepSource) {
		Class<?> firstStepTarget = null;
		Class<?> secondStepTarget = null;

		if (!DatastoreNativeTypes.isNativeType(firstStepSource)) {
			Optional<Class<?>> simpleType = this.customConversions.getCustomWriteTarget(firstStepSource);
			if (simpleType.isPresent()) {
				firstStepTarget = simpleType.get();
			}

			Class<?> effectiveFirstStepTarget =
					firstStepTarget == null ? firstStepSource : firstStepTarget;

			Optional<Class<?>> datastoreBasicType = getCustomWriteTarget(effectiveFirstStepTarget);

			if (datastoreBasicType.isPresent()) {
				secondStepTarget = datastoreBasicType.get();
			}
		}
		return new TypeTargets(firstStepTarget, secondStepTarget);
	}

	@SuppressWarnings("unchecked")
	public <T> T convertCollection(Object collection, Class<?> target) {
		if (collection == null || target == null || ClassUtils.isAssignableValue(target, collection)) {
			return (T) collection;
		}

		return (T) this.conversionService.convert(collection, target);
	}

	private Optional<Class<?>> getCustomWriteTarget(Class<?> sourceType) {
		if (DatastoreNativeTypes.isNativeType(sourceType)) {
			return Optional.empty();
		}
		return this.writeConverters.computeIfAbsent(sourceType, this::getSimpleTypeWithBidirectionalConversion);
	}

	private Optional<Class<?>> getSimpleTypeWithBidirectionalConversion(Class inputType) {
		return DatastoreNativeTypes.DATASTORE_NATIVE_TYPES.stream()
				.filter(simpleType ->
						this.internalConversionService.canConvert(inputType, simpleType)
								&& this.internalConversionService.canConvert(simpleType, inputType))
				.findAny();
	}

	@Override
	public void registerEntityConverter(DatastoreEntityConverter datastoreEntityConverter) {
		this.datastoreEntityConverter = datastoreEntityConverter;
	}

	private static class TypeTargets {
		private Class<?> firstStepTarget;

		private Class<?> secondStepTarget;

		TypeTargets(Class<?> firstStepTarget, Class<?> secondStepTarget) {
			this.firstStepTarget = firstStepTarget;
			this.secondStepTarget = secondStepTarget;
		}

		Class<?> getFirstStepTarget() {
			return this.firstStepTarget;
		}

		Class<?> getSecondStepTarget() {
			return this.secondStepTarget;
		}
	}
}
