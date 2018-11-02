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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.FullEntity.Builder;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Value;

import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastoreDataException;
import org.springframework.cloud.gcp.data.datastore.core.mapping.DatastorePersistentProperty;
import org.springframework.cloud.gcp.data.datastore.core.mapping.EmbeddedType;
import org.springframework.cloud.gcp.data.datastore.core.util.ValueUtil;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * In order to support {@link CustomConversions}, this class applies 2-step conversions.
 * The first step produces one of
 * {@link org.springframework.data.mapping.model.SimpleTypeHolder}'s simple types. The
 * second step converts simple types to Datastore-native types. The second step is skipped
 * if the first one produces a Datastore-native type.
 *
 * @author Dmitry Solomakha
 * @author Chengyuan Zhao
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
	public <T> T convertOnRead(Object val, Class targetCollectionType, Class targetComponentType) {
		return (T) convertOnRead(val, EmbeddedType.NOT_EMBEDDED,
				targetCollectionType,
				ClassTypeInformation.from(targetComponentType));
	}

	@Override
	public <T> T convertOnRead(Object val, EmbeddedType embeddedType, TypeInformation targetTypeInformation) {
		TypeInformation componentTypeInformation;
		Class collectionType = null;
		if (targetTypeInformation.isCollectionLike()) {
			componentTypeInformation = targetTypeInformation.getComponentType();
			collectionType = targetTypeInformation.getType();
		}
		else {
			componentTypeInformation = targetTypeInformation;
		}
		return convertOnRead(val, embeddedType, collectionType, componentTypeInformation);
	}

	private <T> T convertOnRead(Object val, EmbeddedType embeddedType,
			Class targetCollectionType, TypeInformation targetComponentType) {
		if (val == null) {
			return null;
		}
		BiFunction<Object, TypeInformation<?>, ?> readConverter;
		switch (embeddedType) {
		case EMBEDDED_MAP:
			readConverter = (x, typeInformation) -> convertOnReadSingleEmbeddedMap(x,
					typeInformation.getTypeArguments().get(0).getType(),
					typeInformation.getTypeArguments().get(1));
			break;
		case EMBEDDED_ENTITY:
			readConverter = this::convertOnReadSingleEmbedded;
			break;
		case NOT_EMBEDDED:
			readConverter = this::convertOnReadSingle;
			break;
		default:
			throw new DatastoreDataException(
					"Unexpected property embedded type: " + embeddedType);
		}

		if ((val instanceof Iterable || val.getClass().isArray())
				&& targetCollectionType != null && targetComponentType != null) {
			try {
				List elements = (val.getClass().isArray() ? (Arrays.asList(val))
						: ((List<?>) val))
						.stream()
								.map(v -> readConverter.apply(
										v instanceof Value ? ((Value) v).get() : v,
										targetComponentType))
						.collect(Collectors.toList());
				return (T) convertCollection(elements, targetCollectionType);

			}
			catch (ConversionException | DatastoreDataException e) {
				throw new DatastoreDataException("Unable process elements of a collection", e);
			}
		}
		return (T) readConverter.apply(val, targetComponentType);
	}

	private <T, R> Map<T, R> convertOnReadSingleEmbeddedMap(Object value,
			Class<T> keyType, TypeInformation<R> targetComponentType) {
		Assert.notNull(value, "Cannot convert a null value.");
		if (value instanceof BaseEntity) {
			return this.datastoreEntityConverter.readAsMap(keyType, targetComponentType,
					(BaseEntity) value);
		}
		throw new DatastoreDataException(
				"Embedded entity was expected, but " + value.getClass() + " found");
	}

	@SuppressWarnings("unchecked")
	private <T> T convertOnReadSingleEmbedded(Object value, TypeInformation<?> targetTypeInformation) {
		Assert.notNull(value, "Cannot convert a null value.");
		if (value instanceof BaseEntity) {
			return (T) this.datastoreEntityConverter.read(targetTypeInformation.getType(),
					(BaseEntity) value);
		}
		throw new DatastoreDataException("Embedded entity was expected, but "
				+ value.getClass() + " found");
	}

	@SuppressWarnings("unchecked")
	private <T> T convertOnReadSingle(Object val, TypeInformation<?> targetTypeInformation) {
		Class targetType = targetTypeInformation.getType();
		Class sourceType = val.getClass();
		Assert.notNull(val, "Cannot convert a null value.");
		Object result = null;
		TypeTargets typeTargets = computeTypeTargets(targetType);

		if (typeTargets.getFirstStepTarget() == null
				&& typeTargets.getSecondStepTarget() == null
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
			Object secondStepVal = this.internalConversionService.convert(val,
					typeTargets.getFirstStepTarget());
			result = this.conversionService.convert(secondStepVal, targetType);
		}
		// if the value can be directly converted
		else if (DatastoreNativeTypes.isNativeType(sourceType)
				&& this.conversionService.canConvert(sourceType, targetType)) {
			result = this.conversionService.convert(val, targetType);
		}
		else if (DatastoreNativeTypes.isNativeType(sourceType)
				&& this.internalConversionService.canConvert(sourceType, targetType)) {
			result = this.internalConversionService.convert(val, targetType);
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
		return convertOnWrite(proppertyVal, persistentProperty.getEmbeddedType(),
				persistentProperty.getFieldName(),
				persistentProperty.getTypeInformation());
	}

	private Value convertOnWrite(Object proppertyVal, EmbeddedType embeddedType,
			String fieldName, TypeInformation typeInformation) {
		Object val = proppertyVal;

		Function<Object, Value> writeConverter = this::convertOnWriteSingle;
		if (proppertyVal != null) {
			switch (embeddedType) {
			case EMBEDDED_MAP:
				writeConverter = x -> convertOnWriteSingleEmbeddedMap(x, fieldName,
						(TypeInformation) typeInformation.getTypeArguments().get(1));
				break;
			case EMBEDDED_ENTITY:
				writeConverter = x -> convertOnWriteSingleEmbedded(x, fieldName);
				break;
			case NOT_EMBEDDED:
				writeConverter = this::convertOnWriteSingle;
				break;
			default:
				throw new DatastoreDataException(
						"Unexpected property embedded type: " + embeddedType);
			}
		}

		val = ValueUtil.toIterableIfArray(val);

		if (val instanceof Iterable) {
			List<Value<?>> values = new ArrayList<>();
			for (Object propEltValue : (Iterable) val) {
				values.add(writeConverter.apply(propEltValue));
			}
			return ListValue.of(values);
		}
		return writeConverter.apply(val);
	}

	private EntityValue applyEntityValueBuilder(String kindName,
			Consumer<Builder> consumer) {
		IncompleteKey key = this.objectToKeyFactory.getIncompleteKey(kindName);
		FullEntity.Builder<IncompleteKey> builder = FullEntity.newBuilder(key);
		consumer.accept(builder);
		return EntityValue.of(builder.build());
	}

	private EntityValue convertOnWriteSingleEmbeddedMap(Object val, String kindName,
			TypeInformation valueTypeInformation) {
		return applyEntityValueBuilder(kindName, builder -> {
			Map map = (Map) val;
			for (Object key : map.keySet()) {
				String field = convertOnReadSingle(key,
						ClassTypeInformation.from(String.class));
				builder.set(field,
						convertOnWrite(map.get(key),
								EmbeddedType.of(valueTypeInformation),
								field, valueTypeInformation));
			}
		});
	}

	private EntityValue convertOnWriteSingleEmbedded(Object val, String kindName) {
		return applyEntityValueBuilder(kindName,
				builder -> this.datastoreEntityConverter.write(val, builder));
	}

	@Override
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
		return this.writeConverters.computeIfAbsent(sourceType,
				this::getDatastoreCompatibleType);
	}

	@Override
	public Optional<Class<?>> getDatastoreCompatibleType(Class inputType) {
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
